package com.example.museapp.presentation.feature.home

import com.example.museapp.domain.model.Ad
import com.example.museapp.domain.model.User
import com.example.museapp.util.AppConstants

/**
 * One-off UI events / effects.
 */
sealed class HomeUiEffect {
    data class NavigateToDetails(val id: String) : HomeUiEffect()
    data class ShowSnackbar(val message: String) : HomeUiEffect()
    data class ShowError(val message: String) : HomeUiEffect()
    object RefreshCompleted : HomeUiEffect()
}

/**
 * Main UI state for the home tab (kept minimal and compatible).
 */
data class HomeUiState(
    val ads: List<Ad> = emptyList(),
    val allAdsCount: Int = 0,
    val skills: List<String> = listOf("All", "Other"),
    val favorites: Set<String> = emptySet(),
    val selectedSkill: String = "All",
    val searchQuery: String = "",
    val currentLat: Double = AppConstants.DEFAULT_LAT,
    val currentLng: Double = AppConstants.DEFAULT_LNG,
    val locationLabel: String = AppConstants.DEFAULT_LOCATION_NAME,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
) {
    val isEmpty: Boolean get() = ads.isEmpty() && !isLoading
}

/**
 * Events emitted from the UI.
 */
sealed interface HomeEvent {
    data class SearchChanged(val query: String) : HomeEvent
    data class SkillSelected(val skill: String) : HomeEvent
    data class ToggleFavorite(val adId: String) : HomeEvent
    data class ViewDetails(val adId: String) : HomeEvent
    object Refresh : HomeEvent
    object ClearError : HomeEvent
    data class UpdateLocation(val lat: Double, val lng: Double) : HomeEvent
}

/**
 * Users-related UI state
 */
data class HomeUsersState(
    val isLoading: Boolean = false,
    val users: List<UserWithDistance> = emptyList(),
    val error: String? = null
)

data class UserWithDistance(
    val user: User,
    val distanceKm: Double? = null
)
