package com.example.museapp.presentation.feature.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.museapp.util.LocationProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EventsViewModel @Inject constructor(
    private val locationProvider: LocationProvider
) : ViewModel() {

    private val _state = MutableStateFlow(EventsState())
    val state: StateFlow<EventsState> = _state

    private val _latLong = MutableStateFlow<Pair<Double, Double>?>(null)
    val latLong: StateFlow<Pair<Double, Double>?> = _latLong

    fun onEvent(event: EventsEvent) {
        when (event) {
            is EventsEvent.OnTabSelected -> {
                _state.update { it.copy(selectedTabIndex = event.index) }
            }

            EventsEvent.OnPostEventClick -> {
                // handle click
            }

            EventsEvent.FetchLocation -> {
                fetchLocation()
            }
        }
    }

    private fun fetchLocation() {
        viewModelScope.launch {
            val location = locationProvider.getLastKnownLocation()
            _latLong.value = location?.let { it.latitude to it.longitude }
        }
    }
}