package com.example.museapp.data.remote.dto

import com.squareup.moshi.Json

/**
 * Common wrapper for all API requests. Field names are annotated to match the backend
 * snake_case JSON payload you provided.
 */


data class LocationDto(
    @Json(name = "lat") val lat: String = "",
    @Json(name = "long") val long: String = ""
)

data class CommonRequest<T>(
    val request_id: String,
    val timestamp: String,       // ISO 8601
    val app_version: String,
    val platform: String,
    val auth_token: String?,     // nullable
    val locale: String,
    val device_info: DevicesInfo,
    val payload: T,
    // nullable so existing code that ignores location will continue to work
    val location: LocationDto? = null
)