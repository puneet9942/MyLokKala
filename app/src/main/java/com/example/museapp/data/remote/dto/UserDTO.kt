package com.example.museapp.data.remote.dto

import com.example.museapp.domain.model.Interest
import com.example.museapp.domain.model.User
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Drop-in replacement for the existing UserDto file.
 * All Double? and Int? numeric DTO fields default to null (not 0.0 / 0).
 * This file adds missing fields returned by the profile update API and provides a toDomain() mapper.
 */

@JsonClass(generateAdapter = true)
data class InterestDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "remote_id") val remoteId: String? = null,
    @Json(name = "name") val name: String? = null,
) {
    fun toDomain(): Interest {
        // server id becomes remoteId on domain Interest; local id remains null
        return Interest(id = null, remoteId = id, name = name)
    }
}

@JsonClass(generateAdapter = true)
data class InterestItemDto(
    @Json(name = "interestId") val interestId: String? = null,
    @Json(name = "name") val name: String? = null
)

data class UserDto(
    @Json(name = "id") val id: String,
    @Json(name = "fullName") val fullName: String? = null,
    @Json(name = "profile_description") val profile_description: String? = null,
    @Json(name = "lat") val lat: Double? = null,
    @Json(name = "lng") val lng: Double? = null,
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "bio") val bio: String? = null,
    @Json(name = "isEventManager") val isEventManager: Boolean? = null,
    @Json(name = "priceMin") val priceMin: Int? = null,
    @Json(name = "priceMax") val priceMax: Int? = null,
    @Json(name = "photo") val photo: String? = null,
    @Json(name = "photos") val photos: List<String>? = emptyList(),
    @Json(name = "videos") val videos: List<String>? = emptyList(),
    @Json(name = "createdAt") val createdAt: String? = null,
    @Json(name = "updatedAt") val updatedAt: String? = null,
    @Json(name = "interests") val interests: List<InterestDto>? = emptyList(),
    @Json(name = "averageRating") val averageRating: Double? = null,
    @Json(name = "totalRatings") val totalRatings: Int? = null,

    // social ids
    @Json(name = "facebookId") val facebookId: String? = null,
    @Json(name = "twitterId") val twitterId: String? = null,
    @Json(name = "instagramId") val instagramId: String? = null,
    @Json(name = "linkedinId") val linkedinId: String? = null,
    @Json(name = "youtubeId") val youtubeId: String? = null,

    // additional profile fields
    @Json(name = "dob") val dob: String? = null,
    @Json(name = "pricingType") val pricingType: String? = null,
    @Json(name = "standardPrice") val standardPrice: Double? = null,
    @Json(name = "travelRadius") val travelRadius: Double? = null,
    @Json(name = "gender") val gender: String? = null
)

data class UsersDataDto(
    @Json(name = "users") val users: List<UserDto> = emptyList(),
    @Json(name = "pagination") val pagination: Map<String, Any>? = null
)

// Map DTO -> domain User. This uses named parameters similar to the previously provided User mapping.
// If your domain.User constructor differs, tell me the exact signature and I will adapt this function.
fun UserDto.toDomain(): User {
    val domainInterests = interests?.mapNotNull { it?.toDomain() } ?: emptyList()

    return User(
        id = id,
        fullName = fullName ?: "",
        profileDescription = profile_description,
        lat = lat,
        lng = lng,
        bio = bio,
        isEventManager = isEventManager ?: false,
        phone = phone,
        priceMin = priceMin,
        priceMax = priceMax,
        photo = photo,
        photos = photos ?: emptyList(),
        videos = videos ?: emptyList(),
        createdAt = createdAt,
        updatedAt = updatedAt,
        interests = domainInterests,
        averageRating = averageRating ?: 0.0, // domain may expect non-null; map to 0.0 if null
        totalRatings = totalRatings ?: 0,
        facebookId = facebookId,
        twitterId = twitterId,
        instagramId = instagramId,
        linkedinId = linkedinId,
        youtubeId = youtubeId,
        dob = dob,
        pricingType = pricingType,
        standardPrice = standardPrice,
        travelRadius = travelRadius,
        gender = gender
    )
}
