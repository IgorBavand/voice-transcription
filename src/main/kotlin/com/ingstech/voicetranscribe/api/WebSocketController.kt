package com.ingstech.voicetranscribe.api

import com.ingstech.voicetranscribe.domain.services.TranscriptionService
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller
import java.nio.ByteBuffer

@Controller
class WebSocketAudioController(
    private val simpMessagingTemplate: SimpMessagingTemplate,
    private val transcriptionService: TranscriptionService
) {

    @MessageMapping("/transcribe-audio")
    fun handleAudioChunk(audioChunkData: ByteBuffer) {
        try {
            val audioBytes = ByteArray(audioChunkData.remaining())
            audioChunkData.get(audioBytes)

            val partialTranscription = "Backend processou chunk: ${String(audioBytes.take(10).toByteArray())}... (Timestamp: ${System.currentTimeMillis()})"
            println("Recebido chunk de áudio, enviando transcrição: $partialTranscription")

            simpMessagingTemplate.convertAndSend("/topic/live-transcription", partialTranscription)

        } catch (e: Exception) {
            println("Erro ao processar chunk de áudio via WebSocket: ${e.message}")
            simpMessagingTemplate.convertAndSend("/topic/live-transcription-error", "Erro no servidor: ${e.message}")
        }
    }
}