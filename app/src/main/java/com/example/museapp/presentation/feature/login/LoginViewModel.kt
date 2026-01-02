package com.example.museapp.presentation.feature.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.museapp.data.store.TokenStore
import com.example.museapp.domain.usecase.RequestOtpUseCase
import com.example.museapp.domain.usecase.VerifyOtpUseCase
import com.example.museapp.data.util.AppError
import com.example.museapp.data.util.AppErrorBroadcaster
import com.example.museapp.data.util.NetworkResult
import com.example.museapp.data.util.parseApiErrorBody
import com.example.museapp.util.ValidationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val requestOtp: RequestOtpUseCase,
    private val verifyOtp: VerifyOtpUseCase,
    private val tokenStore: TokenStore,
    private val appErrorBroadcaster: AppErrorBroadcaster? = null
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _state

    private val _events = MutableSharedFlow<LoginViewEvent>(replay = 0)
    val events = _events.asSharedFlow()

    sealed class LoginViewEvent {
        object LoggedIn : LoginViewEvent()
    }

    fun onEvent(e: LoginEvent) {
        when (e) {
            is LoginEvent.CountryChanged -> {
                val newIso = ValidationUtils.getCountryIsoFromCode(e.cc) ?: _state.value.iso
                _state.value = _state.value.copy(countryCode = e.cc, iso = newIso)
            }
            is LoginEvent.PhoneChanged -> _state.value = _state.value.copy(phone = e.phone)
            is LoginEvent.OtpChanged -> _state.value = _state.value.copy(otp = e.otp, error = null)
            LoginEvent.ClearError -> _state.value = _state.value.copy(error = null)
            LoginEvent.RequestOtp -> doRequestOtp()
            LoginEvent.VerifyOtp -> doVerifyOtp()
        }
    }

    private fun doRequestOtp() = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null, successMessage = null)

        val countryCode = _state.value.countryCode.trim()
        val phone = _state.value.phone.trim()

        when (val res = requestOtp(countryCode, phone)) {
            is NetworkResult.Success -> _state.value = _state.value.copy(
                loading = false,
                step = LoginUiState.Step.EnterOtp,
                successMessage = res.data
            )

            is NetworkResult.Error -> {
                val parsed = parseApiErrorBody(res.message)
                val display = parsed?.message ?: res.message ?: "Failed to request OTP"
                _state.value = _state.value.copy(loading = false, error = display)
            }
        }
    }

    private fun doVerifyOtp() = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)

        val countryCode = _state.value.countryCode.trim()
        val phone = _state.value.phone.trim()
        val otp = _state.value.otp.trim()

        when (val res = verifyOtp(countryCode, phone, otp)) {
            is NetworkResult.Success -> {
                Log.d("LoginViewModel", "doVerifyOtp: success -> setting loginSuccess=true")
                _state.value = _state.value.copy(loading = false, loginSuccess = true)
                val token = res.data.access_token ?: ""
                token?.let { tokenStore.setToken(it) }
                viewModelScope.launch { _events.emit(LoginViewEvent.LoggedIn) }
            }

            is NetworkResult.Error -> {
                // show API-provided message when available
                val parsed = parseApiErrorBody(res.message)
                val apiMessage = parsed?.message ?: res.message
                val display = apiMessage?.takeIf { it.isNotBlank() } ?: "Failed to verify OTP"

                _state.value = _state.value.copy(loading = false, error = display)

                // Optionally broadcast inline field error if you want global routing of inline messages:
                // appErrorBroadcaster?.broadcast(AppError.InlineFieldError(field = "otp", message = display))

                Log.d("LoginViewModel", "verifyOtp failed code=${res.code} messageFromApi=${apiMessage ?: "null"} throwable=${res.throwable}")
            }
        }
    }
}
