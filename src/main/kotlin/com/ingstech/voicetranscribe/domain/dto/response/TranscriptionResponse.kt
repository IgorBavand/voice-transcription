package com.ingstech.voicetranscribe.domain.dto.response

class TranscriptionResponse(
    val id: Long,
    val fileName: String,
    val transcribedText: String,
    val duration: Double,
    val fileSize: Long,
    val mimeType: String,
    val createdAt: String,
    val transcriptionType: String,
    val confidence: Double?
)