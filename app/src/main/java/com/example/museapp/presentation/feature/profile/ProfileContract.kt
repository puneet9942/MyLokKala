package com.example.museapp.presentation.feature.profile

import com.example.museapp.data.remote.dto.ProfileResponseDto
import com.example.museapp.domain.model.User

sealed interface ProfileEvents {
    object LoadProfile : ProfileEvents
    object RefreshProfile : ProfileEvents
}

sealed interface ProfileUiStates {
    object Idle : ProfileUiStates
    object Loading : ProfileUiStates
    data class ShowingCached(val user: User, val rawDto: ProfileResponseDto? = null) : ProfileUiStates
    data class ShowingRemote(val user: User, val rawDto: ProfileResponseDto? = null) : ProfileUiStates
    data class Error(val message: String) : ProfileUiStates
}
