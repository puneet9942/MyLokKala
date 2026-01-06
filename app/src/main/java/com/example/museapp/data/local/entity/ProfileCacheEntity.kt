package com.example.museapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profile_cache")
data class ProfileCacheEntity(
    @PrimaryKey
    @ColumnInfo(name = "user_id")
    val userId: String,

    // Store the entire API response JSON as-is
    @ColumnInfo(name = "json")
    val json: String,

    @ColumnInfo(name = "last_updated_millis")
    val lastUpdatedMillis: Long = System.currentTimeMillis()
)
