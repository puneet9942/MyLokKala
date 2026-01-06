package com.example.museapp.domain.repository

import com.example.museapp.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Minimal repository interface for profile caching / fetching.
 * If you already have a UserRepository in your project, remove this file
 * and keep the existing interface instead.
 */
interface UserRepository {
    fun observeCachedProfile(): Flow<User?>
    suspend fun getCachedProfileOnce(): User?
    suspend fun fetchProfileFromNetworkAndCache(): Result<User>
    suspend fun clearCachedProfile()
}
