package com.example.museapp.domain.model

/**
 * Cache-specific domain model that mirrors the API `data` object for profile.
 * This is intentionally separate from your existing `User` model.
 */
data class CacheUser(
    val id: String? = null,
    val fullName: String? = null,
    val phone: String? = null,
    val photo: String? = null,
    val dob: String? = null,
    val gender: String? = null,
    val profilePhotos: List<String> = emptyList(),
    val profileVideos: List<String> = emptyList(),
    val pricingType: String? = null,
    val standardPrice: Long? = null,
    val priceMin: Long? = null,
    val priceMax: Long? = null,
    val travelRadius: Int? = null,
    val isEventManager: Boolean = false,
    val instaId: String? = null,
    val twitterId: String? = null,
    val youtubeId: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val profileDescription: String? = null,
    val bio: String? = null,
    val facebookId: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val interests: List<Interest> = emptyList(),
    val averageRating: Double? = null,
    val totalRatings: Int? = null,
    val favoriteUsers: List<String> = emptyList()
)
