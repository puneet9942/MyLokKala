package com.example.museapp.domain.model

import com.squareup.moshi.Json

data class Interest(
    val id: Long?=null,
    val remoteId: String?=null,
    val name: String?=null,
)

data class User(
    val id: String = "",
    val fullName: String? = null,
    val profileDescription: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val phone: String? = null,
    val bio: String? = null,
    val isEventManager: Boolean? = false,
    val priceMin: Int? = null,
    val priceMax: Int? = null,
    val photo: String? = null,
    val photos: List<String> = emptyList(),
    val videos: List<String> = emptyList(),
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val interests: List<Interest> = emptyList(),
    val averageRating: Double? = 0.0,
    val totalRatings: Int? = 0,
    val facebookId: String? = null,
    val twitterId: String? = null,
    val instagramId: String? = null,
    val linkedinId: String? = null,
    val youtubeId: String? = null,
    val dob: String? = null,
    val pricingType: String? = null,
    val standardPrice: Double? = null,
    val travelRadius: Double? = null,
    val gender: String? = null
)
