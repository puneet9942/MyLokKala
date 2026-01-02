package com.example.museapp.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.example.museapp.presentation.feature.home.HomeViewModel
import com.example.museapp.presentation.feature.home.HomeEvent
import com.example.museapp.presentation.ui.components.appTextFieldColors
import com.example.museapp.ui.theme.AppTypography
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.museapp.presentation.feature.favorites.FavoritesViewModel
import com.example.museapp.presentation.feature.favorites.FavoritesEvent
import com.example.museapp.presentation.ui.components.UserCardComposable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedTabContent(
    homeViewModel: HomeViewModel,
    // accept shared favoritesViewModel from parent; default to hiltViewModel() for backward compatibility
    favoritesViewModel: FavoritesViewModel = hiltViewModel(),
    onNavigateToDetails: (String) -> Unit = {}
) {
    // Home UI state (search box reuse)
    val uiState by homeViewModel.uiState.collectAsState()

    // Favorites VM (manages user favorites)
    val favState by favoritesViewModel.uiState.collectAsState()

    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // load favorites when this composable is first launched
    LaunchedEffect(Unit) {
        favoritesViewModel.onEvent(FavoritesEvent.LoadFavorites)
    }

    // collect favourites VM effects (snackbars/errors)
    LaunchedEffect(Unit) {
        favoritesViewModel.effects.collectLatest { eff ->
            when (eff) {
                is com.example.museapp.presentation.feature.home.HomeUiEffect.ShowSnackbar -> {
                    coroutineScope.launch { snackbarHostState.showSnackbar(eff.message) }
                }
                is com.example.museapp.presentation.feature.home.HomeUiEffect.ShowError -> {
                    coroutineScope.launch { snackbarHostState.showSnackbar(eff.message) }
                }
                else -> { /* navigation effects not expected here */ }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Color.White,
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .background(Color.White)
                .padding(innerPadding)
        ) {
            // Search field (re-uses HomeViewModel's search to filter favorites)
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { homeViewModel.onEvent(HomeEvent.SearchChanged(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                placeholder = { Text("Search saved people...", style = AppTypography.titleSmall) },
                colors = appTextFieldColors(),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { homeViewModel.onEvent(HomeEvent.SearchChanged("")) }) {
                        Icon(Icons.Default.Favorite, contentDescription = "Clear")
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // loading indicator
            if (favState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
            }

            // show error if any
            favState.error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = AppTypography.titleSmall,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // filter favorites by search query (case-insensitive fullName match)
            val query = uiState.searchQuery.trim()
            val displayed = remember(favState.favorites, query) {
                if (query.isBlank()) favState.favorites
                else favState.favorites.filter {
                    val name = it.favoriteUser.fullName ?: ""
                    name.contains(query, ignoreCase = true)
                }
            }

            if (!favState.isLoading && displayed.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No saved people", style = AppTypography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Tap the heart on any user to save them here.", style = AppTypography.bodySmall)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { favoritesViewModel.onEvent(FavoritesEvent.LoadFavorites) }) {
                        Text("Reload")
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(displayed, key = { it.id }) { fav ->
                        UserCardComposable(
                            user = fav.favoriteUser,
                            distanceKm = null,
                            currentLat = null,
                            currentLng = null,
                            isFavorite = true,
                            selectedSkill = "",
                            knownSkills = emptyList(),
                            onFavorite = {
                                // remove favorite by record id
                                favoritesViewModel.onEvent(FavoritesEvent.RemoveFavorite(fav.id))
                            },
                            onViewProfile = { onNavigateToDetails(fav.favoriteUser.id) }
                        )
                    }
                }
            }
        }
    }
}
