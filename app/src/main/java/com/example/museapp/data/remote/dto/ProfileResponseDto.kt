package com.example.museapp.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass


@JsonClass(generateAdapter = true)
data class ProfileResponseDto(
    @Json(name = "status") val status: String? = null,
    @Json(name = "status_code") val statusCode: Int? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "timestamp") val timestamp: String? = null,
    @Json(name = "request_id") val requestId: String? = null,
    @Json(name = "data") val data: ProfileDto? = null,
    @Json(name = "error") val error: Any? = null
)


@JsonClass(generateAdapter = true)
data class ProfileDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "fullName") val fullName: String? = null,
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "photo") val photo: String? = null,
    @Json(name = "dob") val dob: String? = null,
    @Json(name = "gender") val gender: String? = null,
    @Json(name = "profilePhotos") val profilePhotos: List<String>? = null,
    @Json(name = "profileVideos") val profileVideos: List<String>? = null,
    @Json(name = "pricingType") val pricingType: String? = null,
    @Json(name = "standardPrice") val standardPrice: Int? = null,
    @Json(name = "priceMin") val priceMin: Int? = null,
    @Json(name = "priceMax") val priceMax: Int? = null,
    @Json(name = "travelRadius") val travelRadius: Int? = null,
    @Json(name = "isEventManager") val isEventManager: Boolean? = null,
    @Json(name = "instaId") val instaId: String? = null,
    @Json(name = "twitterId") val twitterId: String? = null,
    @Json(name = "youtubeId") val youtubeId: String? = null,
    @Json(name = "facebookId") val facebookId: String? = null,
    @Json(name = "latitude") val latitude: Double? = null,
    @Json(name = "longitude") val longitude: Double? = null,
    @Json(name = "profileDescription") val profileDescription: String? = null,
    @Json(name = "bio") val bio: String? = null,
    @Json(name = "createdAt") val createdAt: String? = null,
    @Json(name = "updatedAt") val updatedAt: String? = null,
    @Json(name = "interests") val interests: List<InterestDto>? = null,
    @Json(name = "averageRating") val averageRating: Double? = null,
    @Json(name = "totalRatings") val totalRatings: Int? = null,
    @Json(name = "favoriteUsers") val favoriteUsers: List<FavoritedUserDto>? = null
)


@JsonClass(generateAdapter = true)
data class FavoritedUserDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "user") val user: UserSummaryDto? = null,
    @Json(name = "favoritedAt") val favoritedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class UserSummaryDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "fullName") val fullName: String? = null,
    @Json(name = "photo") val photo: String? = null,
    @Json(name = "gender") val gender: String? = null,
    @Json(name = "pricingType") val pricingType: String? = null
)