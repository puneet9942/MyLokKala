package com.example.museapp.presentation.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.museapp.presentation.feature.login.LoginEvent
import com.example.museapp.presentation.ui.screens.LoginScreen
import com.example.museapp.presentation.feature.login.LoginViewModel
import com.example.museapp.presentation.ui.navigation.Route
import com.example.museapp.presentation.ui.screens.CountryPickerScreen

@Composable
fun LoginHost(
    nav: androidx.navigation.NavController,
    loginViewModel: LoginViewModel = hiltViewModel()
) {
    val state by loginViewModel.uiState.collectAsState()
    var showCountryPicker by remember { mutableStateOf(false) }

    LoginScreen(
        state = loginViewModel.uiState,
        uiState = state,
        onPhoneChanged = { phone -> loginViewModel.onEvent(LoginEvent.PhoneChanged(phone)) },
        onCountryPicker = { showCountryPicker = true },
        onContinue = { loginViewModel.onEvent(LoginEvent.RequestOtp) },
        onLoggedIn = {
            // ✅ Now we actually navigate
            nav.navigate(Route.ProfileSetup.path) {
                popUpTo(Route.Login.path) { inclusive = true }
            }
        },
        onEvent = loginViewModel::onEvent
    )

    if (showCountryPicker) {
        Surface(
            modifier = Modifier.fillMaxSize().zIndex(2f),
            color = MaterialTheme.colorScheme.background
        ) {
            CountryPickerScreen(
                onBack = { showCountryPicker = false },
                onCountrySelected = { country ->
                    loginViewModel.onEvent(LoginEvent.CountryChanged(country.code))
                    showCountryPicker = false
                }
            )
        }
    }
}