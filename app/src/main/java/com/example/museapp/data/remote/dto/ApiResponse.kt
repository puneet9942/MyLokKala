package com.example.museapp.data.remote.dto

import com.squareup.moshi.Json

// imports: com.squareup.moshi.Json

data class ApiResponse<T>(
    // new-style
    @Json(name = "status") val status: String? = null,             // e.g. "success"
    @Json(name = "status_code") val status_code: Int? = null,

    // legacy-style (existing assets)
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "code") val code: Int? = null,

    val message: String? = null,
    val timestamp: String? = null,
    val request_id: String? = null,

    val data: T? = null,

    // new API includes explicit error field
    val error: Any? = null
) {
    fun isSuccessful(): Boolean =
        (success == true) || (status?.equals("success", ignoreCase = true) == true)

    fun httpCode(): Int =
        status_code ?: code ?: 0
}
