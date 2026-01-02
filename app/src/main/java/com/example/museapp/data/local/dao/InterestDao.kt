package com.example.museapp.data.local.dao


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.museapp.data.local.entity.InterestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InterestsDao {

    // Get all interests as a Flow so ViewModel / repo can collect
    @Query("SELECT * FROM interests ORDER BY name COLLATE NOCASE ASC")
    fun getAllInterestsFlow(): Flow<List<InterestEntity>>

    // Also provide a suspend read (one-shot)
    @Query("SELECT * FROM interests ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAllInterestsOnce(): List<InterestEntity>

    // Insert list (suspend!) with replace-on-conflict
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(interests: List<InterestEntity>)

    // Helper for quick verification
    @Query("SELECT COUNT(*) FROM interests")
    suspend fun count(): Int

    // Optional: clear all (debug)
    @Query("DELETE FROM interests")
    suspend fun clearAll()
}
