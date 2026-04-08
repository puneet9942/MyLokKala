package com.example.museapp.presentation.feature.exploreevents

import android.location.Location
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.museapp.domain.model.ExploreEvent
import com.example.museapp.domain.usecase.GetExploreEventsUseCase
import com.example.museapp.util.LocationProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExploreEventViewModel @Inject constructor(
    private val getExploreEventsUseCase: GetExploreEventsUseCase,
    private val locationProvider: LocationProvider
) : ViewModel() {

    var uiState by mutableStateOf(ExploreEventUiState())
        private set

    fun fetchExploreEvents(loc: Location) {
        viewModelScope.launch {

            // ✅ Set loading + clear old error
            uiState = uiState.copy(
                isLoading = true,
                error = null
            )

            try {
                val area = locationProvider.getAreaFromLocation(loc)

                val result = getExploreEventsUseCase("events in $area")

                result.fold(
                    onSuccess = { events ->
                        uiState = uiState.copy(
                            isLoading = false,
                            events = events
                        )
                    },
                    onFailure = { error ->
                        uiState = uiState.copy(
                            isLoading = false,
                            error = error.message ?: "Something went wrong"
                        )
                    }
                )

            } catch (e: Exception) {
                uiState = uiState.copy(
                    isLoading = false,
                    error = e.message ?: "Unexpected error"
                )
            }
        }
    }

    // ✅ Fix: correct type
    fun onEventClick(event: ExploreEvent) {
        uiState = uiState.copy(selectedEvent = event)
    }

    fun onBackFromDetail() {
        uiState = uiState.copy(selectedEvent = null)
    }
}