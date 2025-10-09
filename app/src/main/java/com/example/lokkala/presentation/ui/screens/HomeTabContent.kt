package com.example.lokkala.presentation.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.lokkala.presentation.feature.home.HomeEvent
import com.example.lokkala.presentation.feature.home.HomeUiEffect
import com.example.lokkala.presentation.feature.home.HomeViewModel
import com.example.lokkala.util.AddressHelper
import com.example.lokkala.util.AppConstants
import com.example.lokkala.util.LocationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.LocationOn
import coil.compose.AsyncImage
import com.example.lokkala.presentation.ui.components.AdCardComposable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTabContent(
    viewModel: HomeViewModel,
    onNavigateToDetails: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Local UI state for address (human-friendly)
    var placeName by rememberSaveable { mutableStateOf<String?>(null) }
    var isResolvingAddress by remember { mutableStateOf(false) }

    // Permission launcher (single permission)
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.refreshDeviceLocation()
        } else {
            scope.launch { snackbarHostState.showSnackbar("Location permission denied") }
        }
    }

    // If permission already granted, refresh device location once
    LaunchedEffect(Unit) {
        if (LocationHelper.hasLocationPermission(context)) {
            viewModel.refreshDeviceLocation()
        } else {
            // If no permission, show default place name immediately
            placeName = AppConstants.DEFAULT_LOCATION_NAME
        }
    }

    // Collect effects
    LaunchedEffect(Unit) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is HomeUiEffect.NavigateToDetails -> onNavigateToDetails(effect.adId)
                is HomeUiEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
                is HomeUiEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
                is HomeUiEffect.RefreshCompleted -> { /* no-op */ }
            }
        }
    }

    // Resolve address when coordinates change (non-blocking)
    LaunchedEffect(uiState.currentLat, uiState.currentLng) {
        // Reset while resolving
        placeName = null
        if (LocationHelper.hasLocationPermission(context)) {
            isResolvingAddress = true
            val resolved = try {
                withContext(Dispatchers.IO) {
                    AddressHelper.getCurrentAddress(context)
                }
            } catch (t: Throwable) {
                null
            }

            placeName = when (resolved) {
                null -> AppConstants.DEFAULT_LOCATION_NAME
                "Location permission not granted" -> AppConstants.DEFAULT_LOCATION_NAME
                "Location not available" -> AppConstants.DEFAULT_LOCATION_NAME
                else -> resolved
            }
            isResolvingAddress = false
        } else {
            // No permission: show default name
            placeName = AppConstants.DEFAULT_LOCATION_NAME
            isResolvingAddress = false
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { innerPadding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .padding(innerPadding)
        ) {
            // Location header - shows friendly name when available, otherwise coordinates
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Current Location", style = MaterialTheme.typography.titleSmall, color = Color(0xFF6B4F3F))
                    if (isResolvingAddress) {
                        Text(text = "Resolving...", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    } else {
                        Text(
                            text = placeName ?: "${String.format("%.5f", uiState.currentLat)}, ${String.format("%.5f", uiState.currentLng)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }

                IconButton(onClick = {
                    // request permission and then refresh
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Use current location")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search & chips & list (UI behavior preserved)
            var localSearch by rememberSaveable { mutableStateOf(uiState.searchQuery) }

            OutlinedTextField(
                value = localSearch,
                onValueChange = {
                    localSearch = it
                    viewModel.onEvent(HomeEvent.SearchChanged(it))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                placeholder = { Text("Search for artists, services...") },
                singleLine = true,
                leadingIcon = { Icon(imageVector = Icons.Filled.Favorite, contentDescription = null, tint = Color(0xFFF27B4B)) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Skills chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.skills) { skill ->
                    val isSelected = skill.equals(uiState.selectedSkill, ignoreCase = true)
                    Surface(
                        modifier = Modifier
                            .wrapContentSize()
                            .clickable { viewModel.onEvent(HomeEvent.SkillSelected(skill)) },
                        shape = RoundedCornerShape(18.dp),
                        color = if (isSelected) Color(0xFFF27B4B) else Color.White,
                        tonalElevation = if (isSelected) 6.dp else 0.dp
                    ) {
                        Text(
                            text = skill,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            color = if (isSelected) Color.White else Color.DarkGray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
            }

            uiState.error?.let { err ->
                Text(text = err, color = Color.Red, modifier = Modifier.padding(8.dp))
            }

            // Ads list (sorted by distance in VM)
            LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                items(uiState.ads) { ad ->
                    AdCardComposable(
                        ad = ad,
                        isFavorite = uiState.favorites.contains(ad.id),
                        onFavoriteToggle = { viewModel.onEvent(HomeEvent.ToggleFavorite(ad.id)) },
                        onViewDetails = { viewModel.onEvent(HomeEvent.ViewDetails(ad.id)) },
                        currentLat = uiState.currentLat,
                        currentLng = uiState.currentLng
                    )
                }
            }
        }
    }
}
