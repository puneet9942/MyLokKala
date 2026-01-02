package com.example.museapp.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FeedbackResponseDto(
    @Json(name = "id") val id: String,
    @Json(name = "userId") val userId: String? = null,
    @Json(name = "feedback") val feedback: String,
    @Json(name = "createdAt") val createdAt: String? = null,
    @Json(name = "updatedAt") val updatedAt: String? = null
)
