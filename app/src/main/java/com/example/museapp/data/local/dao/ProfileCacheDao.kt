package com.example.museapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.museapp.data.local.entity.ProfileCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileCacheDao {

    // legacy single-key read
    @Query("SELECT * FROM profile_cache WHERE user_id = :userId LIMIT 1")
    suspend fun getProfileCacheOnce(userId: String): ProfileCacheEntity?

    // observe a specific key as Flow (keeps your existing API)
    @Query("SELECT * FROM profile_cache WHERE user_id = :userId LIMIT 1")
    fun observeProfileCache(userId: String): Flow<ProfileCacheEntity?>

    // NEW: observe the most-recent cache row regardless of key
    @Query("SELECT * FROM profile_cache ORDER BY last_updated_millis DESC LIMIT 1")
    fun observeLatestProfileCache(): Flow<ProfileCacheEntity?>

    // synchronous latest read (used by repository)
    @Query("SELECT * FROM profile_cache ORDER BY last_updated_millis DESC LIMIT 1")
    suspend fun getLatestProfileCache(): ProfileCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ProfileCacheEntity)

    @Query("DELETE FROM profile_cache WHERE user_id = :userId")
    suspend fun clear(userId: String)

    // prune duplicates keeping only one canonical row
    // existing simple case
    @Query("DELETE FROM profile_cache WHERE user_id != :keepUserId")
    suspend fun deleteAllExcept(keepUserId: String)

    // NEW: delete rows that are older than the canonical epoch or not the canonical user.
    // This helps prevent brief races where another row exists with a higher epoch.
    @Query("DELETE FROM profile_cache WHERE user_id != :keepUserId OR last_updated_millis < :minEpoch")
    suspend fun deleteOlderOrNotCanonical(keepUserId: String, minEpoch: Long)

    @Transaction
    suspend fun upsertAndPrune(entity: ProfileCacheEntity) {
        // upsert the canonical row first
        upsert(entity)

        // then prune rows that are either not canonical user or older epoch than canonical
        // this guarantees the canonical row (entity.userId + entity.lastUpdatedMillis) remains top
        deleteOlderOrNotCanonical(entity.userId, entity.lastUpdatedMillis)
    }

    // returns all rows (newest first)
    @Query("SELECT * FROM profile_cache ORDER BY last_updated_millis DESC")
    suspend fun getAllCacheRows(): List<ProfileCacheEntity>

    // aggregated counts per user_id + latest epoch per user
    data class CacheSummary(val user_id: String?, val cnt: Int, val latest_epoch: Long?)
    @Query("SELECT user_id AS user_id, COUNT(*) AS cnt, MAX(last_updated_millis) AS latest_epoch FROM profile_cache GROUP BY user_id")
    suspend fun getCacheSummary(): List<CacheSummary>

    // handy single-row debug by id (if you want to inspect a specific user_id)
    @Query("SELECT * FROM profile_cache WHERE user_id = :userId ORDER BY last_updated_millis DESC")
    suspend fun getRowsForUser(userId: String): List<ProfileCacheEntity>

}
