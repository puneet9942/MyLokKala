package com.example.museapp.presentation.feature.home

import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.museapp.data.util.NetworkResult
import com.example.museapp.domain.model.User
import com.example.museapp.domain.usecase.GetAdsUseCase
import com.example.museapp.domain.usecase.GetUsersUseCase
import com.example.museapp.domain.repository.InterestsRepository
import com.example.museapp.util.AppConstants
import com.example.museapp.util.DistanceUtils
import com.example.museapp.util.LocationProvider
import com.example.museapp.util.AppContextProvider
import com.example.museapp.util.SharedPrefUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getAdsUseCase: GetAdsUseCase,
    private val getUsersUseCase: GetUsersUseCase,
    private val interestsRepository: InterestsRepository,
    private val locationProvider: LocationProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(
            currentLat = AppConstants.DEFAULT_LAT,
            currentLng = AppConstants.DEFAULT_LNG,
            locationLabel = AppConstants.DEFAULT_LOCATION_NAME
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _usersState = MutableStateFlow(HomeUsersState())
    val usersState: StateFlow<HomeUsersState> = _usersState.asStateFlow()

    private val _effects = MutableSharedFlow<HomeUiEffect>(replay = 0)
    val effects: SharedFlow<HomeUiEffect> = _effects.asSharedFlow()

    private var cachedUsers: List<User> = emptyList()

    private companion object {
        // SharedPref keys used across codebase
        private const val PREF_LAST_LAT = "last_lat"
        private const val PREF_LAST_LNG = "last_lng"
        private const val PREF_LOCATION_LABEL = "last_location_label"

        // distance threshold for deciding to re-resolve label (in meters)
        private const val DISTANCE_THRESHOLD_METERS = 50.0
    }

    init {
        // Observe local Room interests and ensure uiState.skills is populated.
        observeLocalSkills()

        // initial user load
        fetchUsers()

        // resolve current location once at startup and update UI + filter distances
        viewModelScope.launch {
            try {
                val loc = locationProvider.getLastKnownLocation()
                val lat = loc?.latitude ?: AppConstants.DEFAULT_LAT
                val lng = loc?.longitude ?: AppConstants.DEFAULT_LNG

                // Update UI state immediately with coords and best-available label (prefer persisted label)
                _uiState.update {
                    it.copy(
                        currentLat = lat,
                        currentLng = lng,
                        locationLabel = formatLocationLabel(lat, lng)
                    )
                }

                // cache the found coords for other utilities/interceptor
                try {
                    val ctx = AppContextProvider.getContext()
                    if (ctx != null && loc != null) {
                        SharedPrefUtils.putString(ctx, PREF_LAST_LAT, lat.toString())
                        SharedPrefUtils.putString(ctx, PREF_LAST_LNG, lng.toString())
                    }
                } catch (t: Throwable) {
                    Log.w("HomeViewModel", "Failed to cache location: ${t.message}")
                }

                // Decide whether to resolve human locality now (avoid redundant calls)
                val shouldResolve = shouldResolveLabel(lat, lng)
                if (shouldResolve) {
                    // ASYNCHRONOUS: attempt reverse-geocode to obtain locality (non-blocking)
                    viewModelScope.launch {
                        try {
                            val label = try {
                                if (loc != null) {
                                    locationProvider.getAreaFromLocation(loc)
                                } else {
                                    locationProvider.getAreaFromLastKnownLocation()
                                }
                            } catch (t: Throwable) {
                                Log.w("HomeViewModel", "locationProvider geocode threw: ${t.message}")
                                null
                            }

                            Log.d("HomeViewModel", "reverse geocode init result: $label")

                            if (!label.isNullOrBlank()) {
                                persistLabelSafely(label, lat, lng)
                                _uiState.update { s -> s.copy(locationLabel = label) }
                            } else {
                                Log.d("HomeViewModel", "No locality found during init for ($lat,$lng)")
                            }
                        } catch (t: Throwable) {
                            Log.w("HomeViewModel", "Reverse geocode failed in init: ${t.message}")
                        } finally {
                            applyFilters()
                        }
                    }
                } else {
                    applyFilters()
                }
            } catch (t: Throwable) {
                Log.w("HomeViewModel", "Location resolve failed: ${t.message}")
                _uiState.update {
                    it.copy(
                        currentLat = AppConstants.DEFAULT_LAT,
                        currentLng = AppConstants.DEFAULT_LNG,
                        locationLabel = AppConstants.DEFAULT_LOCATION_NAME
                    )
                }
                applyFilters()
            }
        }
    }

    /**
     * Returns a short user-readable label for the given coordinates.
     * Preference order:
     * 1) persisted SharedPref label (fast)
     * 2) coordinate string "Lat: xx, Lng: yy" (accurate)
     *
     * This method is synchronous and side-effect free (does not perform network calls).
     */
    private fun formatLocationLabel(lat: Double, lng: Double): String {
        // If default coords -> use the predefined label
        if (lat == AppConstants.DEFAULT_LAT && lng == AppConstants.DEFAULT_LNG) {
            return AppConstants.DEFAULT_LOCATION_NAME
        }

        // Try persisted SharedPref label
        val ctx = AppContextProvider.getContext()
        val persistedLabel = try {
            ctx?.let { SharedPrefUtils.getString(it, PREF_LOCATION_LABEL) }
        } catch (_: Throwable) {
            null
        }
        if (!persistedLabel.isNullOrBlank()) {
            return persistedLabel
        }

        // Nothing available: return precise coordinates immediately so user has accurate info
        return AppConstants.DEFAULT_LOCATION_NAME
    }

    private fun observeLocalSkills() {
        viewModelScope.launch {
            try {
                // If using fake repo flag, load defaults from AppConstants and don't observe Room DB
                if (AppConstants.USE_FAKE_REPO) {
                    Log.d("HomeViewModel", "USE_FAKE_REPO is true - loading DEFAULT_INTERESTS")
                    val defaults = processInterestNames(AppConstants.DEFAULT_INTERESTS ?: emptyList())
                    _uiState.update { old -> old.copy(skills = defaults) }
                    return@launch
                }

                fetchAndSeedIfEmpty()

                interestsRepository.getAllInterests().collect { entities ->
                    val names = entities
                        .mapNotNull { it.name?.trim() }
                        .filter { it.isNotEmpty() }
                        .distinctBy { it.lowercase() }
                        .sortedBy { it.lowercase() }

                    _uiState.update { old ->
                        // Keep existing selection, only replace list
                        old.copy(skills = listOf("All", "Other") + names)
                    }
                }
            } catch (t: Throwable) {
                Log.w("HomeViewModel", "observeLocalSkills failed: ${t.message}")
            }
        }
    }

    private suspend fun fetchAndSeedIfEmpty() {
        try {
            val firstSnapshot = interestsRepository.getAllInterests().firstOrNull()
            if (firstSnapshot.isNullOrEmpty()) {
                try {
                    withContext(Dispatchers.IO) {
                        interestsRepository.fetchAndSaveInterests(page = 1, limit = 100)
                    }
                } catch (t: Throwable) {
                    Log.w("HomeViewModel", "Seed interests failed: ${t.message}")
                }
            }
        } catch (t: Throwable) {
            Log.w("HomeViewModel", "fetchAndSeedIfEmpty failed: ${t.message}")
        }
    }

    // Helper to normalise and dedupe a list of interest strings
    private fun processInterestNames(raw: List<String>): List<String> {
        return raw.map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.SearchChanged -> {
                _uiState.update { it.copy(searchQuery = event.query) }
                applyFilters()
            }
            is HomeEvent.SkillSelected -> {
                _uiState.update { it.copy(selectedSkill = event.skill) }
                applyFilters()
            }
            is HomeEvent.ToggleFavorite -> {
                _uiState.update { s ->
                    val newSet = s.favorites.toMutableSet()
                    if (newSet.contains(event.adId)) newSet.remove(event.adId) else newSet.add(event.adId)
                    s.copy(favorites = newSet)
                }
            }
            is HomeEvent.ViewDetails -> {
                viewModelScope.launch { _effects.emit(HomeUiEffect.NavigateToDetails(event.adId)) }
            }
            HomeEvent.Refresh -> {
                // you can wire getAdsUseCase here if needed in future
                applyFilters()
                viewModelScope.launch { _effects.emit(HomeUiEffect.RefreshCompleted) }
            }
            HomeEvent.ClearError -> _uiState.update { it.copy(error = null) }
            is HomeEvent.UpdateLocation -> {
                // Update UI state quickly with coords and a precise coordinate label (persisted label preferred)
                _uiState.update {
                    it.copy(
                        currentLat = event.lat,
                        currentLng = event.lng,
                        locationLabel = formatLocationLabel(event.lat, event.lng)
                    )
                }

                // also cache coords synchronously (best-effort)
                try {
                    val ctx = AppContextProvider.getContext()
                    ctx?.let {
                        SharedPrefUtils.putString(it, PREF_LAST_LAT, event.lat.toString())
                        SharedPrefUtils.putString(it, PREF_LAST_LNG, event.lng.toString())
                    }
                } catch (_: Throwable) { /* ignore */ }

                // Decide whether to resolve human locality now using persisted prefs only
                val shouldResolve = shouldResolveLabel(event.lat, event.lng)
                if (shouldResolve) {
                    viewModelScope.launch {
                        try {
                            val loc = Location("HomeViewModel").apply {
                                latitude = event.lat
                                longitude = event.lng
                            }

                            val label = try {
                                locationProvider.getAreaFromLocation(loc)
                            } catch (t: Throwable) {
                                Log.w("HomeViewModel", "locationProvider geocode error: ${t.message}")
                                null
                            }

                            Log.d("HomeViewModel", "reverse geocode UpdateLocation result: $label")

                            if (!label.isNullOrBlank()) {
                                persistLabelSafely(label, event.lat, event.lng)
                                _uiState.update { s -> s.copy(locationLabel = label) }
                            } else {
                                Log.d("HomeViewModel", "No locality found on UpdateLocation for (${event.lat},${event.lng})")
                                // leave coordinate label shown for accuracy
                            }
                        } catch (t: Throwable) {
                            Log.w("HomeViewModel", "Reverse geocode failed on UpdateLocation: ${t.message}")
                        } finally {
                            applyFilters()
                        }
                    }
                } else {
                    applyFilters()
                }
            }
        }
    }

    fun fetchUsers(page: Int = 1, limit: Int = 10) {
        viewModelScope.launch {
            _usersState.update { it.copy(isLoading = true, error = null) }
            when (val result = getUsersUseCase.invoke(page, limit)) {
                is NetworkResult.Success -> {
                    val users = result.data ?: emptyList()
                    cachedUsers = users
                    applyFilters()
                    _usersState.update { it.copy(isLoading = false, error = null) }
                }
                is NetworkResult.Error -> {
                    cachedUsers = emptyList()
                    _usersState.update { it.copy(isLoading = false, users = emptyList(), error = result.message ?: "Failed to fetch users") }
                    _effects.tryEmit(HomeUiEffect.ShowError(result.message ?: "Failed to fetch users"))
                }
            }
        }
    }

    private fun applyFilters() {
        val ui = _uiState.value
        val knownSkillsLower = ui.skills.map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()

        val selected = ui.selectedSkill.trim()
        val selectedLower = selected.lowercase()

        val filtered = cachedUsers.filter { user ->
            val userInterestNamesLower = user.interests.mapNotNull { it.name?.trim()?.lowercase() }

            val skillOk = when {
                selected.equals("All", true) -> true
                selected.equals("Other", true) -> {
                    val hasAny = user.interestNamesLowerOrEmpty().isNotEmpty()
                    if (knownSkillsLower.isEmpty()) hasAny else userInterestNamesLower.any { it !in knownSkillsLower }
                }
                else -> {
                    val matchInterest = userInterestNamesLower.any { it.equals(selectedLower, true) }
                    val matchProfileDesc = user.profileDescription?.contains(selected, true) ?: false
                    matchInterest || matchProfileDesc
                }
            }

            val searchOk = ui.searchQuery.trim().takeIf { it.isNotEmpty() }?.let { q ->
                val ql = q.lowercase()
                (user.fullName?.contains(q, true) == true) ||
                        (user.profileDescription?.contains(q, true) == true) ||
                        (user.interests.any { it.name?.contains(q, true) == true })
            } ?: true

            skillOk && searchOk
        }

        val lat = ui.currentLat.takeIf { it != AppConstants.DEFAULT_LAT }
        val lng = ui.currentLng.takeIf { it != AppConstants.DEFAULT_LNG }

        val withDistance = filtered.map { u ->
            val dKm = if (lat != null && lng != null && u.lat != null && u.lng != null) {
                DistanceUtils.distanceKm(lat, lng, u.lat, u.lng)
            } else null
            UserWithDistance(user = u, distanceKm = dKm)
        }.sortedBy { it.distanceKm ?: Double.MAX_VALUE }

        _usersState.update { it.copy(users = withDistance) }
    }

    // --------- Helpers for persistence/decisions ----------

    private fun shouldResolveLabel(lat: Double, lng: Double): Boolean {
        // Use persisted prefs only to decide.
        val ctx = AppContextProvider.getContext()
        val persistedLabel = try { ctx?.let { SharedPrefUtils.getString(it, PREF_LOCATION_LABEL) } } catch (_: Throwable) { null }

        // If no persisted label, we should resolve.
        if (persistedLabel.isNullOrBlank()) return true

        // If we have persisted coords, compare distance. If moved significantly, re-resolve.
        val persistedLat = try { ctx?.let { SharedPrefUtils.getString(it, PREF_LAST_LAT)?.toDoubleOrNull() } } catch (_: Throwable) { null }
        val persistedLng = try { ctx?.let { SharedPrefUtils.getString(it, PREF_LAST_LNG)?.toDoubleOrNull() } } catch (_: Throwable) { null }

        if (persistedLat == null || persistedLng == null) {
            // missing persisted coords -> attempt resolve once
            return true
        }

        return try {
            val meters = DistanceUtils.distanceKm(lat, lng, persistedLat, persistedLng)
            meters > DISTANCE_THRESHOLD_METERS
        } catch (_: Throwable) {
            true
        }
    }

    private fun persistLabelSafely(label: String, lat: Double, lng: Double) {
        try {
            val ctx = AppContextProvider.getContext()
            if (ctx != null) {
                // Persist the label and associated coords so future checks can use prefs only
                SharedPrefUtils.putString(ctx, PREF_LOCATION_LABEL, label)
                SharedPrefUtils.putString(ctx, PREF_LAST_LAT, lat.toString())
                SharedPrefUtils.putString(ctx, PREF_LAST_LNG, lng.toString())
            }
        } catch (t: Throwable) {
            Log.w("HomeViewModel", "Failed to persist location label: ${t.message}")
        }
    }
}

// extension helper used in filtering above to keep code compact
private fun User.interestNamesLowerOrEmpty(): List<String> =
    this.interests.mapNotNull { it.name?.trim()?.lowercase() }
