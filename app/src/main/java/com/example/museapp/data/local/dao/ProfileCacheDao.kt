package com.example.museapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.museapp.data.local.entity.ProfileCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileCacheDao {

    @Query("SELECT * FROM profile_cache WHERE user_id = :userId LIMIT 1")
    suspend fun getProfileCacheOnce(userId: String): ProfileCacheEntity?

    @Query("SELECT * FROM profile_cache WHERE user_id = :userId LIMIT 1")
    fun observeProfileCache(userId: String): Flow<ProfileCacheEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ProfileCacheEntity)

    @Query("DELETE FROM profile_cache WHERE user_id = :userId")
    suspend fun clear(userId: String)
}
