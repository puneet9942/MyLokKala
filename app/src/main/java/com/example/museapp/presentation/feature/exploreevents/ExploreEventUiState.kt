package com.example.museapp.presentation.feature.exploreevents

import com.example.museapp.domain.model.ExploreEvent

data class ExploreEventUiState(
    val isLoading: Boolean = false,
    val events: List<ExploreEvent> = emptyList(),
    val selectedEvent: ExploreEvent? = null,
    val error: String? = null
)