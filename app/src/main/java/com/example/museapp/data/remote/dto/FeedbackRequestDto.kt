package com.example.museapp.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FeedbackRequestDto(
    @Json(name = "feedback")
    val feedback: String
)
