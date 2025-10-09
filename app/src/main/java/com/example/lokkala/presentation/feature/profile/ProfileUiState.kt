package com.example.lokkala.presentation.feature.profile

import android.net.Uri
import com.example.lokkala.util.AppConstants

data class ProfileUiState(
    val name: String = "",
    val profilePicUri: Uri? = null,
    val interests: List<String> = emptyList(),
    val availableInterests: List<String> = AppConstants.DEFAULT_INTERESTS,
    val customInterest: String = "",
    val customInterestError: String? = null,
    val loading: Boolean = false,
    val error: String? = null
)

sealed interface ProfileEvent {
    data class NameChanged(val value: String): ProfileEvent
    data class ProfilePicChanged(val uri: Uri?): ProfileEvent
    data class InterestToggled(val interest: String): ProfileEvent
    data class CustomInterestChanged(val value: String): ProfileEvent
    data object AddCustomInterest: ProfileEvent
    data object Submit: ProfileEvent
    data object ClearError: ProfileEvent
}
