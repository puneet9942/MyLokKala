package com.example.museapp.data.local.mapper

import com.example.museapp.data.local.entity.UserEntity
import com.example.museapp.domain.model.Interest
import com.example.museapp.domain.model.User
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

fun User.toEntity(moshi: Moshi, rawJson: String? = null): UserEntity {
    val interestListType = Types.newParameterizedType(List::class.java, Interest::class.java)
    val interestAdapter: JsonAdapter<List<Interest>> = moshi.adapter(interestListType)
    val interestsJson = try { interestAdapter.toJson(this.interests) } catch (_: Exception) { null }

    val stringListType = Types.newParameterizedType(List::class.java, String::class.java)
    val stringListAdapter: JsonAdapter<List<String>> = moshi.adapter(stringListType)
    val photosJson = try { if (this.photos.isEmpty()) null else stringListAdapter.toJson(this.photos) } catch (_: Exception) { null }
    val videosJson = try { if (this.videos.isEmpty()) null else stringListAdapter.toJson(this.videos) } catch (_: Exception) { null }

    return UserEntity(
        id = this.id,
        fullName = this.fullName,
        profileDescription = this.profileDescription,
        latitude = this.lat,
        longitude = this.lng,
        bio = this.bio,
        isEventManager = this.isEventManager == true,
        phone = this.phone,
        priceMin = this.priceMin,
        priceMax = this.priceMax,
        photo = this.photo,
        profilePhotosJson = photosJson,
        profileVideosJson = videosJson,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        interestsJson = interestsJson,
        averageRating = this.averageRating,
        totalRatings = this.totalRatings,
        facebookId = this.facebookId,
        twitterId = this.twitterId,
        instagramId = this.instagramId,
        youtubeId = this.youtubeId,
        dob = this.dob,
        pricingType = this.pricingType,
        standardPrice = this.standardPrice as Int?,
        travelRadius = this.travelRadius as Int?,
        gender = this.gender,
        rawJson = rawJson
    )
}

fun UserEntity.toDomain(moshi: Moshi): User {
    val interestListType = Types.newParameterizedType(List::class.java, Interest::class.java)
    val interestAdapter: JsonAdapter<List<Interest>> = moshi.adapter(interestListType)

    val interests: List<Interest> = try {
        if (this.interestsJson.isNullOrBlank()) emptyList() else interestAdapter.fromJson(this.interestsJson) ?: emptyList()
    } catch (_: Exception) { emptyList() }

    val stringListType = Types.newParameterizedType(List::class.java, String::class.java)
    val stringAdapter: JsonAdapter<List<String>> = moshi.adapter(stringListType)

    val photos: List<String> = try {
        when {
            !this.profilePhotosJson.isNullOrBlank() -> stringAdapter.fromJson(this.profileVideosJson) ?: emptyList()
            !this.rawJson.isNullOrBlank() -> {
                val mapAdapter = moshi.adapter(Map::class.java)
                val map = mapAdapter.fromJson(this.rawJson) ?: emptyMap<String, Any>()
                val v = map["photos"]
                when (v) {
                    is List<*> -> v.filterIsInstance<String>()
                    is String -> stringAdapter.fromJson(v) ?: emptyList()
                    else -> emptyList()
                }
            }
            else -> emptyList()
        }
    } catch (_: Exception) { emptyList() }

    val videos: List<String> = try {
        when {
            !this.profileVideosJson.isNullOrBlank() -> stringAdapter.fromJson(this.profileVideosJson) ?: emptyList()
            !this.rawJson.isNullOrBlank() -> {
                val mapAdapter = moshi.adapter(Map::class.java)
                val map = mapAdapter.fromJson(this.rawJson) ?: emptyMap<String, Any>()
                val v = map["videos"]
                when (v) {
                    is List<*> -> v.filterIsInstance<String>()
                    is String -> stringAdapter.fromJson(v) ?: emptyList()
                    else -> emptyList()
                }
            }
            else -> emptyList()
        }
    } catch (_: Exception) { emptyList() }

    fun <T> fallbackFromRaw(keyCandidates: List<String>, parse: (Any) -> T?): T? {
        if (this.rawJson.isNullOrBlank()) return null
        return try {
            val mapAdapter = moshi.adapter(Map::class.java)
            val map = mapAdapter.fromJson(this.rawJson) ?: emptyMap<String, Any>()
            for (k in keyCandidates) {
                val v = map[k] ?: map[k.lowercase()]
                if (v != null) {
                    parse(v).let { if (it != null) return it }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    val dobValue: String? = this.dob ?: fallbackFromRaw(listOf("dob", "date_of_birth", "birth_date")) { it as? String }
    val pricingTypeValue: String? = this.pricingType ?: fallbackFromRaw(listOf("pricing_type", "pricingType")) { it as? String }
    val standardPriceValue: Double? =
        (this.standardPrice ?: fallbackFromRaw(listOf("standard_price", "standardprice", "price")) {
            when (it) {
                is Number -> it.toDouble()
                is String -> it.toDoubleOrNull()
                else -> null
            }
        }) as Double?
    val travelRadiusValue: Double? =
        (this.travelRadius ?: fallbackFromRaw(listOf("travel_radius", "travelRadius", "radius")) {
            when (it) {
                is Number -> it.toDouble()
                is String -> it.toDoubleOrNull()
                else -> null
            }
        }) as Double?
    val genderValue: String? = this.gender ?: fallbackFromRaw(listOf("gender", "sex")) { it as? String }

    return User(
        id = this.id,
        fullName = this.fullName ?: "",
        profileDescription = this.profileDescription,
        lat = this.latitude,
        lng = this.longitude,
        bio = this.bio,
        isEventManager = this.isEventManager,
        phone = this.phone,
        priceMin = this.priceMin,
        priceMax = this.priceMax,
        photo = this.photo,
        photos = photos,
        videos = videos,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        interests = interests,
        averageRating = this.averageRating ?: 0.0,
        totalRatings = this.totalRatings ?: 0,
        facebookId = this.facebookId,
        twitterId = this.twitterId,
        instagramId = this.instagramId,
        youtubeId = this.youtubeId,
        dob = dobValue,
        pricingType = pricingTypeValue,
        standardPrice = standardPriceValue,
        travelRadius = travelRadiusValue,
        gender = genderValue
    )
}