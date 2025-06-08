package com.ingstech.voicetranscribe.api

import com.ingstech.voicetranscribe.domain.entities.Transcription
import com.ingstech.voicetranscribe.domain.services.TranscriptionService
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.beans.factory.annotation.Autowired

@RestController
@RequestMapping("/audio")
class StreamAudioController @Autowired constructor(
    private val transcriptionService: TranscriptionService
) {

    @PostMapping("/stream")
    fun receiveAudioChunk(
        @RequestParam("audio") file: MultipartFile,
        @RequestParam("sessionId") sessionId: String
    ): ResponseEntity<String> {
        transcriptionService.saveAudioChunk(sessionId, file.bytes)
        return ResponseEntity.ok("Chunk received")
    }

    @PostMapping("/finish")
    fun finishRecording(
        @RequestParam("sessionId") sessionId: String
    ): ResponseEntity<Transcription> {
        val transcription = transcriptionService.transcribeSessionAudio(sessionId)
            ?: return ResponseEntity.badRequest().body(null)
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .body(transcription)
    }
}