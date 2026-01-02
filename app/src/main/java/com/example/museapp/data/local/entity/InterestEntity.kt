package com.example.museapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "interests",
    indices = [Index(value = ["remote_id"], unique = true)]
)
data class InterestEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "remote_id")
    val remoteId: String? = null,

    @ColumnInfo(name = "name")
    val name: String
)