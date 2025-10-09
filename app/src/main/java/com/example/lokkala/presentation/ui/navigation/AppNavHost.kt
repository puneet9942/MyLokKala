package com.example.lokkala.presentation.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.lokkala.presentation.feature.login.LoginEvent
import com.example.lokkala.presentation.feature.login.LoginViewModel
import com.example.lokkala.presentation.feature.profile.ProfileSetupScreen
import com.example.lokkala.presentation.feature.profile.ProfileSetupViewModel
import com.example.lokkala.presentation.ui.screens.CountryPickerScreen
import com.example.lokkala.presentation.ui.screens.HomeScreen
import com.example.lokkala.presentation.ui.screens.LoginScreen
import com.example.lokkala.presentation.ui.screens.SplashScreen

@Composable
fun AppNavHost() {
    val nav = rememberNavController()
    val vm: LoginViewModel = hiltViewModel()
    val profileVm: ProfileSetupViewModel = hiltViewModel()
    NavHost(navController = nav, startDestination = Route.Splash.path) {
        composable(Route.Splash.path) {
            SplashScreen(onFinish = {
                nav.navigate(Route.Login.path) {
                    popUpTo(Route.Splash.path) { inclusive = true }
                }
            })
        }
        composable(Route.Login.path) {
            LoginScreen(
                state = vm.uiState,
                onEvent = vm::onEvent,
                onCountryPicker ={nav.navigate(Route.CountryPicker.path)},
                onLoggedIn = { /* navigate to Home later */
                    nav.navigate(Route.ProfileSetup.path) {
                        popUpTo(Route.Login.path) { inclusive = true }
                    }
                }

            )
        }
        composable(Route.ProfileSetup.path) {
            ProfileSetupScreen(
                state = profileVm.state,
                onEvent = profileVm::onEvent,
                onContinue = {
                    nav.navigate(Route.Home.path) {
                        popUpTo(Route.ProfileSetup.path) { inclusive = true }
                    }
                }
            )
        }
        composable(Route.Home.path) {
            HomeScreen()
        }

        composable(Route.CountryPicker.path) {
            CountryPickerScreen(
                onBack = { nav.popBackStack() },
                onCountrySelected = { country ->
                    vm.onEvent(LoginEvent.CountryChanged(country.code))  // <-- This updates the code!
                    nav.popBackStack()
                }
            )
        }

        // Home route can be added when you’re ready
    }
}