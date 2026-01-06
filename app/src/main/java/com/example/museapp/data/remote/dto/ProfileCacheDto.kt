package com.example.museapp.data.remote.dto

import com.example.museapp.domain.model.CacheUser
import com.example.museapp.domain.model.User
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Typed representation of the full profile API response for caching.
 * Example server payload:
 * {
 *  "status": "success",
 *  "status_code": 200,
 *  "message": "Profile fetched successfully",
 *  "timestamp": "...",
 *  "request_id": null,
 *  "data": { ...UserDto... },
 *  "error": null
 * }
 *
 * We keep 'data' as the existing UserDto so your mappers remain valid.
 */
@JsonClass(generateAdapter = true)
data class ProfileCacheDto(
    @Json(name = "status") val status: String? = null,
    @Json(name = "status_code") val statusCode: Int? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "timestamp") val timestamp: String? = null,
    @Json(name = "request_id") val requestId: String? = null,
    @Json(name = "data") val data: CacheUserDto? = null,
    @Json(name = "error") val error: Any? = null
) {
    /**
     * Convert cached DTO -> domain User by delegating to existing UserDto.toDomain()
     * Returns null when data is absent.
     */
    fun toDomainUser(): CacheUser? = data?.toDomain()
}
