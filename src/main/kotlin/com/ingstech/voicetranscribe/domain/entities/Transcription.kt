package com.ingstech.voicetranscribe.domain.entities

import com.ingstech.voicetranscribe.domain.enums.TranscriptionType
import jakarta.persistence.Column
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table;
import lombok.Data
import java.time.LocalDateTime

@Entity
@Table(name = "transcriptions")
@Data
data class Transcription(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val fileName: String,

    @Column(nullable = false, length = 10000)
    val transcribedText: String,

    @Column(nullable = false)
    val duration: Double,

    @Column(nullable = false)
    val fileSize: Long,

    @Column(nullable = false)
    val mimeType: String,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val transcriptionType: TranscriptionType,

    @Column
    val confidence: Double? = null
)