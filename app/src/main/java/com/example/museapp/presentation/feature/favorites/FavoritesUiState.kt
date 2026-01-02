package com.example.museapp.presentation.feature.favorites

import com.example.museapp.domain.model.FavoriteUser

data class FavoritesUiState(
    val isLoading: Boolean = false,
    val favorites: List<FavoriteUser> = emptyList(),
    val error: String? = null
)
