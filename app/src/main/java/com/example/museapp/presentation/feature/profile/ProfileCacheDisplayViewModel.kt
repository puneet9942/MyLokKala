package com.example.museapp.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.museapp.data.remote.dto.ProfileCacheDto
import com.example.museapp.data.repository.ProfileCacheRepository
import com.example.museapp.domain.model.CacheUser
import com.example.museapp.domain.model.User
import com.example.museapp.presentation.feature.profile.ProfileCacheEvent
import com.example.museapp.presentation.feature.profile.ProfileCacheState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Hilt-enabled ViewModel for ProfileCache display
 *
 * Now depends only on ProfileCacheRepository.
 * Behavior:
 *  - On LoadProfile: tries cached Room data via profileCacheRepository.getProfileCache()
 *    - if cached => emit that immediately
 *    - else => call profileCacheRepository.fetchAndCacheProfile() which should perform the network call and persist result
 *
 * Keep class name prefixed with ProfileCache to avoid collisions.
 */
@HiltViewModel
class ProfileCacheDisplayViewModel @Inject constructor(
    private val profileCacheRepository: ProfileCacheRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileCacheState())
    val uiState: StateFlow<ProfileCacheState> = _uiState.asStateFlow()

    init {
        // eager load; UI can also call onEvent explicitly on tap
        onEvent(ProfileCacheEvent.LoadProfile)
    }

    fun onEvent(event: ProfileCacheEvent) {
        when (event) {
            is ProfileCacheEvent.LoadProfile -> loadProfile(forceRefresh = false)
            is ProfileCacheEvent.RefreshProfile -> loadProfile(forceRefresh = true)
            is ProfileCacheEvent.Retry -> loadProfile(forceRefresh = true)
        }
    }

    private fun loadProfile(forceRefresh: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // 1) try cache when not force-refresh
                if (!forceRefresh) {
                    val cachedDto: ProfileCacheDto? = profileCacheRepository.getProfileCache()
                    val cachedUser: CacheUser? = cachedDto?.toDomainUser()

                    if (cachedUser != null) {
                        _uiState.update {
                            it.copy(isLoading = false, user = cachedUser, isFromCache = true, error = null)
                        }
                        return@launch
                    }
                }

                // 2) no cache (or forced refresh) => instruct repository to fetch from network and cache it
                // Expectation: fetchAndCacheProfile performs the API call, persists to Room, and returns the cached DTO
                val fetchedCacheDto: ProfileCacheDto? = profileCacheRepository.fetchAndCacheProfile()

                val fetchedUser: CacheUser? = fetchedCacheDto?.toDomainUser()
                if (fetchedUser != null) {
                    _uiState.update {
                        it.copy(isLoading = false, user = fetchedUser, isFromCache = false, error = null)
                    }
                } else {
                    val errMsg = fetchedCacheDto?.message ?: "Failed to fetch profile"
                    _uiState.update {
                        it.copy(isLoading = false, error = errMsg)
                    }
                }
            } catch (t: Throwable) {
                _uiState.update { it.copy(isLoading = false, error = t.localizedMessage ?: "Unknown error") }
            }
        }
    }
}
