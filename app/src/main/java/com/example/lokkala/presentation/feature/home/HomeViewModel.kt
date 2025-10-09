package com.example.lokkala.presentation.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lokkala.data.util.NetworkResult
import com.example.lokkala.domain.model.Ad
import com.example.lokkala.domain.usecase.GetAdsUseCase
import com.example.lokkala.domain.usecase.GetSkillsUseCase
import com.example.lokkala.util.AppConstants
import com.example.lokkala.util.LocationProvider
import com.example.lokkala.util.DistanceUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getAdsUseCase: GetAdsUseCase,
    private val getSkillsUseCase: GetSkillsUseCase,
    private val locationProvider: LocationProvider
) : ViewModel() {

    // --- backing input flows ---
    private val _allAds = MutableStateFlow<List<Ad>>(emptyList())

    // canonical chips: "All" + defaults + "Other"
    private val _skills = MutableStateFlow<List<String>>(listOf("All") + AppConstants.DEFAULT_INTERESTS + listOf("Other"))

    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    private val _selectedSkill = MutableStateFlow("All")
    private val _searchQuery = MutableStateFlow("")
    private val _currentLat = MutableStateFlow(AppConstants.DEFAULT_LAT)
    private val _currentLng = MutableStateFlow(AppConstants.DEFAULT_LNG)
    private val _isLoading = MutableStateFlow(false)
    private val _isRefreshing = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    // --- ui state exposed to the UI ---
    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true,
        currentLat = AppConstants.DEFAULT_LAT,
        currentLng = AppConstants.DEFAULT_LNG
    ))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // --- one-off effects ---
    private val _effects = MutableSharedFlow<HomeUiEffect>(
        replay = 0,
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val effects: SharedFlow<HomeUiEffect> = _effects.asSharedFlow()

    init {
        // recompute UI whenever any backing flow changes
        viewModelScope.launch {
            merge(
                _allAds.map { Unit },
                _skills.map { Unit },
                _favorites.map { Unit },
                _selectedSkill.map { Unit },
                _searchQuery.map { Unit },
                _currentLat.map { Unit },
                _currentLng.map { Unit },
                _isLoading.map { Unit },
                _isRefreshing.map { Unit },
                _error.map { Unit }
            ).collect {
                recomputeUiState()
            }
        }

        // Load data and attempt to fetch device location if possible
        loadInitialData()

        viewModelScope.launch {
            try {
                val loc = locationProvider.getLastKnownLocation()
                if (loc != null) updateCurrentLocation(loc.latitude, loc.longitude)
            } catch (_: Exception) {
                // ignore; will use default coords
            }
        }
    }

    private fun recomputeUiState() {
        val ads = _allAds.value
        val skills = _skills.value
        val favs = _favorites.value
        val selected = _selectedSkill.value
        val search = _searchQuery.value
        val lat = _currentLat.value
        val lng = _currentLng.value
        val loading = _isLoading.value
        val refreshing = _isRefreshing.value
        val err = _error.value

        val q = search.trim().lowercase()
        val filtered = ads.filter { ad ->
            // filter only by primarySkill (not by user.skills)
            val matchesSkill = when {
                selected.equals("All", true) -> true
                selected.equals("Other", true) -> {
                    AppConstants.DEFAULT_INTERESTS.none { it.equals(ad.primarySkill, ignoreCase = true) }
                }
                else -> ad.primarySkill.equals(selected, ignoreCase = true)
            }

            val matchesQuery = q.isEmpty() ||
                    ad.user.name.lowercase().contains(q) ||
                    (ad.description?.lowercase()?.contains(q) ?: false) ||
                    ad.primarySkill.lowercase().contains(q)

            matchesSkill && matchesQuery
        }

        // sort by distance (nearest first)
        val sortedByDistance = filtered.sortedBy { ad ->
            DistanceUtils.distanceKm(lat, lng, ad.user.lat, ad.user.lng)
        }

        _uiState.value = HomeUiState(
            ads = sortedByDistance,
            allAdsCount = ads.size,
            skills = skills,
            favorites = favs,
            selectedSkill = selected,
            searchQuery = search,
            currentLat = lat,
            currentLng = lng,
            isLoading = loading,
            isRefreshing = refreshing,
            error = err
        )
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            when (val adsRes = getAdsUseCase()) {
                is NetworkResult.Success -> {
                    val ads = adsRes.data ?: emptyList()
                    _allAds.value = ads
                    // use canonical interest chips on initial load
                    _skills.value = listOf("All") + AppConstants.DEFAULT_INTERESTS + listOf("Other")
                }
                is NetworkResult.Error -> {
                    _error.value = adsRes.message ?: "Failed to load ads"
                    launch { _effects.emit(HomeUiEffect.ShowError(_error.value ?: "Unknown error")) }
                }
            }

            _isLoading.value = false
        }
    }

    // --- UI event handler ---
    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.SearchChanged -> _searchQuery.value = event.query
            is HomeEvent.SkillSelected -> _selectedSkill.value = event.skill
            is HomeEvent.ToggleFavorite -> toggleFavorite(event.adId)
            is HomeEvent.ViewDetails -> viewModelScope.launch { _effects.emit(HomeUiEffect.NavigateToDetails(event.adId)) }
            HomeEvent.Refresh -> refreshData()
            HomeEvent.ClearError -> _error.value = null
            is HomeEvent.UpdateLocation -> updateCurrentLocation(event.lat, event.lng)
        }
    }

    private fun toggleFavorite(adId: String) {
        val set = _favorites.value.toMutableSet()
        val nowFav = if (set.contains(adId)) {
            set.remove(adId); false
        } else {
            set.add(adId); true
        }
        _favorites.value = set

        viewModelScope.launch {
            val msg = if (nowFav) "Added to favorites" else "Removed from favorites"
            _effects.emit(HomeUiEffect.ShowSnackbar(msg))
        }
    }

    private fun refreshData() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _error.value = null

            // Refresh ads
            when (val adsRes = getAdsUseCase()) {
                is NetworkResult.Success -> _allAds.value = adsRes.data ?: emptyList()
                is NetworkResult.Error -> {
                    _error.value = adsRes.message ?: "Refresh failed"
                    _effects.emit(HomeUiEffect.ShowError(_error.value ?: "Unknown error"))
                }
            }

            // Refresh skills: prefer server list, fallback to canonical
            when (val skillsRes = getSkillsUseCase()) {
                is NetworkResult.Success -> {
                    val skills = skillsRes.data ?: AppConstants.DEFAULT_INTERESTS
                    _skills.value = listOf("All") + skills.sorted() + listOf("Other")
                }
                is NetworkResult.Error -> {
                    _skills.value = listOf("All") + AppConstants.DEFAULT_INTERESTS + listOf("Other")
                    _effects.emit(HomeUiEffect.ShowSnackbar(skillsRes.message ?: "Failed to refresh skills"))
                }
            }

            _effects.emit(HomeUiEffect.RefreshCompleted)
            _isRefreshing.value = false
        }
    }

    /**
     * Attempts to fetch the device's last-known location and update VM state.
     * Falls back to AppConstants.DEFAULT_* if not available.
     */
    fun refreshDeviceLocation() {
        viewModelScope.launch {
            val loc = try {
                locationProvider.getLastKnownLocation()
            } catch (t: Throwable) {
                null
            }

            if (loc != null) {
                updateCurrentLocation(loc.latitude, loc.longitude)
                _effects.emit(HomeUiEffect.ShowSnackbar("Location updated"))
            } else {
                // explicit fallback to default Faridabad coords
                updateCurrentLocation(AppConstants.DEFAULT_LAT, AppConstants.DEFAULT_LNG)
                _effects.emit(HomeUiEffect.ShowSnackbar("Using default location: ${AppConstants.DEFAULT_LOCATION_NAME}"))
            }
        }
    }

    private fun updateCurrentLocation(lat: Double, lng: Double) {
        _currentLat.value = lat
        _currentLng.value = lng
    }

    // helper used by DetailsScreen
    fun findAdById(adId: String): Ad? = _allAds.value.firstOrNull { it.id == adId }
}
