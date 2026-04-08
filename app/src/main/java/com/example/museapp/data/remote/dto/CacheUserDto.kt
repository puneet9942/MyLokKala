package com.example.museapp.data.remote.dto

import com.example.museapp.domain.model.CacheUser
import com.example.museapp.domain.model.Interest
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CacheUserDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "fullName") val fullName: String? = null,
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "photo") val photo: String? = null,
    @Json(name = "dob") val dob: String? = null,
    @Json(name = "gender") val gender: String? = null,
    @Json(name = "profilePhotos") val profilePhotos: List<String>? = null,
    @Json(name = "profileVideos") val profileVideos: List<String>? = null,
    @Json(name = "pricingType") val pricingType: String? = null,
    @Json(name = "standardPrice") val standardPrice: Long? = null,
    @Json(name = "priceMin") val priceMin: Long? = null,
    @Json(name = "priceMax") val priceMax: Long? = null,
    @Json(name = "travelRadius") val travelRadius: Int? = null,
    @Json(name = "isEventManager") val isEventManager: Boolean? = null,
    @Json(name = "instaId") val instaId: String? = null,
    @Json(name = "twitterId") val twitterId: String? = null,
    @Json(name = "youtubeId") val youtubeId: String? = null,
    @Json(name = "latitude") val latitude: Double? = null,
    @Json(name = "longitude") val longitude: Double? = null,
    @Json(name = "profileDescription") val profileDescription: String? = null,
    @Json(name = "bio") val bio: String? = null,
    @Json(name = "facebookId") val facebookId: String? = null,
    @Json(name = "createdAt") val createdAt: String? = null,
    @Json(name = "updatedAt") val updatedAt: String? = null,
    // <- tolerant: allow string list OR object list OR null
    @Json(name = "interests") val interests: List<Any>? = null,
    @Json(name = "averageRating") val averageRating: Double? = null,
    @Json(name = "totalRatings") val totalRatings: Int? = null,
    @Json(name = "favoriteUsers") val favoriteUsers: List<FavoritedUserDto>? = null
) {
    /**
     * Convert CacheUserDto -> CacheUser (cache-specific domain model).
     * This is tolerant of mixed shapes for `interests`:
     * - List of strings
     * - List of objects/maps like { "name": "Guitar" }
     * - null
     */
    fun toDomain(): CacheUser {
        val domainInterests: List<Interest> = interests?.mapNotNull { raw ->
            when (raw) {
                is String -> Interest(name = raw)
                is Map<*, *> -> {
                    // safe-extract common keys
                    val n = raw["name"] as? String ?: raw["interest"] as? String ?: raw["title"] as? String
                    n?.let { Interest(name = it) }
                }
                // sometimes Moshi will create LinkedHashMap for JSON objects -> handled above
                else -> null
            }
        } ?: emptyList()

        return CacheUser(
            id = id,
            fullName = fullName,
            phone = phone,
            photo = photo,
            dob = dob,
            gender = gender,
            profilePhotos = profilePhotos ?: emptyList(),
            profileVideos = profileVideos ?: emptyList(),
            pricingType = pricingType,
            standardPrice = standardPrice,
            priceMin = priceMin,
            priceMax = priceMax,
            travelRadius = travelRadius,
            isEventManager = isEventManager ?: false,
            instaId = instaId,
            twitterId = twitterId,
            youtubeId = youtubeId,
            latitude = latitude,
            longitude = longitude,
            profileDescription = profileDescription,
            bio = bio,
            facebookId = facebookId,
            createdAt = createdAt,
            updatedAt = updatedAt,
            interests = domainInterests,
            averageRating = averageRating,
            totalRatings = totalRatings,
            favoriteUsers = favoriteUsers ?: emptyList()
        )
    }
}
