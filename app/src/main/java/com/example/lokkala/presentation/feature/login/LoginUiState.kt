package com.example.lokkala.presentation.feature.login

data class LoginUiState(
    val countryCode: String = "+91",
    val iso: String =  "IN",
    val phone: String = "",
    val otp: String = "",
    val step: Step = Step.EnterPhone,
    val loading: Boolean = false,
    val error: String? = null,
    val loginSuccess: Boolean = false,
    val successMessage: String? = null
) {
    enum class Step { EnterPhone, EnterOtp }
}

sealed interface LoginEvent {
    data class CountryChanged(val cc: String): LoginEvent
    data class PhoneChanged(val phone: String): LoginEvent
    data class OtpChanged(val otp: String): LoginEvent
    data object RequestOtp : LoginEvent
    data object VerifyOtp : LoginEvent
    data object ClearError : LoginEvent
}