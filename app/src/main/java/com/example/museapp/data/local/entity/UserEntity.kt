package com.example.museapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Full UserEntity storing all common user/profile fields returned by the backend.
 * Complex lists are stored as JSON strings (see Converters.kt and AppDatabase @TypeConverters).
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,

    @ColumnInfo(name = "full_name") val fullName: String? = null,
    @ColumnInfo(name = "phone") val phone: String? = null,
    @ColumnInfo(name = "photo") val photo: String? = null,
    @ColumnInfo(name = "dob") val dob: String? = null,
    @ColumnInfo(name = "gender") val gender: String? = null,

    // lists stored as JSON
    @ColumnInfo(name = "profile_photos_json") val profilePhotosJson: String? = null,
    @ColumnInfo(name = "profile_videos_json") val profileVideosJson: String? = null,

    @ColumnInfo(name = "pricing_type") val pricingType: String? = null,
    @ColumnInfo(name = "standard_price") val standardPrice: Int? = null,
    @ColumnInfo(name = "price_min") val priceMin: Int? = null,
    @ColumnInfo(name = "price_max") val priceMax: Int? = null,
    @ColumnInfo(name = "travel_radius") val travelRadius: Int? = null,

    @ColumnInfo(name = "is_event_manager") val isEventManager: Boolean? = null,

    @ColumnInfo(name = "instagram_id") val instagramId: String? = null,
    @ColumnInfo(name = "twitter_id") val twitterId: String? = null,
    @ColumnInfo(name = "youtube_id") val youtubeId: String? = null,
    @ColumnInfo(name = "facebook_id") val facebookId: String? = null,

    @ColumnInfo(name = "latitude") val latitude: Double? = null,
    @ColumnInfo(name = "longitude") val longitude: Double? = null,

    @ColumnInfo(name = "profile_description") val profileDescription: String? = null,
    @ColumnInfo(name = "bio") val bio: String? = null,

    @ColumnInfo(name = "created_at") val createdAt: String? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: String? = null,

    // interests and favoriteUsers are stored as JSON strings
    @ColumnInfo(name = "interests_json") val interestsJson: String? = null,
    @ColumnInfo(name = "favorite_users_json") val favoriteUsersJson: String? = null,

    @ColumnInfo(name = "average_rating") val averageRating: Double? = null,
    @ColumnInfo(name = "total_ratings") val totalRatings: Int? = null,

    // raw JSON response from server if you want to keep it
    @ColumnInfo(name = "raw_json") val rawJson: String? = null
)
