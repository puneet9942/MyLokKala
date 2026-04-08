package com.example.museapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Profile cache entity stored in Room.
 *
 * Note: column names use snake_case to match DAO SQL (user_id, json, last_updated_millis).
 */
@Entity(tableName = "profile_cache")
data class ProfileCacheEntity(
    @PrimaryKey
    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "json")
    val json: String,

    // epoch millis used to pick the most-recent row
    @ColumnInfo(name = "last_updated_millis")
    val lastUpdatedMillis: Long = 0L
)
