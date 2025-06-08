package com.ingstech.voicetranscribe.api

import com.ingstech.voicetranscribe.domain.entities.Transcription
import com.ingstech.voicetranscribe.domain.services.TranscriptionService
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ListOperations
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockMultipartFile

@RestController
@RequestMapping("/audio")
class StreamAudioController @Autowired constructor(
    private val redisTemplate: RedisTemplate<String, ByteArray>,
    private val transcriptionService: TranscriptionService
) {

    private fun getRedisKey(sessionId: String) = "audio_chunks:$sessionId"

    @PostMapping("/stream")
    fun receiveAudioChunk(
        @RequestParam("audio") file: MultipartFile,
        @RequestParam("sessionId") sessionId: String
    ): ResponseEntity<String> {

        val chunk = file.bytes
        val key = getRedisKey(sessionId)
        redisTemplate.opsForList().rightPush(key, chunk)
        return ResponseEntity.ok("Chunk recebido")
    }

    @PostMapping("/finish")
    fun finishRecording(
        @RequestParam("sessionId") sessionId: String
    ): ResponseEntity<Transcription> {
        val key = getRedisKey(sessionId)
        val ops: ListOperations<String, ByteArray> = redisTemplate.opsForList()
        val size = ops.size(key) ?: 0
        if (size == 0L) {
            return ResponseEntity.badRequest().body(null)
        }

        val chunks = ops.range(key, 0, -1) ?: emptyList()
        redisTemplate.delete(key)

        val outputStream = java.io.ByteArrayOutputStream()
        for (chunk in chunks) {
            outputStream.write(chunk)
        }
        val fullAudio = outputStream.toByteArray()

        val multipartFile = MockMultipartFile(
            "file",
            "live-audio.wav",
            "audio/wav",
            fullAudio
        )

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .body(transcriptionService.transcribeAudioFile(multipartFile))
    }
}