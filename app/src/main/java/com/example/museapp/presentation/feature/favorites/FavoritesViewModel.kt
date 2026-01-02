package com.example.museapp.presentation.feature.favorites

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.museapp.data.util.NetworkResult
import com.example.museapp.domain.model.FavoriteUser
import com.example.museapp.domain.usecase.AddUserFavoriteUseCase
import com.example.museapp.domain.usecase.GetUserFavoritesUseCase
import com.example.museapp.domain.usecase.RemoveUserFavoriteUseCase
import com.example.museapp.presentation.feature.home.HomeUiEffect
import com.example.museapp.util.FavoritesChangeBus
import com.example.museapp.util.FavoriteChange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "FavoritesVM"

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val getUserFavoritesUseCase: GetUserFavoritesUseCase,
    private val addUserFavoriteUseCase: AddUserFavoriteUseCase,
    private val removeUserFavoriteUseCase: RemoveUserFavoriteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<HomeUiEffect>()
    val effects: SharedFlow<HomeUiEffect> = _effects.asSharedFlow()

    init {
        // initial load
        onEvent(FavoritesEvent.LoadFavorites)

        // react to external changes (id-only). If it's an add -> reload (to get full object).
        // If it's a remove -> remove by id locally.
        viewModelScope.launch {
            try {
                FavoritesChangeBus.changes.collectLatest { change ->
                    if (change.isAdded) {
                        Log.d(TAG, "External favorite added -> reloading favorites id=${change.favoriteId}")
                        // reload to fetch full favorite object (safe approach)
                        loadFavorites()
                    } else {
                        Log.d(TAG, "External favorite removed -> remove locally id=${change.favoriteId}")
                        _uiState.update { current ->
                            current.copy(favorites = current.favorites.filterNot { it.id == change.favoriteId })
                        }
                    }
                }
            } catch (t: Throwable) {
                Log.w(TAG, "FavoritesViewModel: favorites bus collector failed: ${t.message}")
            }
        }
    }

    fun onEvent(event: FavoritesEvent) {
        when (event) {
            is FavoritesEvent.LoadFavorites -> loadFavorites()
            is FavoritesEvent.AddFavorite -> addFavorite(event.userId)
            is FavoritesEvent.RemoveFavorite -> removeFavorite(event.favoriteId)
            // If your FavoritesEvent has other events (e.g., SearchChanged) keep them here
        }
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val res = getUserFavoritesUseCase()) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, favorites = res.data ?: emptyList())
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = res.message)
                    _effects.emit(HomeUiEffect.ShowError(res.message ?: "Failed to load favorites"))
                }
            }
        }
    }

    private fun addFavorite(userId: String) {
        viewModelScope.launch {
            when (val res = addUserFavoriteUseCase(userId)) {
                is NetworkResult.Success -> {
                    res.data?.let { fav ->
                        // update local ui state with the returned favorite object
                        _uiState.update { current -> current.copy(favorites = current.favorites + fav) }
                        _effects.emit(HomeUiEffect.ShowSnackbar("Added to favorites"))
                        // notify other instances by id-only (they will reload as needed)
                        FavoritesChangeBus.tryEmit(FavoriteChange(favoriteId = fav.id, isAdded = true))
                        Log.d(TAG, "addFavorite -> success id=${fav.id} ; emitted change")
                    } ?: run {
                        _effects.emit(HomeUiEffect.ShowError("Empty add response"))
                    }
                }
                is NetworkResult.Error -> {
                    _effects.emit(HomeUiEffect.ShowError(res.message ?: "Failed to add favorite"))
                }
            }
        }
    }

    private fun removeFavorite(favoriteId: String) {
        viewModelScope.launch {
            when (val res = removeUserFavoriteUseCase(favoriteId)) {
                is NetworkResult.Success -> {
                    // remove locally
                    _uiState.update { current -> current.copy(favorites = current.favorites.filterNot { it.id == favoriteId }) }
                    _effects.emit(HomeUiEffect.ShowSnackbar("Removed from favorites"))
                    // notify other instances (they will remove locally or reload if needed)
                    FavoritesChangeBus.tryEmit(FavoriteChange(favoriteId = favoriteId, isAdded = false))
                    Log.d(TAG, "removeFavorite -> success id=$favoriteId ; emitted change")
                }
                is NetworkResult.Error -> {
                    _effects.emit(HomeUiEffect.ShowError(res.message ?: "Failed to remove favorite"))
                }
            }
        }
    }
}
