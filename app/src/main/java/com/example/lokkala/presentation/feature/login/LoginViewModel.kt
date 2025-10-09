package com.example.lokkala.presentation.feature.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lokkala.domain.usecase.RequestOtpUseCase
import com.example.lokkala.domain.usecase.VerifyOtpUseCase
import com.example.lokkala.data.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val requestOtp: RequestOtpUseCase,
    private val verifyOtp: VerifyOtpUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())

    val uiState: StateFlow<LoginUiState> = _state

    fun onEvent(e: LoginEvent) {
        when (e) {
            is LoginEvent.CountryChanged -> {

                _state.value = _state.value.copy(countryCode = e.cc)}
            is LoginEvent.PhoneChanged   -> _state.value = _state.value.copy(phone = e.phone)
            is LoginEvent.OtpChanged     -> _state.value = _state.value.copy(otp = e.otp)
            LoginEvent.ClearError        -> _state.value = _state.value.copy(error = null)
            LoginEvent.RequestOtp        -> doRequestOtp()
            LoginEvent.VerifyOtp         -> doVerifyOtp()
        }
    }

    private fun doRequestOtp() = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null, successMessage = null)
        when (val res = requestOtp(_state.value.countryCode, _state.value.phone)) {
            is NetworkResult.Success -> _state.value = _state.value.copy(
                loading = false,
                step = LoginUiState.Step.EnterOtp,
                successMessage = res.data // Now res.data is String from API
            )
            is NetworkResult.Error -> _state.value = _state.value.copy(
                loading = false,
                error = res.message ?: "Failed"
            )
        }
    }

    private fun doVerifyOtp() = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        when (val res = verifyOtp(_state.value.countryCode, _state.value.phone, _state.value.otp)) {
            is NetworkResult.Success ->
                _state.value = _state.value.copy(loading = false, loginSuccess = true)
            is NetworkResult.Error ->
                _state.value = _state.value.copy(loading = false, error = res.message ?: "Failed")
        }
    }
}