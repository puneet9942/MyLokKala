package com.example.museapp.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Represents the "data" object returned from /api/user/feedback
 *
 * Example:
 * "data": {
 *   "feedback": { "id": "...", "userId": "...", "feedback": "...", ... }
 * }
 */
@JsonClass(generateAdapter = true)
data class FeedbackDataDto(
    @Json(name = "feedback") val feedback: FeedbackResponseDto?
)
