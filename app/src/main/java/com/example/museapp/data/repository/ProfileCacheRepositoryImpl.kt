package com.example.museapp.data.repository

import android.util.Log
import com.example.museapp.util.AppConstants
import com.example.museapp.data.local.dao.ProfileCacheDao
import com.example.museapp.data.local.entity.ProfileCacheEntity
import com.example.museapp.data.remote.ApiService
import com.example.museapp.data.remote.dto.ApiResponse
import com.example.museapp.data.remote.dto.CacheUserDto
import com.example.museapp.data.remote.dto.ErrorDto
import com.example.museapp.data.remote.dto.ProfileCacheDto
import com.example.museapp.domain.repository.ProfileCacheRepository
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject
import kotlin.reflect.full.memberProperties

class ProfileCacheRepositoryImpl @Inject constructor(
    private val dao: ProfileCacheDao,
    private val apiService: ApiService,
    private val moshi: Moshi
) : ProfileCacheRepository {

    companion object {
        private val DEFAULT_USER_ID = AppConstants.PROFILE_CACHE_USER_ID
    }

    private val adapter = moshi.adapter(ProfileCacheDto::class.java)

    // Read the most-recent cached profile (if any)
    override suspend fun getProfileCache(): ProfileCacheDto? {
        return withContext(Dispatchers.IO) {
            try {
                val entity = dao.getLatestProfileCache()
                entity?.let { adapter.fromJson(it.json) }
            } catch (t: Throwable) {
                null
            }
        }
    }

    // Fetch from network and persist to canonical single-row cache.
    // Also prune other rows to avoid duplicates.
    override suspend fun fetchAndCacheProfile(): ProfileCacheDto? {
        return withContext(Dispatchers.IO) {
            try {
                val response: ApiResponse<CacheUserDto> = apiService.getProfile()

                val profileDto = ProfileCacheDto(
                    status = try { response.status } catch (_: Throwable) { null },
                    statusCode = try { response.status_code } catch (_: Throwable) { null },
                    message = try { response.message } catch (_: Throwable) { null },
                    timestamp = try { response.timestamp } catch (_: Throwable) { null },
                    requestId = try { response.request_id } catch (_: Throwable) { null },
                    data = try { response.data } catch (_: Throwable) { null },
                    error = try { response.error } catch (_: Throwable) { null } as ErrorDto?
                )

                val json = adapter.toJson(profileDto)
                // compute serverIso: prefer top-level response.timestamp, else try to read updatedAt from data reflectively
                val serverIso: String? = try {
                    response.timestamp ?: run {
                        response.data?.let { d ->
                            val prop = d::class.memberProperties.firstOrNull { p ->
                                p.name.equals("updatedAt", ignoreCase = true) || p.name.equals("updated_at", ignoreCase = true)
                            }
                            prop?.call(d) as? String
                        }
                    }
                } catch (_: Throwable) {
                    null
                }
                Log.d("PROFILE_DEBUG", "API raw response: $response")

                val serverEpoch: Long = try {
                    serverIso?.let { Instant.parse(it).toEpochMilli() } ?: System.currentTimeMillis()
                } catch (_: Throwable) {
                    System.currentTimeMillis()
                }
                val entity = ProfileCacheEntity(
                    userId = DEFAULT_USER_ID,
                    json = json,
                    lastUpdatedMillis = serverEpoch
                )

// Use atomic upsert + prune
                try {
                    dao.upsertAndPrune(entity)
                } catch (_: Throwable) {
                    // non-fatal
                }


//                // prune other rows so only canonical remains
//                try {
//                    dao.deleteAllExcept(DEFAULT_USER_ID)
//                } catch (_: Throwable) {
//                    // non-fatal pruning failure
//                }

                profileDto
            } catch (t: Throwable) {
                null
            }
        }
    }

    private fun profileJsonToDto(json: String): ProfileCacheDto? {
        return try {
            adapter.fromJson(json)
        } catch (t: Throwable) {
            null
        }
    }
    // debug helper - returns a readable dump of the profile_cache table
    suspend fun dumpProfileCacheTable(): String = withContext(Dispatchers.IO) {
        try {
            val rows = dao.getAllCacheRows()
            val sb = StringBuilder()
            sb.appendLine("profile_cache rows: count=${rows.size}")
            rows.forEachIndexed { i, r ->
                sb.appendLine("---- row #${i + 1} ----")
                sb.appendLine("userId: ${r.userId}")
                sb.appendLine("lastUpdatedMillis: ${r.lastUpdatedMillis}")
                sb.appendLine("json: ${r.json}")
            }

            // summary per user
            sb.appendLine()
            sb.appendLine("per-user summary:")
            dao.getCacheSummary().forEach { s ->
                sb.appendLine("user=${s.user_id} count=${s.cnt} latest_epoch=${s.latest_epoch}")
            }

            sb.toString()
        } catch (t: Throwable) {
            "dumpProfileCacheTable failed: ${t.message}"
        }
    }

}
