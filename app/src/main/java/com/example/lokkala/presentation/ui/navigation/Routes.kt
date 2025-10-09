package com.example.lokkala.presentation.ui.navigation

sealed class Route(val path: String) {
    data object Splash : Route("splash")
    data object Login  : Route("login")
    data object ProfileSetup : Route("profile_setup")
    data object Home   : Route("home") // placeholder target after login

    data object CountryPicker : Route("country_picker")
}