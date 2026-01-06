package com.example.museapp.presentation.feature.profile

/**
 * Intent / events for the ProfileCache display screen
 * Prefixed with ProfileCache to avoid regression with existing Profile classes.
 */
sealed class ProfileCacheEvent {
    object LoadProfile : ProfileCacheEvent()           // initial load when user taps profile icon
    object RefreshProfile : ProfileCacheEvent()        // force-refresh from network
    data class Retry(val reason: String? = null) : ProfileCacheEvent()
}
