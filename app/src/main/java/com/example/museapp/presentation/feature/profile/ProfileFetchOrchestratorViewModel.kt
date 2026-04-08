package com.example.museapp.presentation.feature.profile

import androidx.lifecycle.ViewModel
import com.example.museapp.data.remote.dto.ProfileCacheDto
import com.example.museapp.domain.repository.ProfileCacheRepository
import com.example.museapp.domain.usecase.FetchAndCacheProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ProfileFetchOrchestratorViewModel @Inject constructor(
    private val profileCacheRepository: ProfileCacheRepository, // Room-backed repo
    private val fetchAndCacheProfileUseCase: FetchAndCacheProfileUseCase // network + persist usecase
) : ViewModel() {

    // return cached profile DTO (or null)
    suspend fun getCachedProfileFromRoom(): ProfileCacheDto? {
        return try {
            // adapt: call your repo method to read latest profile cache
            profileCacheRepository.getProfileCache()
        } catch (t: Throwable) {
            null
        }
    }
    // fetch from network, persist into Room, and return result (or null)
    suspend fun fetchAndCacheProfileFromApi(): ProfileCacheDto? = withContext(Dispatchers.IO) {
        try {
            // Primary: call use-case which should fetch + persist
            val result = runCatching { fetchAndCacheProfileUseCase.invoke() }.getOrNull()

            // Defensive: if the use-case returned a DTO, re-read the repository to get the authoritative persisted version
            val persisted = runCatching { profileCacheRepository.getProfileCache() }.getOrNull()

            // Prefer repository value (persisted). If repo is null, fall back to the use-case return.
            persisted ?: result
        } catch (t: Throwable) {
            // Fallback: try repository's direct fetch-and-cache helper if available
            runCatching { profileCacheRepository.fetchAndCacheProfile() }.getOrNull()
        }
    }
}