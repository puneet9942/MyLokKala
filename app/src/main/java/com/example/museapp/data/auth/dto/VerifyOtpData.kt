package com.example.museapp.data.auth.dto

data class UserDto(
    val id: String,
    val phone: String?,
    val pendingPhone: String?,
    val fullName: String?,
    val photo: String?,
    val dob: String?,
    val gender: String?,
    val profileVideos: Any?,   // kept generic because server may return complex shape or null
    val profilePhotos: Any?,
    val pricingType: String?,
    val standardPrice: Int?,
    val priceMin: Int?,
    val priceMax: Int?,
    val travelRadius: Int?,
    val isEventManager: Boolean?,
    val instaId: String?,
    val twitterId: String?,
    val youtubeId: String?,
    val latitude: Double?,
    val longitude: Double?,
    val profileDescription: String?,
    val bio: String?,
    val facebookId: String?,
    val refreshToken: String?,
    val createdAt: String?,
    val updatedAt: String?
)

data class VerifyOtpData(
    val user: UserDto?,
    val access_token: String?,
    val refresh_token: String?,
    val expires_in: Long?,
    val token_type: String?,     // legacy / backward compatibility
)