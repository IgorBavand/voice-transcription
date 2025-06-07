package com.ingstech.voicetranscribe.infrestructure

import com.ingstech.voicetranscribe.domain.entities.Transcription
import com.ingstech.voicetranscribe.domain.enums.TranscriptionType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface TranscriptionRepository : JpaRepository<Transcription, Long> {

    fun findByTranscriptionTypeOrderByCreatedAtDesc(type: TranscriptionType): List<Transcription>

    fun findAllByOrderByCreatedAtDesc(): List<Transcription>

    @Query("SELECT t FROM Transcription t WHERE t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    fun findByDateRangeOrderByCreatedAtDesc(startDate: LocalDateTime, endDate: LocalDateTime): List<Transcription>

    @Query("SELECT t FROM Transcription t WHERE LOWER(t.transcribedText) LIKE LOWER(CONCAT('%', :searchTerm, '%')) ORDER BY t.createdAt DESC")
    fun findByTranscribedTextContainingIgnoreCaseOrderByCreatedAtDesc(searchTerm: String): List<Transcription>
}