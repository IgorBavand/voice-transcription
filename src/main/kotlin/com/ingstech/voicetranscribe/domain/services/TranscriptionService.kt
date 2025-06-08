package com.ingstech.voicetranscribe.domain.services

import com.ingstech.voicetranscribe.domain.dto.request.GeminiContent
import com.ingstech.voicetranscribe.domain.dto.request.GeminiInlineData
import com.ingstech.voicetranscribe.domain.dto.request.GeminiPart
import com.ingstech.voicetranscribe.domain.dto.request.GeminiRequest
import com.ingstech.voicetranscribe.domain.dto.response.GeminiResponse
import com.ingstech.voicetranscribe.domain.dto.response.TranscriptionResult
import com.ingstech.voicetranscribe.domain.entities.Transcription
import com.ingstech.voicetranscribe.domain.enums.TranscriptionType
import com.ingstech.voicetranscribe.infrestructure.TranscriptionRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ListOperations
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.multipart.MultipartFile
import org.springframework.mock.web.MockMultipartFile
import java.util.*
import javax.sound.sampled.AudioSystem

@Service
class TranscriptionService(
    private val transcriptionRepository: TranscriptionRepository,
    private val redisTemplate: RedisTemplate<String, ByteArray>,
    @Value("\${gemini.api.key}") private val geminiApiKey: String,
    @Value("\${gemini.api.url}") private val geminiApiUrl: String
) {

    private val restTemplate = RestTemplate()

    private fun getRedisKey(sessionId: String) = "audio_chunks:$sessionId"

    fun saveAudioChunk(sessionId: String, chunk: ByteArray) {
        redisTemplate.opsForList().rightPush(getRedisKey(sessionId), chunk)
    }

    fun transcribeSessionAudio(sessionId: String): Transcription? {
        val key = getRedisKey(sessionId)
        val ops: ListOperations<String, ByteArray> = redisTemplate.opsForList()
        val size = ops.size(key) ?: 0
        if (size == 0L) return null

        val chunks = ops.range(key, 0, -1) ?: emptyList()
        redisTemplate.delete(key)

        val fullAudio = mergeChunks(chunks)
        val multipartFile = MockMultipartFile(
            "file",
            "live-audio.wav",
            "audio/wav",
            fullAudio
        )

        return transcribeLiveAudio(multipartFile)
    }

    private fun mergeChunks(chunks: List<ByteArray>): ByteArray {
        val outputStream = java.io.ByteArrayOutputStream()
        chunks.forEach { outputStream.write(it) }
        return outputStream.toByteArray()
    }

    fun transcribeAudioFile(audioFile: MultipartFile): Transcription {
        val transcriptionResult = transcribeWithGemini(audioFile)
        val entity = Transcription(
            fileName = audioFile.originalFilename ?: "unknown",
            transcribedText = transcriptionResult.text,
            duration = transcriptionResult.duration,
            fileSize = audioFile.size,
            mimeType = audioFile.contentType ?: "unknown",
            transcriptionType = TranscriptionType.FILE_UPLOAD,
            confidence = transcriptionResult.confidence
        )
        return transcriptionRepository.save(entity)
    }

    fun transcribeLiveAudio(audioFile: MultipartFile): Transcription {
        val transcriptionResult = transcribeWithGemini(audioFile)
        val entity = Transcription(
            fileName = "live-recording-${System.currentTimeMillis()}.wav",
            transcribedText = transcriptionResult.text,
            duration = transcriptionResult.duration,
            fileSize = audioFile.size,
            mimeType = audioFile.contentType ?: "audio/wav",
            transcriptionType = TranscriptionType.LIVE_RECORDING,
            confidence = transcriptionResult.confidence
        )
        return transcriptionRepository.save(entity)
    }

    private fun transcribeWithGemini(audioFile: MultipartFile): TranscriptionResult {
        return try {
            val base64Audio = Base64.getEncoder().encodeToString(audioFile.bytes)
            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(
                            GeminiPart(
                                text = """
                                    Transcreva este áudio para texto em português brasileiro. 
                                    Retorne apenas o texto transcrito, sem comentários adicionais.
                                    Se não conseguir transcrever, retorne "Não foi possível transcrever o áudio".
                                """.trimIndent()
                            ),
                            GeminiPart(
                                inlineData = GeminiInlineData(
                                    mimeType = getSupportedMimeType(audioFile.contentType),
                                    data = base64Audio
                                )
                            )
                        )
                    )
                )
            )
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                accept = listOf(MediaType.APPLICATION_JSON)
            }
            val entity = HttpEntity(request, headers)
            val url = "$geminiApiUrl?key=$geminiApiKey"
            val response = restTemplate.postForObject(url, entity, GeminiResponse::class.java)
            val transcribedText = response?.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?.trim()
                ?: "Não foi possível transcrever o áudio"
            val duration = getAudioDuration(audioFile)
            TranscriptionResult(transcribedText, null, duration)
        } catch (e: Exception) {
            println("Erro ao transcrever com Gemini: ${e.message}")
            e.printStackTrace()
            TranscriptionResult(
                "Erro na transcrição: ${e.message}",
                null,
                getAudioDuration(audioFile)
            )
        }
    }

    private fun getSupportedMimeType(contentType: String?): String {
        return when (contentType?.lowercase()) {
            "audio/wav", "audio/wave" -> "audio/wav"
            "audio/mp3", "audio/mpeg" -> "audio/mp3"
            "audio/flac" -> "audio/flac"
            "audio/ogg" -> "audio/ogg"
            "audio/webm" -> "audio/webm"
            else -> "audio/wav"
        }
    }

    private fun getAudioDuration(audioFile: MultipartFile): Double {
        return try {
            val audioInputStream = AudioSystem.getAudioInputStream(audioFile.inputStream)
            val format = audioInputStream.format
            val frames = audioInputStream.frameLength
            if (frames != -1L && format.frameRate > 0) {
                (frames / format.frameRate).toDouble()
            } else {
                0.0
            }
        } catch (e: Exception) {
            (audioFile.size / 32000.0)
        }
    }

    fun getAllTranscriptions(): List<Transcription> =
        transcriptionRepository.findAllByOrderByCreatedAtDesc()

    fun getTranscriptionById(id: Long): Transcription? =
        transcriptionRepository.findById(id).orElse(null)

    fun deleteTranscription(id: Long) =
        transcriptionRepository.deleteById(id)

    fun searchTranscriptions(searchTerm: String): List<Transcription> =
        transcriptionRepository.findByTranscribedTextContainingIgnoreCaseOrderByCreatedAtDesc(searchTerm)

    fun getTranscriptionsByType(type: TranscriptionType): List<Transcription> =
        transcriptionRepository.findByTranscriptionTypeOrderByCreatedAtDesc(type)
}