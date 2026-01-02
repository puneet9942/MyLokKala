package com.example.museapp.presentation.feature.saved

sealed class SavedEvent {
    object LoadSavedAds : SavedEvent()
    data class ToggleFavorite(val adId: String, val isCurrentlyFavorite: Boolean) : SavedEvent()
}
