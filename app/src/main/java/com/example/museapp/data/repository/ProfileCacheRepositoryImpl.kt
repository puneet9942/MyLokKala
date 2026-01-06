package com.example.museapp.data.repository

import com.example.museapp.data.local.dao.ProfileCacheDao
import com.example.museapp.data.local.entity.ProfileCacheEntity
import com.example.museapp.data.remote.ApiService
import com.example.museapp.data.remote.dto.ApiResponse
import com.example.museapp.data.remote.dto.CacheUserDto
import com.example.museapp.data.remote.dto.ProfileCacheDto
import com.example.museapp.data.remote.dto.UserDto
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Implementation of ProfileCacheRepository.
 *
 * Responsibilities:
 *  - getProfileCache(): read cached ProfileCacheDto from Room (or other local store)
 *  - fetchAndCacheProfile(): call API (/api/user/profile), convert/return ProfileCacheDto and persist it to Room
 *
 * Note: This implementation uses a single DEFAULT_USER_ID ("me") for the single-profile cache.
 * If you need multi-user behaviour adapt the repository and ViewModel to accept a userId parameter.
 */
class ProfileCacheRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val dao: ProfileCacheDao,
    private val moshi: Moshi
) : ProfileCacheRepository {

    companion object {
        private const val DEFAULT_USER_ID = "me"
    }

    private val adapter = moshi.adapter(ProfileCacheDto::class.java)

    /**
     * Read the last persisted ProfileCacheDto (if any) from the DB and return it.
     */
    override suspend fun getProfileCache(): ProfileCacheDto? {
        return withContext(Dispatchers.IO) {
            try {
                val entity = dao.getProfileCacheOnce(DEFAULT_USER_ID)
                entity?.let { adapter.fromJson(it.json) }
            } catch (t: Throwable) {
                // swallow and return null to keep ViewModel resilient; adjust if you want exceptions surfaced
                null
            }
        }
    }

    /**
     * Fetch profile from network, persist the full ProfileCacheDto into local storage,
     * and return the typed ProfileCacheDto (or null on failure).
     */
    override suspend fun fetchAndCacheProfile(): ProfileCacheDto? {
        return withContext(Dispatchers.IO) {
            try {
                // call network
                val response: ApiResponse<CacheUserDto> = apiService.getProfile()

                // build typed DTO (supporting typical field names used in ApiResponse)
                val profileDto = ProfileCacheDto(
                    status = try { response.status } catch (_: Throwable) { null },
                    statusCode = try {
                        response.status_code ?: try { response.status_code } catch (_: Throwable) { null }
                    } catch (_: Throwable) { null },
                    message = try { response.message } catch (_: Throwable) { null },
                    timestamp = try { response.timestamp } catch (_: Throwable) { null },
                    requestId = try {
                        response.request_id ?: try { response.request_id } catch (_: Throwable) { null }
                    } catch (_: Throwable) { null },
                    data = try { response.data } catch (_: Throwable) { null },
                    error = try { response.error } catch (_: Throwable) { null }
                )

                // serialize typed profile DTO to JSON and persist
                val json = adapter.toJson(profileDto)
                val entity = ProfileCacheEntity(userId = DEFAULT_USER_ID, json = json, lastUpdatedMillis = System.currentTimeMillis())
                dao.upsert(entity)

                // return the typed DTO
                profileDto
            } catch (t: Throwable) {
                // failed network or persistence; return null to let callers handle gracefully
                null
            }
        }
    }

    /**
     * Keep a private helper in case it is useful elsewhere in the class or future extensions.
     * Not exposed publicly.
     */
    private fun profileJsonToDto(json: String): ProfileCacheDto? {
        return try {
            adapter.fromJson(json)
        } catch (t: Throwable) {
            null
        }
    }
}
