package com.example.museapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.museapp.data.local.dao.InterestsDao
import com.example.museapp.data.local.dao.ProfileCacheDao
import com.example.museapp.data.local.dao.UserDao
import com.example.museapp.data.local.entity.InterestEntity
import com.example.museapp.data.local.entity.ProfileCacheEntity
import com.example.museapp.data.local.entity.UserEntity


@Database(entities = [InterestEntity::class, UserEntity::class, ProfileCacheEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun interestsDao(): InterestsDao
    abstract fun userDao(): UserDao

    abstract fun profileCacheDao(): ProfileCacheDao
}
