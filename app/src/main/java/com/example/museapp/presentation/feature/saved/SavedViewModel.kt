package com.example.museapp.presentation.feature.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.museapp.data.util.NetworkResult
import com.example.museapp.domain.model.Ad
import com.example.museapp.domain.usecase.AddFavoriteUseCase
import com.example.museapp.domain.usecase.GetFavoritesUseCase
import com.example.museapp.domain.usecase.RemoveFavoriteUseCase
import com.example.museapp.presentation.feature.home.HomeUiEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class SavedViewModel @Inject constructor(
    private val getFavs: GetFavoritesUseCase,
    private val addFav: AddFavoriteUseCase,
    private val removeFav: RemoveFavoriteUseCase
) : ViewModel() {

//    private val _ads = MutableStateFlow<List<Ad>>(emptyList())
//    val ads: StateFlow<List<Ad>> = _ads.asStateFlow()
//
//    private val _isLoading = MutableStateFlow(false)
//    private val _error = MutableStateFlow<String?>(null)
//    private val _search = MutableStateFlow("")
//    private val _effects = MutableSharedFlow<HomeUiEffect>(replay = 0, onBufferOverflow = BufferOverflow.DROP_OLDEST)
//
//    val effects: SharedFlow<HomeUiEffect> = _effects.asSharedFlow()
//
//    val uiState: StateFlow<com.example.museapp.presentation.feature.home.HomeUiState> =
//        combine(_isLoading, _ads, _error, _search) { isLoading, ads, error, search ->
//            val filtered = if (search.isBlank()) ads else ads.filter {
//                it.user.name.contains(search, ignoreCase = true) ||
//                        it.primarySkill.contains(search, ignoreCase = true) ||
//                        (it.description ?: "").contains(search, ignoreCase = true)
//            }
//            com.example.museapp.presentation.feature.home.HomeUiState(
//                ads = filtered,
//                allAdsCount = ads.size,
//                favorites = ads.map { it.id }.toSet(),
//                searchQuery = search,
//                isLoading = isLoading,
//                isRefreshing = false
//            )
//        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.museapp.presentation.feature.home.HomeUiState())
//
//    init {
//        refresh()
//    }
//
//    fun onSearchChanged(q: String) {
//        _search.value = q
//    }
//
//    fun refresh() {
//        viewModelScope.launch {
//            _isLoading.value = true
//            _error.value = null
//            when (val r = getFavs()) {
//                is NetworkResult.Success -> _ads.value = r.data ?: emptyList()
//                is NetworkResult.Error -> {
//                    _error.value = r.message ?: "Failed to load saved"
//                    _effects.emit(HomeUiEffect.ShowError(_error.value ?: "Unknown"))
//                }
//            }
//            _isLoading.value = false
//        }
//    }
//
//    fun toggleFavorite(adId: String) {
//        viewModelScope.launch {
//            // If ad present in local list -> remove; otherwise add.
//            val isPresent = _ads.value.any { it.id == adId }
//            if (isPresent) {
//                when (val r = removeFav(adId)) {
//                    is NetworkResult.Success -> {
//                        _ads.value = _ads.value.filter { it.id != adId }
//                        _effects.emit(HomeUiEffect.ShowSnackbar("Removed from favourites"))
//                    }
//                    is NetworkResult.Error -> _effects.emit(HomeUiEffect.ShowError(r.message ?: "Failed to remove"))
//                }
//            } else {
//                when (val r = addFav(adId)) {
//                    is NetworkResult.Success -> {
//                        // After add, refresh to fetch full ad data from server/fake repo
//                        refresh()
//                        _effects.emit(HomeUiEffect.ShowSnackbar("Added to favourites"))
//                    }
//                    is NetworkResult.Error -> _effects.emit(HomeUiEffect.ShowError(r.message ?: "Failed to add"))
//                }
//            }
//        }
//    }
//
//    fun findAdById(adId: String): Ad? = _ads.value.firstOrNull { it.id == adId }
}