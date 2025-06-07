package com.ingstech.voicetranscribe.domain.services

import com.google.cloud.speech.v1.RecognitionAudio
import com.google.cloud.speech.v1.RecognitionConfig
import com.google.cloud.speech.v1.SpeechClient
import com.google.protobuf.ByteString
import com.ingstech.voicetranscribe.domain.entities.Transcription
import com.ingstech.voicetranscribe.domain.enums.TranscriptionType
import com.ingstech.voicetranscribe.infrestructure.TranscriptionRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.multipart.MultipartFile
import javax.sound.sampled.AudioSystem

data class TranscriptionResult(
    val text: String,
    val confidence: Double?,
    val duration: Double
)

data class GeminiRequest(
    val contents: List<GeminiContent>
)

data class GeminiContent(
    val parts: List<GeminiPart>
)

data class GeminiPart(
    val text: String? = null,
    val inlineData: GeminiInlineData? = null
)

data class GeminiInlineData(
    val mimeType: String,
    val data: String
)

data class GeminiResponse(
    val candidates: List<GeminiCandidate>
)

data class GeminiCandidate(
    val content: GeminiContent
)

@Service
class TranscriptionService(
    private val transcriptionRepository: TranscriptionRepository,
    @Value("\${gemini.api.key}") private val geminiApiKey: String,
    @Value("\${gemini.api.url}") private val geminiApiUrl: String
) {

    private val restTemplate = RestTemplate()

    fun transcribeAudioFile(audioFile: MultipartFile): Transcription {
        val transcriptionResult = transcribeWithGoogleCloudSpeech(audioFile)

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

    private fun transcribeWithGoogleCloudSpeech(audioFile: MultipartFile): TranscriptionResult {
        try {
            val speechClient = SpeechClient.create()

            val audioBytes = ByteString.copyFrom(audioFile.bytes)
            val audio = RecognitionAudio.newBuilder().setContent(audioBytes).build()

            val config = RecognitionConfig.newBuilder()
                .setEncoding(getAudioEncoding(audioFile.contentType))
                .setSampleRateHertz(16000)
                .setLanguageCode("pt-BR")
                .setEnableAutomaticPunctuation(true)
                .build()

            val response = speechClient.recognize(config, audio)
            val results = response.resultsList

            if (results.isEmpty()) {
                return TranscriptionResult("Nenhum áudio detectado", null, 0.0)
            }

            val transcript = results.joinToString(" ") { result ->
                result.alternativesList.firstOrNull()?.transcript ?: ""
            }

            val confidence = results.mapNotNull { result ->
                result.alternativesList.firstOrNull()?.confidence
            }.average()

            val duration = getAudioDuration(audioFile)

            speechClient.close()

            return TranscriptionResult(transcript, confidence, duration)

        } catch (e: Exception) {
            throw RuntimeException("Erro ao transcrever áudio com Google Cloud Speech: ${e.message}", e)
        }
    }

    private fun transcribeWithGemini(audioFile: MultipartFile): TranscriptionResult {
        try {
            val base64Audio = java.util.Base64.getEncoder().encodeToString(audioFile.bytes)

            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(
                            GeminiPart(text = "Transcreva este áudio para texto em português:"),
                            GeminiPart(
                                inlineData = GeminiInlineData(
                                    mimeType = audioFile.contentType ?: "audio/wav",
                                    data = base64Audio
                                )
                            )
                        )
                    )
                )
            )

            val headers = HttpHeaders()
            headers.contentType = MediaType.APPLICATION_JSON
            headers.set("x-goog-api-key", geminiApiKey)

            val entity = HttpEntity(request, headers)
            val response = restTemplate.postForObject(
                "$geminiApiUrl?key=$geminiApiKey",
                entity,
                GeminiResponse::class.java
            )

            val transcribedText = response?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "Não foi possível transcrever o áudio"

            val duration = getAudioDuration(audioFile)

            return TranscriptionResult(transcribedText, null, duration)

        } catch (e: Exception) {
            throw RuntimeException("Erro ao transcrever áudio com Gemini: ${e.message}", e)
        }
    }

    private fun getAudioEncoding(contentType: String?): RecognitionConfig.AudioEncoding {
        return when (contentType?.lowercase()) {
            "audio/wav" -> RecognitionConfig.AudioEncoding.LINEAR16
            "audio/flac" -> RecognitionConfig.AudioEncoding.FLAC
            "audio/ogg" -> RecognitionConfig.AudioEncoding.OGG_OPUS
            else -> RecognitionConfig.AudioEncoding.LINEAR16
        }
    }

    private fun getAudioDuration(audioFile: MultipartFile): Double {
        return try {
            val audioInputStream = AudioSystem.getAudioInputStream(audioFile.inputStream)
            val format = audioInputStream.format
            val frames = audioInputStream.frameLength
            (frames / format.frameRate).toDouble()
        } catch (e: Exception) {
            0.0
        }
    }

    fun getAllTranscriptions(): List<Transcription> {
        return transcriptionRepository.findAllByOrderByCreatedAtDesc()
    }

    fun getTranscriptionById(id: Long): Transcription? {
        return transcriptionRepository.findById(id).orElse(null)
    }

    fun deleteTranscription(id: Long) {
        transcriptionRepository.deleteById(id)
    }

    fun searchTranscriptions(searchTerm: String): List<Transcription> {
        return transcriptionRepository.findByTranscribedTextContainingIgnoreCaseOrderByCreatedAtDesc(searchTerm)
    }
}