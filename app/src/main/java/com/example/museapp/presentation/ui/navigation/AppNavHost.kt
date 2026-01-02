package com.example.museapp.presentation.ui.navigation

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.museapp.data.auth.dto.VerifyOtpData
import com.example.museapp.data.store.TokenStore.Companion.KEY_ACCESS_TOKEN
import com.example.museapp.presentation.feature.feedback.FeedbackScreen
import com.example.museapp.presentation.feature.home.HomeViewModel
import com.example.museapp.presentation.feature.login.LoginEvent
import com.example.museapp.presentation.feature.login.LoginViewModel
import com.example.museapp.presentation.feature.profile.ProfileDisplayScreen
import com.example.museapp.presentation.ui.screens.ProfileSetupScreen

import com.example.museapp.presentation.feature.profile.ProfileSetupViewModel


import com.example.museapp.presentation.ui.screens.*
import com.example.museapp.util.AppConstants.SPLASH_SCREEN_DURATION_MILLISECONDS
import com.example.museapp.util.HideSystemBarsDuring
import kotlinx.coroutines.delay
import kotlin.reflect.full.memberProperties

@SuppressLint("UnrememberedGetBackStackEntry")
@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val startDestination = Route.Splash.path

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {

        composable(Route.Splash.path) {
            HideSystemBarsDuring(active = true)
            SplashScreen()
            val context = LocalContext.current
            LaunchedEffect(Unit) {
                // Read KEY_ACCESS_TOKEN from SharedPreferences
                val prefs = context.getSharedPreferences("museapp_auth_prefs", Context.MODE_PRIVATE)
                val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null)
                delay(SPLASH_SCREEN_DURATION_MILLISECONDS)
                if (!accessToken.isNullOrEmpty()) {
                    // If token exists, skip login/profile setup and go to Home
                    navController.navigate(Route.Home.path) {
                        popUpTo(Route.Splash.path) { inclusive = true }
                    }
                } else {
                    // Else show login as usual
                    navController.navigate(Route.Login.path) {
                        popUpTo(Route.Splash.path) { inclusive = true }
                    }
                }
            }
        }

        composable(ProfileDestinations.FEEDBACK) {
            FeedbackScreen()
        }

        // Login Screen
        composable(Route.Login.path) { backEntry ->
            // get Login VM from this back entry (safe)
            val vm: LoginViewModel = hiltViewModel(backEntry)

            LoginScreen(
                state = vm.uiState,
                onPhoneChanged = { phone -> vm.onEvent(LoginEvent.PhoneChanged(phone)) },
                onCountryPicker = { navController.navigate(Route.CountryPicker.path) },
                onContinue = { /* optionally used */ },
                onLoggedIn = {
                    // BEFORE navigating, try to extract VerifyOtpData from the LoginViewModel (defensive)
                    try {
                        val loginEntry = navController.getBackStackEntry(Route.Login.path)

                        // Try common property names on the VM first
                        val candidates = listOf(
                            "verifyOtpData",
                            "verifyResponse",
                            "verifyData",
                            "verifyResponseData",
                            "otpVerifyResponse",
                            "verify"
                        )

                        var verifyToPass: VerifyOtpData? = null

                        // 1) check named properties
                        candidates.forEach { name ->
                            if (verifyToPass != null) return@forEach
                            runCatching {
                                val prop = vm::class.memberProperties.firstOrNull { it.name.equals(name, ignoreCase = true) }
                                val value = prop?.getter?.call(vm)
                                when (value) {
                                    is VerifyOtpData -> verifyToPass = value
                                    is kotlinx.coroutines.flow.StateFlow<*> -> {
                                        val inner = value.value
                                        if (inner is VerifyOtpData) verifyToPass = inner
                                    }
                                    is androidx.lifecycle.LiveData<*> -> {
                                        val inner = value.value
                                        if (inner is VerifyOtpData) verifyToPass = inner
                                    }
                                }
                            }
                        }

                        // 2) fallback: probe vm.uiState.value for nested VerifyOtpData
                        if (verifyToPass == null) {
                            runCatching {
                                val uiVal = runCatching { vm.uiState.value }.getOrNull()
                                uiVal?.let { st ->
                                    st::class.memberProperties.forEach { p ->
                                        if (verifyToPass != null) return@forEach
                                        val v = runCatching { p.getter.call(st) }.getOrNull()
                                        if (v is VerifyOtpData) verifyToPass = v
                                    }
                                }
                            }
                        }

                        // 3) final fallback: scan all properties on the VM
                        if (verifyToPass == null) {
                            vm::class.memberProperties.forEach { prop ->
                                if (verifyToPass != null) return@forEach
                                runCatching {
                                    val v = prop.getter.call(vm)
                                    when (v) {
                                        is VerifyOtpData -> verifyToPass = v
                                        is kotlinx.coroutines.flow.StateFlow<*> -> {
                                            val inner = v.value
                                            if (inner is VerifyOtpData) verifyToPass = inner
                                        }
                                        is androidx.lifecycle.LiveData<*> -> {
                                            val inner = v.value
                                            if (inner is VerifyOtpData) verifyToPass = inner
                                        }
                                    }
                                }
                            }
                        }

                        // If found, write to savedStateHandle on the Login back entry under key "verifyData"
                        verifyToPass?.let { loginEntry.savedStateHandle["verifyData"] = it }

                    } catch (t: Throwable) {
                        // swallow - navigation should still happen even if saving fails
                        android.util.Log.w("AppNavHost", "failed to persist verifyData into savedStateHandle: ${t.message}")
                    } finally {
                        // proceed to navigate
                        navController.navigate(Route.ProfileSetup.path) {
                            popUpTo(Route.Login.path) { inclusive = true }
                        }
                    }
                },
                onEvent = vm::onEvent
            )
        }

        // Profile Setup Screen
        composable(Route.ProfileSetup.path) {
            // Obtain Profile VM
            val profileVm: ProfileSetupViewModel = hiltViewModel()

            // safely get the login back entry (may be null)
            val loginBackEntry = runCatching { navController.getBackStackEntry(Route.Login.path) }.getOrNull()

            // If loginBackEntry exists, obtain its LoginViewModel in composition
            val loginVm: LoginViewModel? = if (loginBackEntry != null) {
                hiltViewModel(loginBackEntry)
            } else null

            // Compose-time remember block to extract VerifyOtpData:
            val verifyData: VerifyOtpData? = remember(loginVm, loginBackEntry) {
                // 1) first try savedStateHandle on login back entry (explicit transport)
                val saved = runCatching { loginBackEntry?.savedStateHandle?.get<VerifyOtpData>("verifyData") }.getOrNull()
                if (saved != null) return@remember saved

                // 2) if no saved payload, try probing the login VM (if present)
                if (loginVm == null) return@remember null

                // candidate names to try first
                val candidatesDirect = listOf("verifyOtpData", "verifyResponse", "verifyResponseData", "verify", "verifyOtp", "verifyData", "otpVerifyResponse")
                candidatesDirect.forEach { name ->
                    try {
                        val prop = loginVm::class.memberProperties.firstOrNull { it.name.equals(name, ignoreCase = true) }
                        val value = prop?.getter?.call(loginVm)
                        if (value is VerifyOtpData) return@remember value
                        if (value is kotlinx.coroutines.flow.StateFlow<*>) {
                            val inner = value.value
                            if (inner is VerifyOtpData) return@remember inner
                        }
                        if (value is androidx.lifecycle.LiveData<*>) {
                            val inner = value.value
                            if (inner is VerifyOtpData) return@remember inner
                        }
                    } catch (_: Throwable) { /* ignore and continue */ }
                }

                // 3) final fallback: scan all properties
                loginVm::class.memberProperties.forEach { prop ->
                    try {
                        val v = prop.getter.call(loginVm)
                        if (v is VerifyOtpData) return@remember v
                        if (v is kotlinx.coroutines.flow.StateFlow<*>) {
                            val inner = v.value
                            if (inner is VerifyOtpData) return@remember inner
                        }
                        if (v is androidx.lifecycle.LiveData<*>) {
                            val inner = v.value
                            if (inner is VerifyOtpData) return@remember inner
                        }
                    } catch (_: Throwable) { /* ignore */ }
                }

                // 4) also attempt to inspect vm.uiState.value for nested VerifyOtpData
                runCatching {
                    val uiStateVal = runCatching { loginVm.uiState.value }.getOrNull()
                    uiStateVal?.let { st ->
                        st::class.memberProperties.forEach { p ->
                            try {
                                val valP = p.getter.call(st)
                                if (valP is VerifyOtpData) return@remember valP
                            } catch (_: Throwable) { }
                        }
                    }
                }

                null
            }
            android.util.Log.d("PROFILE_PREFILL", "verifyData present? = ${verifyData != null} ; verify user: ${verifyData?.user}")

            ProfileSetupScreen(
                viewModel = profileVm,
                state = profileVm.state,
                onEvent = profileVm::onEvent,
                initialVerifyData = verifyData,
                onContinue = {
                    navController.navigate(Route.Home.path) {
                        popUpTo(Route.ProfileSetup.path) { inclusive = true }
                    }
                }
            )
        }

        // Home Screen
        composable(Route.Home.path) { backStackEntry ->
            val homeVm: HomeViewModel = hiltViewModel(backStackEntry)
            HomeScreen(
                homeViewModel = homeVm,
                rootNavController = navController
            )
        }

        // Saved Screen (Saved People / Favorites)
        composable(Route.Saved.path) {
            SavedScreen(
                onNavigateToDetails = { userId ->
                    // If you have a profile detail route that accepts a userId, use it here.
                    // Kept as PROFILE_FULL placeholder to avoid changing other parts of the app.
                    navController.navigate(ProfileDestinations.PROFILE_FULL)
                }
            )
        }

        // Country Picker Screen
        composable(Route.CountryPicker.path) { backEntry ->
            val loginBackStackEntry = runCatching { navController.getBackStackEntry(Route.Login.path) }.getOrNull()
            val loginVm: LoginViewModel? = if (loginBackStackEntry != null) hiltViewModel(loginBackStackEntry) else null
            CountryPickerScreen(
                onBack = { navController.popBackStack() },
                onCountrySelected = { country ->
                    loginVm?.onEvent(LoginEvent.CountryChanged(country.code))
                    navController.popBackStack()
                }
            )
        }

        // Create Ad Screen (if present in project)
        composable("create_ad") {
            CreateAdScreen(onBack = { navController.popBackStack() })
        }

        // Profile fullscreens / placeholders (kept as-is)
        composable(ProfileDestinations.PROFILE_FULL) {
            ProfileScreen(navController = navController, onLogout = {
                navController.navigate(Route.Login.path) {
                    popUpTo(0) { inclusive = true }
                }
            })
        }

        composable(ProfileDestinations.PROFILE_DETAIL) {
            //ProfileFullscreenScreen(onBack = { navController.popBackStack() })
            ProfileDisplayScreen()
        }
        composable(ProfileDestinations.PROFILE_SUBSCRIPTIONS) { DummyFullScreen(title = "My subscriptions") }
        composable(ProfileDestinations.SUBSCRIBE_PRO) { DummyFullScreen(title = "Subscribe to Pro") }
        composable(ProfileDestinations.REFER_APP) { DummyFullScreen(title = "Refer the App") }
       // composable(ProfileDestinations.FEEDBACK) { DummyFullScreen(title = "Send Feedback") }
        composable(ProfileDestinations.RATE_APP) { DummyFullScreen(title = "Rate App") }
        composable(ProfileDestinations.ABOUT_US) { DummyFullScreen(title = "About Us") }
        composable(ProfileDestinations.TERMS) { DummyFullScreen(title = "Terms & Conditions") }
        composable(ProfileDestinations.PRIVACY) { DummyFullScreen(title = "Privacy Policy") }
        composable(ProfileDestinations.NOTIFICATION_PREFS) { DummyFullScreen(title = "Notification Preferences") }
        composable(ProfileDestinations.FAQ) { DummyFullScreen(title = "FAQs") }
        composable(ProfileDestinations.MORE_APPS) { DummyFullScreen(title = "More Apps") }
    }
}
