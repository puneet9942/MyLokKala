package com.example.lokkala.presentation.feature.home

import com.example.lokkala.domain.model.Ad
import com.example.lokkala.util.AppConstants

/**
 * UI state for the Home screen / Home tab.
 * Keep only what the UI needs here.
 */
data class HomeUiState(
    val ads: List<Ad> = emptyList(),                // filtered list shown in UI
    val allAdsCount: Int = 0,                       // total loaded ads count
    val skills: List<String> = listOf("All", "Other"),
    val favorites: Set<String> = emptySet(),        // favorited ad ids
    val selectedSkill: String = "All",
    val searchQuery: String = "",
    val currentLat: Double = AppConstants.DEFAULT_LAT,
    val currentLng: Double = AppConstants.DEFAULT_LNG,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
) {
    val isEmpty: Boolean get() = ads.isEmpty() && !isLoading
}

sealed interface HomeEvent {
    data class SearchChanged(val query: String) : HomeEvent
    data class SkillSelected(val skill: String) : HomeEvent
    data class ToggleFavorite(val adId: String) : HomeEvent
    data class ViewDetails(val adId: String) : HomeEvent
    object Refresh : HomeEvent                       // user pulled to refresh
    object ClearError : HomeEvent                    // clear transient error
    data class UpdateLocation(val lat: Double, val lng: Double) : HomeEvent
}