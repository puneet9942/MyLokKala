package com.example.museapp.presentation.feature.events

sealed class EventsEvent {
    data class OnTabSelected(val index: Int) : EventsEvent()
    data object OnPostEventClick : EventsEvent()
    data object FetchLocation : EventsEvent()
}