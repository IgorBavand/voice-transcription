package com.ingstech.voicetranscribe.domain.dto.response

import com.ingstech.voicetranscribe.domain.dto.request.GeminiContent

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


data class TranscriptionResult(
    val text: String,
    val confidence: Double?,
    val duration: Double
)

