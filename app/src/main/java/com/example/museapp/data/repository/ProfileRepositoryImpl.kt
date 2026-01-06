package com.example.museapp.data.repository

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.provider.OpenableColumns
import android.util.Log
import com.example.museapp.data.local.dao.UserDao
import com.example.museapp.data.local.mapper.toEntity
import com.example.museapp.data.remote.ApiService
import com.example.museapp.data.remote.dto.ProfileRequestDto
import com.example.museapp.data.remote.dto.ProfileDataDto
import com.example.museapp.data.remote.dto.UserDto
// NOTE: removed import of com.example.museapp.data.remote.dto.toDomain to avoid duplicate extension import
import com.example.museapp.data.remote.mapper.toDomain
import com.example.museapp.data.util.NetworkResult
import com.example.museapp.data.util.safeApiCall
import com.example.museapp.domain.model.User
import com.example.museapp.domain.repository.ProfileRepository
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private const val TAG = "ProfileRepoImpl"

class ProfileRepositoryImpl @Inject constructor(
    private val api: ApiService,
    private val appContext: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val moshi: Moshi = Moshi.Builder().build(),
    private val userDao: UserDao                 // <-- injected dependency (AppDatabase.userDao())
) : ProfileRepository {

    override suspend fun updateProfile(
        payloadDto: ProfileRequestDto,
        profilePhoto: Uri?,
        photos: List<Uri>,
        videos: List<Uri>
    ): NetworkResult<User> = safeApiCall(ioDispatcher) {
        // --- 1) Build payload JSON part robustly (normalize dob, drop blank strings) ---
        val adapter = moshi.adapter(ProfileRequestDto::class.java)
        val jsonRaw = adapter.toJson(payloadDto)

        @Suppress("UNCHECKED_CAST")
        val payloadMap: MutableMap<String, Any?> = try {
            (moshi.adapter(Map::class.java).fromJson(jsonRaw) as? Map<String, Any?>)?.toMutableMap()
                ?: mutableMapOf()
        } catch (e: Exception) {
            Log.w(TAG, "Failed parsing payload DTO to Map: ${e.message}")
            mutableMapOf()
        }

        fun normalizeDobToIso(input: Any?): String? {
            if (input == null) return null
            val s = input.toString().trim()
            if (s.isEmpty()) return null

            try {
                val ld = LocalDate.parse(s, DateTimeFormatter.ISO_DATE)
                return ld.format(DateTimeFormatter.ISO_DATE)
            } catch (_: DateTimeParseException) { }

            val patterns = listOf(
                "dd-MM-yyyy",
                "d-M-yyyy",
                "dd/MM/yyyy",
                "d/M/yyyy",
                "MM/dd/yyyy",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
            )
            for (p in patterns) {
                try {
                    val fmt = DateTimeFormatter.ofPattern(p)
                    val ld = LocalDate.parse(s, fmt)
                    return ld.format(DateTimeFormatter.ISO_DATE)
                } catch (_: Exception) { }
            }

            try {
                val n = s.toLong()
                val instant = if (n > 1_000_000_000_000L) Instant.ofEpochMilli(n) else Instant.ofEpochSecond(n)
                val ld = instant.atZone(ZoneOffset.UTC).toLocalDate()
                return ld.format(DateTimeFormatter.ISO_DATE)
            } catch (_: Exception) { }

            return null
        }

        val normalizedDob = normalizeDobToIso(payloadMap["dob"])
        if (normalizedDob != null) {
            payloadMap["dob"] = normalizedDob
        } else {
            payloadMap.remove("dob")
        }

        val keysToRemove = payloadMap.filterValues { v ->
            v is String && v.trim().isEmpty()
        }.keys
        for (k in keysToRemove) payloadMap.remove(k)

        val cleanedJson = moshi.adapter(Map::class.java).toJson(payloadMap)
        // <-- FIX: pass MediaType using toMediaTypeOrNull()
        val payloadRb: RequestBody = cleanedJson.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        Log.d(TAG, "updateProfile: cleaned payload JSON = $cleanedJson")

        // --- 2) Helpers: uri -> temp file, filename extraction, mime guessing ---
        suspend fun uriToTempFile(uri: Uri, preferredName: String?): File? = withContext(ioDispatcher) {
            try {
                val input = appContext.contentResolver.openInputStream(uri) ?: return@withContext null
                val suffix = preferredName?.substringAfterLast('.', "")
                val base = "upload_${SystemClock.uptimeMillis()}"
                val tmp = if (!suffix.isNullOrBlank()) {
                    File.createTempFile(base, ".${suffix}", appContext.cacheDir)
                } else {
                    File.createTempFile(base, null, appContext.cacheDir)
                }
                FileOutputStream(tmp).use { out -> input.copyTo(out) }
                tmp
            } catch (t: Throwable) {
                Log.w(TAG, "uriToTempFile failed for $uri", t)
                null
            }
        }

        fun filenameFromUri(uri: Uri): String? {
            var name: String? = null
            val cursor = appContext.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) name = it.getString(idx)
                }
            }
            return name
        }

        fun guessMime(name: String?): String {
            val ext = (name ?: "").substringAfterLast('.', "").lowercase()
            return when (ext) {
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "webp" -> "image/webp"
                "mp4" -> "video/mp4"
                else -> "application/octet-stream"
            }
        }

        // --- 3) Build profile photo part (single "photo" part) ---
        val photoPart: MultipartBody.Part? = profilePhoto?.let { uri ->
            val filename = filenameFromUri(uri) ?: "profile_photo.jpg"
            val tmp = uriToTempFile(uri, filename)
            tmp?.let { f ->
                val ext = f.extension.lowercase()
                val finalFile = if (ext == "jpg" || ext == "jpeg" || ext == "png") {
                    f
                } else {
                    val newName = f.nameWithoutExtension + ".jpg"
                    val newFile = File(f.parentFile, newName)
                    f.copyTo(newFile, overwrite = true)
                    newFile
                }

                val resolverMime = try { appContext.contentResolver.getType(uri) } catch (_: Exception) { null }
                val mime = resolverMime?.takeIf { it.startsWith("image/") } ?: guessMime(finalFile.name)

                val rb = finalFile.asRequestBody(mime.toMediaTypeOrNull())
                MultipartBody.Part.createFormData("photo", finalFile.name, rb)
            }
        }

        // --- 4) Build photos and videos as binary multipart parts (fix: DO NOT send JSON of URIs) ---
        val photosParts: List<MultipartBody.Part>? = if (photos.isNullOrEmpty()) {
            null
        } else {
            photos.mapNotNull { uri ->
                val filename = filenameFromUri(uri) ?: uri.lastPathSegment ?: "photo_${SystemClock.uptimeMillis()}.jpg"
                val tmp = uriToTempFile(uri, filename)
                tmp?.let { f ->
                    val mime = try { appContext.contentResolver.getType(uri) } catch (_: Exception) { null } ?: guessMime(f.name)
                    val rb = f.asRequestBody(mime.toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("photos", f.name, rb)
                }
            }
        }

        val videosParts: List<MultipartBody.Part>? = if (videos.isNullOrEmpty()) {
            null
        } else {
            videos.mapNotNull { uri ->
                val filename = filenameFromUri(uri) ?: uri.lastPathSegment ?: "video_${SystemClock.uptimeMillis()}.mp4"
                val tmp = uriToTempFile(uri, filename)
                tmp?.let { f ->
                    val mime = try { appContext.contentResolver.getType(uri) } catch (_: Exception) { null } ?: guessMime(f.name)
                    val rb = f.asRequestBody(mime.toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("videos", f.name, rb)
                }
            }
        }

        // --- 5) Call server update endpoint with binary parts ---
        Log.d(TAG, "about to call api.updateProfile; photoPart=${photoPart != null} photosCount=${photosParts?.size ?: 0} videosCount=${videosParts?.size ?: 0}")
        val updateResp = withContext(ioDispatcher) {
            api.updateProfile(payloadRb, photoPart, photosParts, videosParts)
        }
        Log.d(TAG, "api.updateProfile returned: ${updateResp?.toString() ?: "null"}")

        // --- 6) Map server response to domain User if present and persist to Room ---
        val dataObj: Any? = try {
            updateResp::class.java.getMethod("getData").invoke(updateResp)
        } catch (_: Exception) {
            null
        }

        if (dataObj != null) {
            val rawServerJson: String? = try { moshi.adapter(Any::class.java).toJson(dataObj) } catch (_: Exception) { null }

            val mappedUser: User? = try {
                when (dataObj) {
                    is ProfileDataDto -> dataObj.toDomain()
                    is UserDto -> dataObj.toDomain()
                    else -> {
                        val rawJson = moshi.adapter(Any::class.java).toJson(dataObj)
                        try {
                            val ud = moshi.adapter(UserDto::class.java).fromJson(rawJson)
                            ud?.toDomain()
                        } catch (_: Exception) {
                            null
                        }
                    }
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Mapping updateResp.data -> User failed: ${t.message}")
                null
            }

            if (mappedUser != null) {
                // persist mapped user safely (no side-effects)
                try {
                    withContext(ioDispatcher) {
                        val entity = mappedUser.toEntity(moshi, rawServerJson)
                        userDao.insertUser(entity)
                    }
                } catch (t: Throwable) {
                    Log.w(TAG, "Failed to persist mapped user: ${t.message}")
                }

                return@safeApiCall mappedUser
            }
        }

        // --- 7) Fallback: construct User from cleaned payload and persist ---
        val payloadMapFinal: Map<String, Any> = try {
            @Suppress("UNCHECKED_CAST")
            moshi.adapter(Map::class.java).fromJson(cleanedJson) as? Map<String, Any> ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }

        fun anyToString(key: String, altKeys: List<String> = emptyList()): String? {
            val keys = listOf(key) + altKeys
            for (k in keys) {
                val candidates = listOf(k, k.lowercase(), k.replaceFirstChar { it.lowercase() })
                for (ck in candidates) {
                    val v = payloadMapFinal[ck] ?: payloadMapFinal[ck.lowercase()]
                    if (v is String && v.isNotBlank()) return v
                    if (v is Number) return v.toString()
                    if (v != null) return v.toString()
                }
            }
            return null
        }

        fun anyToDouble(key: String, altKeys: List<String> = emptyList()): Double? {
            return anyToString(key, altKeys)?.toDoubleOrNull()
        }

        fun anyToInt(key: String, altKeys: List<String> = emptyList()): Int? {
            return anyToString(key, altKeys)?.toIntOrNull()
        }

        val idValue = anyToString("id", listOf("_id", "userId", "user_id")) ?: ""
        val fullNameValue = anyToString("fullName", listOf("fullname", "name")) ?: ""
        val profileDescriptionValue = anyToString("profileDescription", listOf("description", "profile_description")) ?: ""
        val bioValue = anyToString("biography", listOf("bio")) ?: ""
        val phoneValue = anyToString("phone", listOf("mobile", "phoneNumber", "phone_number")) ?: ""
        val createdAtValue = anyToString("createdAt", listOf("created_at")) ?: ""
        val updatedAtValue = anyToString("updatedAt", listOf("updated_at")) ?: ""
        val latValue = anyToDouble("lat", listOf("latitude")) ?: 0.0
        val lngValue = anyToDouble("lng", listOf("lon", "longitude")) ?: 0.0
        val priceMinValue = anyToInt("priceMin", listOf("minPrice", "price_min")) ?: 0
        val priceMaxValue = anyToInt("priceMax", listOf("maxPrice", "price_max")) ?: 0

        val isEventManagerValue: Boolean = run {
            val candidates = listOf("isEventManager", "is_event_manager", "is_eventmanager")
            for (k in candidates) {
                val v = payloadMapFinal[k] ?: payloadMapFinal[k.lowercase()]
                when (v) {
                    is Boolean -> return@run v
                    is Number -> return@run v.toInt() != 0
                    is String -> if (v.equals("true", ignoreCase = true) || v == "1") return@run true
                }
            }
            false
        }

        val constructedUser = try {
            User(
                id = idValue,
                fullName = fullNameValue,
                profileDescription = profileDescriptionValue,
                lat = latValue,
                lng = lngValue,
                bio = bioValue,
                isEventManager = isEventManagerValue,
                phone = phoneValue,
                priceMin = priceMinValue,
                priceMax = priceMaxValue,
                photo = null,
                createdAt = createdAtValue,
                updatedAt = updatedAtValue
            )
        } catch (e: Exception) {
            User(id = System.currentTimeMillis().toString(), fullName = "")
        }

        // persist constructed fallback user (best-effort, safe)
        try {
            withContext(ioDispatcher) {
                val entity = constructedUser.toEntity(moshi, cleanedJson)
                userDao.insertUser(entity)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to persist constructed user: ${t.message}")
        }

        return@safeApiCall constructedUser
    }
}