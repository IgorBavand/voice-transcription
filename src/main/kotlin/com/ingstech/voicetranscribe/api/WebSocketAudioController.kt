package com.ingstech.voicetranscribe.api

import com.ingstech.voicetranscribe.domain.services.TranscriptionService
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller

@Controller
class WebSocketAudioController(
    private val simpMessagingTemplate: SimpMessagingTemplate,
    private val transcriptionService: TranscriptionService
) {

    @MessageMapping("/transcribe-audio")
    fun handleAudioChunk(audioBytes: ByteArray, @Header("content-type") contentType: String?) {
        try {
            println("Recebido chunk de áudio binário. Content-Type: $contentType. Tamanho: ${audioBytes.size} bytes.")
            val partialTranscription = "Backend processou chunk binário (primeiros bytes): ${audioBytes.take(10).joinToString()}... (Timestamp: ${System.currentTimeMillis()})"
            println("Enviando transcrição: $partialTranscription")

            simpMessagingTemplate.convertAndSend("/topic/live-transcription", partialTranscription)

        } catch (e: Exception) {
            println("Erro ao processar chunk de áudio binário via WebSocket: ${e.message}")
            simpMessagingTemplate.convertAndSend("/topic/live-transcription-error", "Erro no servidor: ${e.message}")
        }
    }
}