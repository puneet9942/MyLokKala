package com.example.museapp.presentation.feature.profile

import com.example.museapp.domain.model.CacheUser
import com.example.museapp.domain.model.User

/**
 * UI state for ProfileCache display
 * Prefixed with ProfileCache to avoid regression with existing Profile classes.
 */
data class ProfileCacheState(
    val isLoading: Boolean = false,
    val user: CacheUser? = null,
    val error: String? = null,
    val isFromCache: Boolean = false
)
