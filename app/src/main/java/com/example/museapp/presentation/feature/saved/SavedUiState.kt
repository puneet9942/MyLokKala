package com.example.museapp.presentation.feature.saved

import com.example.museapp.domain.model.Ad

data class SavedUiState(
    val isLoading: Boolean = false,
    val ads: List<Ad> = emptyList(),
    val error: String? = null
)
