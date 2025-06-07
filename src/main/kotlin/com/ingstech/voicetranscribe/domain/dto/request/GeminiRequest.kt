package com.ingstech.voicetranscribe.domain.dto.request

import com.fasterxml.jackson.annotation.JsonProperty

data class GeminiRequest(
    val contents: List<GeminiContent>
)

data class GeminiContent(
    val parts: List<GeminiPart>
)

data class GeminiPart(
    val text: String? = null,
    @JsonProperty("inline_data")
    val inlineData: GeminiInlineData? = null
)

data class GeminiInlineData(
    @JsonProperty("mime_type")
    val mimeType: String,
    val data: String
)