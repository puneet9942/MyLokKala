package com.example.museapp.data.remote.mapper

import com.example.museapp.data.remote.dto.AdDto
import com.example.museapp.data.remote.dto.FeedbackResponseDto
import com.example.museapp.data.remote.dto.InterestDto
import com.example.museapp.data.remote.dto.UserDto
import com.example.museapp.data.remote.dto.ProfileDataDto
import com.example.museapp.data.remote.dto.ProfileRequestDto
import com.example.museapp.domain.model.Ad
import com.example.museapp.domain.model.Feedback
import com.example.museapp.domain.model.Interest
import com.example.museapp.domain.model.User
import com.example.museapp.presentation.feature.profile.ProfileUiState
import java.nio.charset.StandardCharsets
import java.util.UUID

fun InterestDto.toDomain(): Interest {
    // prefer explicit server id if present; otherwise generate deterministic id from name
    val fallbackId = name?.let {
        UUID.nameUUIDFromBytes(it.trim().lowercase().toByteArray(StandardCharsets.UTF_8)).toString()
    } ?: UUID.randomUUID().toString()

    // Use DTO.id (server id) as remoteId; fallback to deterministic UUID if null
    val resolvedRemoteId = id ?: fallbackId

    // fallback display name if missing
    val displayName = name?.trim().takeIf { !it.isNullOrBlank() } ?: "Unknown"

    return Interest(
        remoteId = resolvedRemoteId,
        name = displayName
    )
}

fun UserDto.toDomain(): User {
    // map interests defensively: skip null entries and ensure name is present
    val domainInterests = this.interests
        ?.mapNotNull { dto ->
            if (dto == null) return@mapNotNull null
            val n = dto.name?.trim()
            if (n.isNullOrEmpty()) return@mapNotNull null
            dto.toDomain()
        } ?: emptyList()

    return User(
        id = this.id ?: "",
        fullName = this.fullName ?: "",
        profileDescription = this.profile_description,
        lat = this.lat,
        lng = this.lng,
        phone = this.phone,
        priceMin = this.priceMin,
        priceMax = this.priceMax,
        bio = this.bio,
        photo = this.photo,
        isEventManager = this.isEventManager ?: false,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        interests = domainInterests,
        averageRating = this.averageRating ?: 0.0,
        totalRatings = this.totalRatings ?: 0
    )
}

fun AdDto.toDomain(): Ad = Ad(
    id = id,
    user = user.toDomain(),
    primarySkill = primarySkill,
    priceMin = priceMin,
    priceMax = priceMax,
    description = description,
    createdAt = createdAt
)

fun FeedbackResponseDto.toDomain(): Feedback = Feedback(
    id = this.id,
    userId = this.userId,
    feedback = this.feedback,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt
)

fun ProfileUiState.toProfileRequestDto(): ProfileRequestDto {
    return ProfileRequestDto(
        fullName = this.name,
        dob = this.dob,
        gender = this.gender,
        profileDescription = this.description,
        bio = this.biography,
        pricingType = this.pricingType,
        standardPrice = this.standardPrice.toIntOrNull(),
        priceMin = this.minPrice.toIntOrNull(),
        priceMax = this.maxPrice.toIntOrNull(),
        travelRadius = this.travelRadiusKm.toIntOrNull(),
        isEventManager = this.isEventManager ?: false,
        instaId = this.instaId,
        twitterId = this.twitterId,
        youtubeId = this.youtubeId,
        facebookId = this.facebookId,
        interests = this.interests
        // Note: if backend supports 'mobile' field, add mobile = this.mobile
    )
}

/**
 * Map ProfileDataDto (returned by profile update endpoint) into domain User.
 * If ProfileDataDto.user is null, return a safe default User instance to avoid null handling
 * in callers. This follows the project's defensive mapping style used above.
 */
fun ProfileDataDto.toDomain(): User {
    val userDto = this.user
    return if (userDto != null) {
        userDto.toDomain()
    } else {
        // Safe default user - mirrors fields used across the app
        User(
            id = "",
            fullName = "",
            profileDescription = null,
            lat = null,
            lng = null,
            phone = null,
            priceMin = null,
            priceMax = null,
            bio = null,
            photo = null,
            isEventManager = false,
            createdAt = null,
            updatedAt = null,
            interests = emptyList(),
            averageRating = 0.0,
            totalRatings = 0
        )
    }
}
