package com.example.museapp.presentation.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.Button
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.museapp.presentation.feature.favorites.FavoritesEvent
import com.example.museapp.presentation.feature.favorites.FavoritesViewModel
import com.example.museapp.presentation.ui.components.UserCardComposable
import com.example.museapp.ui.theme.AppTypography
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val TAG = "SavedScreen"

@Composable
fun SavedScreen(
    favoritesViewModel: FavoritesViewModel = hiltViewModel(),
    onNavigateToDetails: (String) -> Unit = {}
) {
    val favState by favoritesViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Ensure favorites loaded
    LaunchedEffect(Unit) {
        Log.d(TAG, "LaunchedEffect -> requesting load")
        favoritesViewModel.onEvent(FavoritesEvent.LoadFavorites)
    }

    // Effects -> snackbar
    LaunchedEffect(Unit) {
        favoritesViewModel.effects.collectLatest { eff ->
            when (eff) {
                is com.example.museapp.presentation.feature.home.HomeUiEffect.ShowSnackbar -> coroutineScope.launch {
                    snackbarHostState.showSnackbar(eff.message)
                }
                is com.example.museapp.presentation.feature.home.HomeUiEffect.ShowError -> coroutineScope.launch {
                    snackbarHostState.showSnackbar(eff.message)
                }
                else -> {}
            }
        }
    }

    var selectedTab by remember { mutableStateOf(1) } // people tab
    val tabTitles = listOf("Saved", "People")

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.TopCenter))
        }

        TabRow(selectedTabIndex = selectedTab) {
            tabTitles.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }) {
                    Text(text = title, modifier = Modifier.padding(12.dp))
                }
            }
        }

        when (selectedTab) {
            0 -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No saved items (app uses user favorites - switch to People tab)")
                }
            }
            1 -> {
                // People tab
                Box(modifier = Modifier.fillMaxSize()) {
                    if (favState.isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Debug header showing counts and reload button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Favorites: ${favState.favorites.size}", style = AppTypography.titleSmall)
                                Button(onClick = {
                                    Log.d(TAG, "manual reload clicked")
                                    favoritesViewModel.onEvent(FavoritesEvent.LoadFavorites)
                                }) {
                                    Text("Reload")
                                }
                            }

                            if (!favState.error.isNullOrBlank()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Error loading saved people")
                                        Text(favState.error ?: "Unknown")
                                    }
                                }
                            } else if (favState.favorites.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("No saved people found", style = AppTypography.titleSmall)
                                        Text("If you expect favorites, check Logcat for 'FavoritesVM' & 'FakeFavRepo' messages.")
                                    }
                                }
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(favState.favorites, key = { it.id }) { fav ->
                                        UserCardComposable(
                                            user = fav.favoriteUser,
                                            distanceKm = null,
                                            currentLat = null,
                                            currentLng = null,
                                            isFavorite = true,
                                            onFavorite = {
                                                favoritesViewModel.onEvent(FavoritesEvent.RemoveFavorite(fav.id))
                                            },
                                            onViewProfile = { onNavigateToDetails(fav.favoriteUser.id) },
                                            selectedSkill = "",
                                            knownSkills = emptyList()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
