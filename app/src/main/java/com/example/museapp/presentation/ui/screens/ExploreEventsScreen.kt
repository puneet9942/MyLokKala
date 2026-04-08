package com.example.museapp.presentation.ui.screens

import android.location.Location
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.museapp.domain.model.ExploreEvent
import com.example.museapp.presentation.feature.exploreevents.ExploreEventViewModel
import com.example.museapp.ui.theme.AppTypography

@Composable
fun ExploreEventsScreen(
    viewModel: ExploreEventViewModel = hiltViewModel(),
    loc: Location
) {

    val state = viewModel.uiState

    // ✅ Prevent multiple API calls
    LaunchedEffect(loc.latitude, loc.longitude) {
        viewModel.fetchExploreEvents(loc)
    }

    when {
        state.selectedEvent != null -> {
            ExploreEventDetailScreen(
                event = state.selectedEvent,
                onBack = { viewModel.onBackFromDetail() }
            )
        }

        state.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        state.error != null -> {
            Text(
                text = state.error ?: "Something went wrong",
                color = Color.Red,
                modifier = Modifier.padding(16.dp)
            )
        }

        else -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                items(state.events) { event ->
                    ExploreEventItem(
                        event = event,
                        onClick = { viewModel.onEventClick(event) }
                    )
                }
            }
        }
    }
}

@Composable
fun ExploreEventItem(
    event: ExploreEvent,
    onClick: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp)
    ) {

        Text(
            text = event.title,
            style = AppTypography.titleLarge
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = event.date,
            style = AppTypography.bodySmall,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = event.location,
            style = AppTypography.bodySmall
        )
    }
}

@Composable
fun ExploreEventDetailScreen(
    event: ExploreEvent,
    onBack: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "← Back",
            modifier = Modifier.clickable { onBack() },
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = event.title,
            style = AppTypography.titleLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(event.date)
        Text(event.location)

        Spacer(modifier = Modifier.height(16.dp))

        if (event.ticketLink.isNotEmpty()) {
            Text(
                text = "Book Tickets",
                color = Color.Blue,
                modifier = Modifier.clickable {
                    // TODO: openUrl(event.ticketLink)
                }
            )
        }
    }
}