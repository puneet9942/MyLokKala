package com.example.museapp.data.repository

import android.util.Log
import com.example.museapp.data.local.dao.InterestsDao
import com.example.museapp.data.local.entity.InterestEntity
import com.example.museapp.data.remote.ApiService
import com.example.museapp.data.remote.dto.ApiResponse
import com.example.museapp.data.remote.dto.InterestDto
import com.example.museapp.data.remote.dto.InterestsDataDto
import com.example.museapp.domain.repository.InterestsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * InterestsRepositoryImpl
 *
 * - Matches ApiService.getInterests(page,limit): ApiResponse<InterestsDataDto>
 * - Preserves existing method names used by ViewModel:
 *     - getAllInterests()
 *     - fetchAndSaveInterests(page, limit)
 *
 * Minimal, defensive implementation:
 * - Runs network + DB writes on IO
 * - Handles multiple API payload shapes
 * - Logs mapping/inserts for debugging
 */
class InterestsRepositoryImpl @Inject constructor(
    private val api: ApiService,
    private val dao: InterestsDao
) : InterestsRepository {

    private val TAG = "InterestsRepository"

    // Flow used by HomeViewModel to observe DB changes
    override fun getAllInterests(): Flow<List<InterestEntity>> = dao.getAllInterestsFlow()

    /**
     * Fetches a page of interests from the remote API and saves them to Room.
     * Keeps the method signature identical to existing usage.
     */
    override suspend fun fetchAndSaveInterests(page: Int, limit: Int) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "fetchAndSaveInterests: calling API page=$page limit=$limit")

                // ApiService returns ApiResponse<InterestsDataDto>
                val resp: ApiResponse<InterestsDataDto> = api.getInterests(page = page, limit = limit)

                if (resp.isSuccessful()) {
                    val body: InterestsDataDto? = resp.data

                    // Normalize items from many possible shapes
                    val items: List<InterestDto> = when {
                        body == null -> emptyList()
                        body.items != null -> body.items
                        body.interests != null -> body.interests
                        body.dataList != null -> body.dataList
                        body.nestedData?.items != null -> body.nestedData.items
                        body.nestedData?.interests != null -> body.nestedData.interests
                        else -> emptyList()
                    } ?: emptyList()

                    Log.d(TAG, "API returned ${items.size} interest items")

                    // Resolve pagination safely (nullable fields)
                    val resolvedPage = body?.pagination?.page ?: body?.page ?: page
                    val resolvedLimit = body?.pagination?.limit ?: body?.limit ?: limit
                    val resolvedTotal = body?.pagination?.total ?: body?.total ?: items.size
                    val resolvedTotalPages = body?.pagination?.totalPages ?: body?.totalPages
                    ?: if (resolvedLimit > 0) ((resolvedTotal + resolvedLimit - 1) / resolvedLimit) else 1

                    Log.d(TAG, "Pagination resolved page=$resolvedPage limit=$resolvedLimit total=$resolvedTotal totalPages=$resolvedTotalPages")

                    // Map DTO -> Entity, drop empty names
                    val entities = items.mapNotNull { dto ->
                        val name = (dto.name ?:  "").trim()
                        if (name.isEmpty()) null
                        else {
                            val remoteId = dto.remoteId ?: dto.id?.toString()
                            InterestEntity(remoteId = remoteId, name = name)
                        }
                    }
                        .distinctBy { it.name.lowercase() } // dedupe case-insensitive
                        .sortedBy { it.name.lowercase() }

                    Log.d(TAG, "Mapped ${entities.size} interest entities to insert")

                    if (entities.isNotEmpty()) {
                        try {
                            dao.insertAll(entities)
                            val afterCount = try { dao.count() } catch (e: Throwable) {
                                Log.w(TAG, "count() failed after insert: ${e.message}")
                                -1
                            }
                            Log.d(TAG, "Inserted interests; db count now = $afterCount")
                        } catch (dbEx: Throwable) {
                            Log.e(TAG, "Error inserting interests into DB: ${dbEx.message}", dbEx)
                            throw dbEx
                        }
                    } else {
                        Log.w(TAG, "No entities to insert (mapped list empty)")
                    }
                } else {
                    Log.w(TAG, "API reported failure: message=${resp.message} httpCode=${resp.httpCode()}")
                }
            } catch (t: Throwable) {
                Log.e(TAG, "fetchAndSaveInterests failed: ${t.message}", t)
                throw t
            }
        }
    }

    /**
     * One-shot DB read helper (non-overriding; safe if interface doesn't declare it).
     */
    suspend fun getAllInterestsOnce(): List<InterestEntity> =
        withContext(Dispatchers.IO) { dao.getAllInterestsOnce() }

    /**
     * Debug helper to clear interests table (non-overriding).
     */
    suspend fun clearAllInterestsForDebug() =
        withContext(Dispatchers.IO) { dao.clearAll() }
}
