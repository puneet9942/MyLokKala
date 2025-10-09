package com.example.lokkala.presentation.feature.home

import com.example.lokkala.domain.model.Ad

/**
 * UI state for the Home screen / Home tab.
 * Keep only what the UI needs here.
 */
sealed class HomeUiEffect {
    data class NavigateToDetails(val adId: String) : HomeUiEffect()
    data class ShowSnackbar(val message: String) : HomeUiEffect()
    data class ShowError(val message: String) : HomeUiEffect()
    object RefreshCompleted : HomeUiEffect()
}