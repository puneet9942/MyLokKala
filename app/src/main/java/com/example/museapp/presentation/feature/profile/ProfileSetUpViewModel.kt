package com.example.museapp.presentation.feature.profile

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.museapp.data.auth.dto.VerifyOtpData
import com.example.museapp.data.local.dao.InterestsDao
import com.example.museapp.data.remote.dto.ProfileRequestDto
import com.example.museapp.data.remote.mapper.toProfileRequestDto
import com.example.museapp.data.util.NetworkResult
import com.example.museapp.domain.model.User
import com.example.museapp.domain.usecase.UpdateProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ProfileSetupViewModel
 *
 * - Fetches availableInterests from Room via InterestsDao (Flow) -- same as your old VM.
 * - Keeps submitProfile() and UpdateProfileUseCase call (same behavior as updated VM).
 * - No UI/validation changes.
 */
@HiltViewModel
class ProfileSetupViewModel @Inject constructor(
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val interestDao: InterestsDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    // expose available interests from Room
    private val _availableInterests = MutableStateFlow<List<String>>(emptyList())
    val availableInterests: StateFlow<List<String>> = _availableInterests.asStateFlow()

    init {
        // Collect interests from Room DAO flow. This mirrors your old VM:
        // interestDao.getAllInterestsFlow() -> Flow<List<InterestEntity>>
        viewModelScope.launch(ioDispatcher) {
            try {
                interestDao.getAllInterestsFlow()
                    .map { list -> list.mapNotNull { it.name } } // adapt field 'name' if needed
                    .collect { names ->
                        _availableInterests.value = names
                    }
            } catch (_: Throwable) {
                // swallow - keep empty list on error
            }
        }
    }

    fun onEvent(event: ProfileEvent) {
        val cur = _state.value
        when (event) {
            is ProfileEvent.NameChanged -> _state.value = cur.copy(name = event.name ?: "")
            is ProfileEvent.DobChanged -> _state.value = cur.copy(dob = event.dob ?: "")
            is ProfileEvent.GenderChanged -> _state.value = cur.copy(gender = event.gender)
            is ProfileEvent.DescriptionChanged -> _state.value = cur.copy(description = event.desc ?: "")
            is ProfileEvent.BiographyChanged -> _state.value = cur.copy(biography = event.bio ?: "")

            is ProfileEvent.MobileChanged -> _state.value = cur.copy(mobile = event.mobile ?: "")

            is ProfileEvent.InterestToggled -> {
                val m = cur.interests.toMutableList()
                if (m.contains(event.interest)) {
                    // user is unselecting -> allow always
                    m.remove(event.interest)
                    _state.value = cur.copy(interests = m)
                } else {
                    // user is selecting -> enforce max 4
                    if (m.size >= 4) {
                        // set error on state so your Screen's LaunchedEffect will show snackbar
                        _state.value = cur.copy(error = "You can select up to 4 interests")
                    } else {
                        m.add(event.interest)
                        _state.value = cur.copy(interests = m)
                    }
                }
            }

            is ProfileEvent.CustomInterestChanged -> _state.value = cur.copy(customInterest = event.value)
            is ProfileEvent.AddCustomInterest -> {
                val custom = (cur.customInterest ?: "").trim()
                if (custom.isNotEmpty()) {
                    val m = cur.interests.toMutableList()
                    // enforce max 4 before adding custom
                    if (m.size >= 4) {
                        _state.value = cur.copy(error = "You can select up to 4 interests")
                    } else {
                        // add to selected interests
                        m.add(custom)

                        // also add to availableInterests so the chip bar shows the new custom chip
                        val avail = _availableInterests.value.toMutableList()
                        // avoid duplicates (case-insensitive)
                        val lower = avail.map { it.lowercase() }
                        if (!lower.contains(custom.lowercase())) {
                            avail.add(custom)
                            _availableInterests.value = avail
                        }

                        _state.value = cur.copy(interests = m, customInterest = null)
                    }
                }
            }

            // Photos / videos handling (keeps consistent semantics)
            is ProfileEvent.ProfilePicChanged -> {
                val uri = toUriOrNull(event.uri)
                _state.value = cur.copy(profilePicUri = uri)
            }
            is ProfileEvent.AddPhotos -> {
                val newUris = event.uris.mapNotNull { toUriOrNull(it) }
                val merged = cur.photos.toMutableList()
                newUris.forEach { if (!merged.contains(it)) merged.add(it) }
                _state.value = cur.copy(photos = merged)
            }
            is ProfileEvent.AddVideos -> {
                val newUris = event.uris.mapNotNull { toUriOrNull(it) }
                val merged = cur.videos.toMutableList()
                newUris.forEach { if (!merged.contains(it)) merged.add(it) }
                _state.value = cur.copy(videos = merged)
            }

            is ProfileEvent.RemovePhoto -> {
                val target = toUriOrNull(event.uri)
                val remaining = cur.photos.filterNot { it == target }
                _state.value = cur.copy(photos = remaining)
            }
            is ProfileEvent.RemoveVideo -> {
                val target = toUriOrNull(event.uri)
                val remaining = cur.videos.filterNot { it == target }
                _state.value = cur.copy(videos = remaining)
            }

            is ProfileEvent.InstaChanged -> _state.value = cur.copy(instaId = event.id)
            is ProfileEvent.TwitterChanged -> _state.value = cur.copy(twitterId = event.id)
            is ProfileEvent.YoutubeChanged -> _state.value = cur.copy(youtubeId = event.id)
            is ProfileEvent.FacebookChanged -> _state.value = cur.copy(facebookId = event.id)

            ProfileEvent.NextStep -> _state.value = cur.copy(step = (cur.step + 1).coerceAtMost(ProfileUiState.MAX_STEP))
            ProfileEvent.PrevStep -> _state.value = cur.copy(step = (cur.step - 1).coerceAtLeast(1))
            ProfileEvent.ClearError -> _state.value = cur.copy(error = null)

            // Ensure Submit triggers API call
            ProfileEvent.Submit -> submitProfile()

            // keep placeholders / other events untouched
            is ProfileEvent.MaxPriceChanged -> {
                _state.value = cur.copy(maxPrice = event.value ?: "")
            }

            is ProfileEvent.MinPriceChanged -> {
                _state.value = cur.copy(minPrice = event.value ?: "")
            }

            is ProfileEvent.SetEventManager -> {
                _state.value = cur.copy(isEventManager = event.isEventManager)
            }

            is ProfileEvent.StandardPriceChanged -> {
                _state.value = cur.copy(standardPrice = event.value ?: "")
            }

            is ProfileEvent.TravelRadiusChanged -> {
                _state.value = cur.copy(travelRadiusKm = event.value ?: "")
            }

            is ProfileEvent.PricingTypeSelected -> {
                _state.value = cur.copy(pricingType = event.type ?: cur.pricingType)
            }
        }
    }

    fun prefillFromVerify(verify: VerifyOtpData?) {
        if (verify == null) return
        val u = verify.user ?: return
        val cur = _state.value
        _state.value = cur.copy(
            name = u.fullName ?: cur.name,
            dob = u.dob ?: cur.dob,
            gender = u.gender ?: cur.gender,
            description = u.profileDescription ?: cur.description,
            biography = u.bio ?: cur.biography,
            profilePicUri = cur.profilePicUri // do not auto-change Uri
        )
    }

    /**
     * Submit profile: validate, construct DTO (via extension if present), call use-case,
     * and update state based on NetworkResult.
     */
    private fun submitProfile() {
        val cur = _state.value

        // client-side validation (keeps previous messages)
        if (cur.name.isBlank()) {
            _state.value = cur.copy(loading = false, error = "Please enter full name")
            Log.d("ProfileVM", "submitProfile: validation failed - name blank")
            return
        }

        viewModelScope.launch(ioDispatcher) {
            _state.value = cur.copy(loading = true, error = null)
            try {
                Log.d("ProfileVM", "submitProfile: building DTO from UI state")
                val dto = cur.toProfileRequestDto() // keep your existing extension

                Log.d("ProfileVM", "submitProfile: calling updateProfileUseCase")
                val photosUris = cur.photos.mapNotNull { it }
                val videosUris = cur.videos.mapNotNull { it }

                val result = try {
                    updateProfileUseCase(dto, cur.profilePicUri, photosUris, videosUris)
                } catch (e: Throwable) {
                    Log.e("ProfileVM", "submitProfile: updateProfileUseCase threw", e)
                    _state.value = _state.value.copy(loading = false, error = e.message ?: "Network error")
                    return@launch
                }

                Log.d("ProfileVM", "submitProfile: updateProfileUseCase returned: $result")

                when (result) {
                    is NetworkResult.Success -> {
                        _state.value = _state.value.copy(loading = false, error = null)
                    }
                    is NetworkResult.Error -> {
                        _state.value = _state.value.copy(loading = false, error = result.message ?: "Profile update failed")
                    }
                }
            } catch (t: Throwable) {
                Log.e("ProfileVM", "submitProfile: unexpected", t)
                _state.value = _state.value.copy(loading = false, error = t.message ?: "Unexpected error")
            }
        }
    }

    // ---------- Helpers ----------
    private fun toUriOrNull(input: Any?): Uri? {
        return when (input) {
            null -> null
            is Uri -> input
            is String -> try { Uri.parse(input) } catch (_: Exception) { null }
            else -> null
        }
    }
}
