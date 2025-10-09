package com.example.lokkala.presentation.feature.profile

import androidx.lifecycle.ViewModel
import com.example.lokkala.util.InterestValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class ProfileSetupViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state

    fun onEvent(event: ProfileEvent) {
        when(event) {
            is ProfileEvent.NameChanged -> _state.value = _state.value.copy(name = event.value)
            is ProfileEvent.ProfilePicChanged -> _state.value = _state.value.copy(profilePicUri = event.uri)
            is ProfileEvent.InterestToggled -> {
                val interests = _state.value.interests.toMutableList()
                if (interests.contains(event.interest)) interests.remove(event.interest)
                else interests.add(event.interest)
                _state.value = _state.value.copy(interests = interests)
            }
            is ProfileEvent.CustomInterestChanged -> {
                _state.value = _state.value.copy(
                    customInterest = event.value,
                    customInterestError = null // clear error on change
                )
            }
            ProfileEvent.AddCustomInterest -> {
                val custom = _state.value.customInterest.trim()
                if (InterestValidator.isValidInterest(custom)) {
                    _state.value = _state.value.copy(
                        interests = _state.value.interests + custom,
                        customInterest = "",
                        customInterestError = null
                    )
                } else {
                    _state.value = _state.value.copy(
                        customInterestError = "Please enter a valid, safe skill or interest (no profanity or gibberish)"
                    )
                }
            }
            ProfileEvent.Submit -> {
                // Save or send profile (extend here)
            }
            ProfileEvent.ClearError -> _state.value = _state.value.copy(error = null)
        }
    }
}
