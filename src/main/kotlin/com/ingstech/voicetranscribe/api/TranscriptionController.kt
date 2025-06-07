package com.ingstech.voicetranscribe.api

import com.ingstech.voicetranscribe.domain.dto.response.ErrorResponse
import com.ingstech.voicetranscribe.domain.dto.response.TranscriptionResponse
import com.ingstech.voicetranscribe.domain.entities.Transcription
import com.ingstech.voicetranscribe.domain.services.TranscriptionService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/transcriptions")
class TranscriptionController(
    private val transcriptionService: TranscriptionService
) {

    @PostMapping("/transcribe")
    fun transcribeAudio(@RequestParam("file") file: MultipartFile): ResponseEntity<Any> {
        return try {
            if (file.isEmpty) {
                return ResponseEntity.badRequest().body(
                    ErrorResponse("Arquivo não pode estar vazio", "EMPTY_FILE")
                )
            }

            if (!isAudioFile(file.contentType)) {
                return ResponseEntity.badRequest().body(
                    ErrorResponse("Tipo de arquivo não suportado. Use arquivos de áudio (.wav, .mp3, .flac, .ogg)", "UNSUPPORTED_FILE_TYPE")
                )
            }

            val transcription = transcriptionService.transcribeAudioFile(file)
            val response = transcription.toResponse()

            ResponseEntity.ok(response)

        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse("Erro ao processar transcrição: ${e.message}", "TRANSCRIPTION_ERROR"))
        }
    }

    @PostMapping("/live-transcribe")
    fun liveTranscribe(@RequestParam("audio") audioData: MultipartFile): ResponseEntity<Any> {
        return try {
            if (audioData.isEmpty) {
                return ResponseEntity.badRequest().body(
                    ErrorResponse("Dados de áudio não podem estar vazios", "EMPTY_AUDIO_DATA")
                )
            }

            val transcription = transcriptionService.transcribeLiveAudio(audioData)
            val response = transcription.toResponse()

            ResponseEntity.ok(response)

        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse("Erro ao processar transcrição ao vivo: ${e.message}", "LIVE_TRANSCRIPTION_ERROR"))
        }
    }

    @GetMapping
    fun getAllTranscriptions(): ResponseEntity<List<TranscriptionResponse>> {
        val transcriptions = transcriptionService.getAllTranscriptions()
        val responses = transcriptions.map { it.toResponse() }
        return ResponseEntity.ok(responses)
    }

    @GetMapping("/{id}")
    fun getTranscriptionById(@PathVariable id: Long): ResponseEntity<Any> {
        val transcription = transcriptionService.getTranscriptionById(id)
        return if (transcription != null) {
            ResponseEntity.ok(transcription.toResponse())
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @DeleteMapping("/{id}")
    fun deleteTranscription(@PathVariable id: Long): ResponseEntity<Any> {
        return try {
            transcriptionService.deleteTranscription(id)
            ResponseEntity.noContent().build()
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse("Erro ao deletar transcrição: ${e.message}", "DELETE_ERROR"))
        }
    }

    @GetMapping("/search")
    fun searchTranscriptions(@RequestParam query: String): ResponseEntity<List<TranscriptionResponse>> {
        val transcriptions = transcriptionService.searchTranscriptions(query)
        val responses = transcriptions.map { it.toResponse() }
        return ResponseEntity.ok(responses)
    }

    private fun isAudioFile(contentType: String?): Boolean {
        return contentType?.startsWith("audio/") == true
    }

    private fun Transcription.toResponse(): TranscriptionResponse {
        return TranscriptionResponse(
            id = this.id,
            fileName = this.fileName,
            transcribedText = this.transcribedText,
            duration = this.duration,
            fileSize = this.fileSize,
            mimeType = this.mimeType,
            createdAt = this.createdAt.toString(),
            transcriptionType = this.transcriptionType.name,
            confidence = this.confidence
        )
    }
}