package com.example.museapp.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ProfileRequestDto(
    val fullName: String? = null,
    val dob: String? = null,
    val gender: String? = null,
    val profileDescription: String? = null,
    val bio: String? = null,
    val pricingType: String? = null,
    val standardPrice: Int? = null,
    val priceMin: Int? = null,
    val priceMax: Int? = null,
    val travelRadius: Int? = null,
    val isEventManager: Boolean? = null,
    val instaId: String? = null,
    val twitterId: String? = null,
    val youtubeId: String? = null,
    val facebookId: String? = null,
    val interests: List<String> = emptyList()
)
