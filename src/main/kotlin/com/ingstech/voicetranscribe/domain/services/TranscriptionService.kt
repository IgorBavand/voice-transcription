package com.ingstech.voicetranscribe.domain.services

import com.fasterxml.jackson.annotation.JsonProperty
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
import java.util.*
import javax.sound.sampled.AudioSystem

data class TranscriptionResult(
    val text: String,
    val confidence: Double?,
    val duration: Double
)

// Gemini API Request/Response Models
data class GeminiRequest(
    val contents: List<GeminiContent>
)

data class GeminiContent(
    val parts: List<GeminiPart>
)

data class GeminiPart(
    val text: String? = null,
    @JsonProperty("inline_data")
    val inlineData: GeminiInlineData? = null
)

data class GeminiInlineData(
    @JsonProperty("mime_type")
    val mimeType: String,
    val data: String
)

data class GeminiResponse(
    val candidates: List<GeminiCandidate>
)

data class GeminiCandidate(
    val content: GeminiContent,
    val finishReason: String? = null,
    val safetyRatings: List<GeminiSafetyRating>? = null
)

data class GeminiSafetyRating(
    val category: String,
    val probability: String
)

@Service
class TranscriptionService(
    private val transcriptionRepository: TranscriptionRepository,
    @Value("\${gemini.api.key}") private val geminiApiKey: String,
    @Value("\${gemini.api.url}") private val geminiApiUrl: String
) {

    private val restTemplate = RestTemplate()

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
        try {
            // Converte o áudio para base64
            val base64Audio = Base64.getEncoder().encodeToString(audioFile.bytes)

            // Monta a requisição para o Gemini
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

            // Configura os headers
            val headers = HttpHeaders()
            headers.contentType = MediaType.APPLICATION_JSON
            headers.accept = listOf(MediaType.APPLICATION_JSON)

            val entity = HttpEntity(request, headers)

            // Faz a requisição para o Gemini
            val url = "$geminiApiUrl?key=$geminiApiKey"
            val response = restTemplate.postForObject(url, entity, GeminiResponse::class.java)

            // Extrai o texto transcrito
            val transcribedText = response?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                ?: "Não foi possível transcrever o áudio"

            val duration = getAudioDuration(audioFile)

            return TranscriptionResult(transcribedText, null, duration)

        } catch (e: Exception) {
            // Log do erro para debug
            println("Erro ao transcrever com Gemini: ${e.message}")
            e.printStackTrace()

            // Retorna um resultado de erro
            return TranscriptionResult(
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
            else -> "audio/wav" // Default fallback
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
            // Se não conseguir obter a duração, estima baseado no tamanho do arquivo
            (audioFile.size / 32000.0) // Estimativa aproximada para áudio de 16kHz, 16-bit
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

    fun getTranscriptionsByType(type: TranscriptionType): List<Transcription> {
        return transcriptionRepository.findByTranscriptionTypeOrderByCreatedAtDesc(type)
    }
}