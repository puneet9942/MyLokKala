package com.example.museapp.presentation.ui.navigation

sealed class Route(val path: String) {
    object Splash : Route("splash")
    object Login : Route("login")

    object OtpVerification : Route("otp_verification/{phone}") {
        fun createRoute(phone: String) = "otp_verification/${phone}"
    }
    object ProfileSetup : Route("profile_setup")
    object Home : Route("home")

    object CountryPicker : Route("country_picker")
    object Saved : Route("saved")

    object Details : Route("details/{adId}") {
        fun createRoute(adId: String) = "details/$adId"
    }
}