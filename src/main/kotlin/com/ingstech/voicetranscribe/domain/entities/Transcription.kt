package com.ingstech.voicetranscribe.domain.entities

import com.ingstech.voicetranscribe.domain.enums.TranscriptionType
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "transcriptions")
data class Transcription(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val fileName: String = "",

    @Column(nullable = false, length = 10000)
    val transcribedText: String = "",

    @Column(nullable = false)
    val duration: Double = 0.0,

    @Column(nullable = false)
    val fileSize: Long = 0,

    @Column(nullable = false)
    val mimeType: String = "",

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val transcriptionType: TranscriptionType = TranscriptionType.FILE_UPLOAD,

    @Column
    val confidence: Double? = null
) {
    // Construtor sem argumentos exigido pelo Hibernate
    constructor() : this(
        0L,
        "",
        "",
        0.0,
        0L,
        "",
        LocalDateTime.now(),
        TranscriptionType.FILE_UPLOAD,
        null
    )
}
