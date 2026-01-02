package com.example.museapp.presentation.feature.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.museapp.data.local.dao.UserDao
import com.example.museapp.data.local.entity.UserEntity
import com.example.museapp.domain.model.Interest
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ProfileDisplayVM"

data class ProfileDisplayState(
    val mainPhoto: String? = null,
    val rawMainPhoto: String? = null,           // raw string from DB for debugging
    val fullName: String? = null,
    val profileDescription: String? = null,
    val bio: String? = null,
    val phone: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val dob: String? = null,
    val gender: String? = null,
    val priceMin: Double? = null,
    val priceMax: Double? = null,
    val isEventManager: Boolean = false,
    val interests: List<Interest> = emptyList(),
    val rawInterestsJson: String? = null,       // raw interests JSON for debugging
    val averageRating: Double? = null,
    val totalRatings: Int? = null,
    val facebookId: String? = null,
    val twitterId: String? = null,
    val instagramId: String? = null,
    val linkedinId: String? = null,
    val youtubeId: String? = null,
    val pricingType: String? = null,
    val standardPrice: Double? = null,
    val travelRadius: Double? = null,
    val photos: List<String> = emptyList(),
    val videos: List<String> = emptyList(),
    val loading: Boolean = true
)

@HiltViewModel
class ProfileDisplayViewModel @Inject constructor(
    private val userDao: UserDao,
    private val moshi: Moshi
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileDisplayState())
    val uiState: StateFlow<ProfileDisplayState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userDao.observeUser()
                .onStart { _uiState.update { it.copy(loading = true) } }
                .collect { entity ->
                    Log.d(TAG, "observeUser emitted entity: $entity")
                    _uiState.value = entity?.let { mapEntityToState(it) } ?: ProfileDisplayState(loading = false)
                }
        }
    }

    suspend fun fetchOnce(): ProfileDisplayState {
        val entity = userDao.getUserOnce()
        Log.d(TAG, "fetchOnce returned entity: $entity")
        return entity?.let { mapEntityToState(it) } ?: ProfileDisplayState(loading = false)
    }

    private fun mapEntityToState(e: UserEntity): ProfileDisplayState {
        val dtoMap = parseJsonToMap(e.rawJson)

        // helpers
        fun preferString(vararg keys: String, fallback: String? = null): String? {
            dtoMap?.let {
                for (k in keys) {
                    val s = anyToStringOrNull(it[k])
                    if (!s.isNullOrBlank()) return s
                }
            }
            return fallback?.takeIf { it.isNotBlank() }
        }
        fun preferDouble(vararg keys: String, fallback: Double? = null): Double? {
            dtoMap?.let {
                for (k in keys) {
                    anyToDoubleOrNull(it[k])?.let { d -> return d }
                }
            }
            return fallback
        }
        fun preferInt(vararg keys: String, fallback: Int? = null): Int? {
            dtoMap?.let {
                for (k in keys) {
                    anyToIntOrNull(it[k])?.let { i -> return i }
                }
            }
            return fallback
        }
        fun preferBoolean(vararg keys: String, fallback: Boolean = false): Boolean {
            dtoMap?.let {
                for (k in keys) {
                    anyToBooleanOrNull(it[k])?.let { b -> return b }
                }
            }
            return fallback
        }

        // photos/videos handling
        val photos = when {
            dtoMap != null && dtoMap["photos"] is List<*> -> (dtoMap["photos"] as List<*>).mapNotNull { anyToStringOrNull(it) }
            else -> parseStringList(e.profilePhotosJson)
        }
        val videos = when {
            dtoMap != null && dtoMap["videos"] is List<*> -> (dtoMap["videos"] as List<*>).mapNotNull { anyToStringOrNull(it) }
            else -> parseStringList(e.profileVideosJson)
        }

        // flexible interests parsing
        val interestsParsed = parseInterestsFlexible(dtoMap, e.interestsJson)

        val rawMainPhotoCandidate = preferString("photo", "photo_url", "photoUrl", fallback = e.photo)
            ?: photos.firstOrNull()

        // If rawMainPhotoCandidate is server-relative or plain filename, we keep raw value in rawMainPhoto for debugging
        val mainPhotoForUi = normalizeImageSource(rawMainPhotoCandidate)

        // Logging for debugging
        Log.d(TAG, "rawMainPhotoCandidate='$rawMainPhotoCandidate' normalized='$mainPhotoForUi' photos=$photos videos=$videos interestsParsed=$interestsParsed")

        return ProfileDisplayState(
            mainPhoto = mainPhotoForUi,
            rawMainPhoto = rawMainPhotoCandidate,
            fullName = preferString("fullName", "full_name", "name", fallback = e.fullName),
            profileDescription = preferString("profileDescription", "profile_description", fallback = e.profileDescription),
            bio = preferString("bio", fallback = e.bio),
            phone = preferString("phone", "mobile", "phone_number", fallback = e.phone),
            lat = preferDouble("lat", "latitude", fallback = e.latitude?.toDouble()),
            lng = preferDouble("lng", "longitude", fallback = e.longitude?.toDouble()),
            dob = preferString("dob", "dateOfBirth", "date_of_birth", fallback = e.dob),
            gender = preferString("gender", fallback = e.gender),
            priceMin = preferDouble("priceMin", "price_min", fallback = e.priceMin?.toDouble()),
            priceMax = preferDouble("priceMax", "price_max", fallback = e.priceMax?.toDouble()),
            isEventManager = preferBoolean("isEventManager", "is_event_manager", fallback = e.isEventManager == true),
            interests = interestsParsed,
            rawInterestsJson = e.interestsJson ?: dtoMap?.let { firstMatchingJsonFragment(it, listOf("interests", "interests_json")) },
            averageRating = preferDouble("averageRating", "average_rating", fallback = e.averageRating?.toDouble()),
            totalRatings = preferInt("totalRatings", "total_ratings", fallback = e.totalRatings),
            facebookId = preferString("facebookId", "facebook_id", fallback = e.facebookId),
            twitterId = preferString("twitterId", "twitter_id", fallback = e.twitterId),
            instagramId = preferString("instagramId", "instagram_id", fallback = e.instagramId),
            youtubeId = preferString("youtubeId", "youtube_id", fallback = e.youtubeId),
            pricingType = preferString("pricingType", "pricing_type", fallback = e.pricingType),
            standardPrice = preferDouble("standardPrice", "standard_price", fallback = e.standardPrice?.toDouble()),
            travelRadius = preferDouble("travelRadius", "travel_radius", fallback = e.travelRadius?.toDouble()),
            photos = photos,
            videos = videos,
            loading = false
        )
    }

    // If value is server-relative ('/media/x.jpg') or plain 'abc.jpg', you may want to prepend baseUrl.
    // For now we only normalize obvious schemes and leave server-relative as-is (debug UI will show rawMainPhoto).
    private fun normalizeImageSource(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val trimmed = raw.trim()
        return when {
            trimmed.startsWith("data:", ignoreCase = true) -> trimmed
            trimmed.startsWith("http://", ignoreCase = true) -> trimmed
            trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            trimmed.startsWith("content://", ignoreCase = true) -> trimmed
            trimmed.startsWith("file://", ignoreCase = true) -> trimmed
            trimmed.startsWith("/") -> {
                // absolute file path on device (likely ok)
                trimmed
            }
            // plain filename or server-relative - leave as-is so debug UI shows it.
            else -> trimmed
        }
    }

    // Try to extract a small JSON fragment string for interests from dtoMap (best-effort)
    private fun firstMatchingJsonFragment(map: Map<String, Any?>, keys: List<String>): String? {
        for (k in keys) {
            if (map.containsKey(k)) {
                val v = map[k]
                if (v != null) {
                    return v.toString()
                }
            }
        }
        return null
    }

    // Flexible interest parsing:
    // - if dtoMap has interests as List<*>, handle list of maps or list of strings/numbers
    // - else parse entity.interestsJson (which may be JSON array)
    private fun parseInterestsFlexible(dtoMap: Map<String, Any?>?, interestsJsonFallback: String?): List<Interest> {
        // 1) try map first
        dtoMap?.let { map ->
            val candidate = map["interests"] ?: map["interest"] ?: map["interests_json"]
            if (candidate is List<*>) {
                val list = candidate.mapNotNull { item ->
                    when (item) {
                        is String -> Interest(id = null, remoteId = item, name = item)
                        is Number -> Interest(id = item.toLong(), remoteId = item.toString(), name = item.toString())
                        is Map<*, *> -> {
                            val idRaw = anyToStringOrNull(item["id"]) ?: anyToStringOrNull(item["remote_id"])
                            val idLong = anyToLongOrNull(idRaw)
                            val remoteId = anyToStringOrNull(item["remote_id"]) ?: anyToStringOrNull(item["id"])
                            // try multiple keys for a name
                            val name = anyToStringOrNull(item["name"]) ?: anyToStringOrNull(item["title"])
                            val label = anyToStringOrNull(item["label"]) ?: anyToStringOrNull(item["interest"])
                            val finalName = name ?: label
                            Interest(id = idLong, remoteId = remoteId, name = finalName)
                        }
                        else -> null
                    }
                }
                if (list.isNotEmpty()) return list
            }
            // candidate could be a stringified JSON array (rare)
            if (candidate is String) {
                // attempt parse below as fallback
            }
        }

        // 2) fallback to parsing string JSON stored in entity
        if (!interestsJsonFallback.isNullOrBlank()) {
            try {
                val type = Types.newParameterizedType(List::class.java, Any::class.java)
                val adapter = moshi.adapter<List<Any>>(type)
                val parsed = adapter.fromJson(interestsJsonFallback) ?: emptyList()
                val list = parsed.mapNotNull { item ->
                    when (item) {
                        is String -> Interest(id = null, remoteId = item, name = item)
                        is Number -> Interest(id = item.toLong(), remoteId = item.toString(), name = item.toString())
                        is Map<*, *> -> {
                            val idRaw = anyToStringOrNull(item["id"]) ?: anyToStringOrNull(item["remote_id"])
                            val idLong = anyToLongOrNull(idRaw)
                            val remoteId = anyToStringOrNull(item["remote_id"]) ?: anyToStringOrNull(item["id"])
                            val name = anyToStringOrNull(item["name"]) ?: anyToStringOrNull(item["title"]) ?: anyToStringOrNull(item["label"])
                            Interest(id = idLong, remoteId = remoteId, name = name)
                        }
                        else -> null
                    }
                }
                if (list.isNotEmpty()) return list
            } catch (t: Throwable) {
                Log.d(TAG, "parseInterestsFlexible: fallback parse failed: ${t.message}")
            }
        }

        // final fallback empty
        return emptyList()
    }

    // --- JSON parse helpers ---
    private fun parseJsonToMap(json: String?): Map<String, Any?>? {
        if (json.isNullOrBlank()) return null
        return try {
            val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
            val adapter = moshi.adapter<Map<String, Any?>>(type)
            adapter.fromJson(json)
        } catch (t: Throwable) {
            Log.d(TAG, "parseJsonToMap failed: ${t.message}")
            null
        }
    }

    private fun anyToStringOrNull(v: Any?): String? {
        return when (v) {
            null -> null
            is String -> v
            is Number -> v.toString()
            is Boolean -> v.toString()
            else -> v.toString()
        }
    }

    private fun anyToDoubleOrNull(v: Any?): Double? {
        return when (v) {
            null -> null
            is Number -> v.toDouble()
            is String -> v.toDoubleOrNull()
            else -> null
        }
    }

    private fun anyToIntOrNull(v: Any?): Int? {
        return when (v) {
            null -> null
            is Number -> v.toInt()
            is String -> v.toIntOrNull()
            else -> null
        }
    }

    private fun anyToLongOrNull(v: Any?): Long? {
        return when (v) {
            null -> null
            is Number -> v.toLong()
            is String -> v.toLongOrNull()
            else -> null
        }
    }

    private fun anyToBooleanOrNull(v: Any?): Boolean? {
        return when (v) {
            null -> null
            is Boolean -> v
            is String -> v.equals("true", ignoreCase = true) || v == "1"
            is Number -> v.toInt() != 0
            else -> null
        }
    }

    private fun parseStringList(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val type = Types.newParameterizedType(List::class.java, String::class.java)
            val adapter = moshi.adapter<List<String>>(type)
            adapter.fromJson(json) ?: emptyList()
        } catch (t: Throwable) {
            Log.d(TAG, "parseStringList failed: ${t.message}")
            emptyList()
        }
    }
}
