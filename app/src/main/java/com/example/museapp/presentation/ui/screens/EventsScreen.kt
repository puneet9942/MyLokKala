package com.example.museapp.presentation.events

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.Text
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.museapp.presentation.feature.events.EventsEvent
import com.example.museapp.presentation.feature.events.EventsState
import com.example.museapp.presentation.ui.screens.ExploreEventsScreen
import com.example.museapp.ui.theme.AppTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(
    state: EventsState,
    onEvent: (EventsEvent) -> Unit,
    latLong: Pair<Double, Double>?
) {
    LaunchedEffect(Unit) {
        onEvent(EventsEvent.FetchLocation)
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(text = "Events") }
                )
                TabRow(
                    selectedTabIndex = state.selectedTabIndex
                ) {
                    Tab(
                        selected = state.selectedTabIndex == 0,
                        onClick = { onEvent(EventsEvent.OnTabSelected(0)) },
                        text = { Text("My Events", style = AppTypography.bodyMedium) }
                    )
                    Tab(
                        selected = state.selectedTabIndex == 1,
                        onClick = { onEvent(EventsEvent.OnTabSelected(1)) },
                        text = { Text("Explore Events", style = AppTypography.bodyMedium) }
                    )
                }
            }
        },
        floatingActionButton = {
            if (state.selectedTabIndex == 0) {
                ExtendedFloatingActionButton(
                    onClick = { onEvent(EventsEvent.OnPostEventClick) },
                    shape = CircleShape,
                    text = { Text("Post Event") }
                )
            }
        }
    ) { padding ->
        when (state.selectedTabIndex) {
            0 -> MyEventsContent(
                modifier = Modifier.padding(padding),
                latLong = latLong
            )
            1 -> {
                latLong?.let { (lat, lng) ->
                    val location = android.location.Location("").apply {
                        latitude = lat
                        longitude = lng
                    }

                    ExploreEventsScreen(
                        loc = location
                    )
                } ?: Text(
                    text = "Location not available",
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun MyEventsContent(
    modifier: Modifier = Modifier,
    latLong: Pair<Double, Double>?
) {
    val lat = latLong?.first
    val lng = latLong?.second

    Text(
        text = "Lat: ${lat ?: "NA"}, Lng: ${lng ?: "NA"}",
        modifier = modifier,
        style = AppTypography.bodyMedium
    )
}

@Composable
private fun ExploreContent(modifier: Modifier = Modifier) {
    Text(
        text = "Explore Content",
        modifier = modifier,
        style = AppTypography.bodyMedium
    )
}