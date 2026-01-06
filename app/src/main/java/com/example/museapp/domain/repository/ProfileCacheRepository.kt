package com.example.museapp.data.repository

import com.example.museapp.data.remote.dto.ProfileCacheDto

/**
 * Repository contract used by ProfileCacheDisplayViewModel.
 *
 * Implementation responsibilities:
 *  - getProfileCache(): read cached ProfileCacheDto from Room (or other local store)
 *  - fetchAndCacheProfile(): call API (/api/user/profile), convert/return ProfileCacheDto and persist it to Room
 *
 * Naming is intentionally explicit so the ViewModel needs only this single repository.
 */
interface ProfileCacheRepository {
    /**
     * Return last persisted ProfileCacheDto or null when none present.
     * Should be a suspend function.
     */
    suspend fun getProfileCache(): ProfileCacheDto?

    /**
     * Fetch profile from network, persist the full ProfileCacheDto into local storage, and return it.
     * This centralizes network + caching logic in the repository so ViewModel can stay simple.
     */
    suspend fun fetchAndCacheProfile(): ProfileCacheDto?
}
