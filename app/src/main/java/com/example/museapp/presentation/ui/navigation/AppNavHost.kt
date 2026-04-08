package com.example.museapp.presentation.ui.navigation

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
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
import com.example.museapp.data.remote.dto.ProfileCacheDto
import com.example.museapp.data.store.TokenStore.Companion.KEY_ACCESS_TOKEN
import com.example.museapp.presentation.feature.feedback.FeedbackScreen
import com.example.museapp.presentation.feature.home.HomeViewModel
import com.example.museapp.presentation.feature.login.LoginEvent
import com.example.museapp.presentation.feature.login.LoginViewModel
import com.example.museapp.presentation.feature.profile.ProfileFetchOrchestratorViewModel
import com.example.museapp.presentation.ui.screens.ProfileSetupScreen
import com.example.museapp.presentation.feature.profile.ProfileSetupViewModel
import com.example.museapp.presentation.ui.screens.CountryPickerScreen
import com.example.museapp.presentation.ui.screens.HomeScreen
import com.example.museapp.presentation.ui.screens.LoginScreen
import com.example.museapp.presentation.ui.screens.SavedScreen
import com.example.museapp.presentation.ui.screens.DummyFullScreen
import com.example.museapp.presentation.ui.screens.ProfileScreen
import com.example.museapp.presentation.ui.screens.CreateAdScreen
import com.example.museapp.presentation.ui.screens.ProfileDestinations
import com.example.museapp.presentation.ui.screens.ProfileDestinations.PROFILE_DETAIL
import com.example.museapp.presentation.ui.screens.SplashScreen
import com.example.museapp.util.AppConstants.SPLASH_SCREEN_DURATION_MILLISECONDS
import com.example.museapp.util.HideSystemBarsDuring
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.reflect.full.memberProperties

@SuppressLint("UnrememberedGetBackStackEntry")
@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val startDestination = Route.Splash.path
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {

        composable(Route.Splash.path) {
            HideSystemBarsDuring(active = true)
            SplashScreen()
            LaunchedEffect(Unit) {
                // Read KEY_ACCESS_TOKEN from SharedPreferences (kept if you want auto-login later)
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
            // obtain Login VM from this back entry
            val vm: LoginViewModel = hiltViewModel(backEntry)

            LoginScreen(
                state = vm.uiState,
                onPhoneChanged = { phone -> vm.onEvent(LoginEvent.PhoneChanged(phone)) },
                onCountryPicker = { navController.navigate(Route.CountryPicker.path) },
                onContinue = { /* optionally used */ },
                onLoggedIn = {
                    // 1) try to inject one-time JSON saved by LoginViewModel into the Login savedStateHandle
                    try {
                        val prefs = context.getSharedPreferences("museapp_auth_prefs", Context.MODE_PRIVATE)
                        val verifyJson = prefs.getString("verify_response_json", null)

                        if (!verifyJson.isNullOrBlank()) {
                            try {
                                val loginEntry = navController.getBackStackEntry(Route.Login.path)
                                // store JSON string under a safe key
                                loginEntry.savedStateHandle["verifyDataJson"] = verifyJson

                                // remove the one-time cache so it won't be reused
                               // prefs.edit().remove("verify_response_json").apply()
                                Log.d("AppNavHost", "verify response JSON injected into savedStateHandle and removed cache")
                            } catch (t: Throwable) {
                                Log.w("AppNavHost", "failed to inject verify_response_json into savedStateHandle: ${t.message}")
                            }
                        } else {
                            Log.d("AppNavHost", "no verify_response_json found in prefs (not coming from verify flow)")
                        }
                    } catch (t: Throwable) {
                        Log.w("AppNavHost", "failed to access prefs for verify_response_json: ${t.message}")
                    }

                    // 2) reflection fallback: if no JSON present, try to extract the object from the VM and serialize to JSON
                    try {
                        val existingJson = runCatching { navController.getBackStackEntry(Route.Login.path).savedStateHandle.get<String>("verifyDataJson") }.getOrNull()
                        if (existingJson.isNullOrBlank()) {
                            // probe common property names on the VM for a VerifyOtpData instance
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

                            // 2) probe uiState for nested VerifyOtpData
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

                            // 3) final fallback: scan all VM properties
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

                            // If found, serialize and inject as JSON string
                            verifyToPass?.let { vt ->
                                try {
                                    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                                    val adapter = moshi.adapter(VerifyOtpData::class.java)
                                    val json = adapter.toJson(vt)

                                    val loginEntry = navController.getBackStackEntry(Route.Login.path)
                                    loginEntry.savedStateHandle["verifyDataJson"] = json
                                    Log.d("AppNavHost", "verify object found via reflection -> serialized and injected as JSON")
                                } catch (t: Throwable) {
                                    Log.w("AppNavHost", "failed to serialize verify object found via reflection: ${t.message}")
                                }
                            } // end verifyToPass?.let
                        } else {
                            Log.d("AppNavHost", "verifyDataJson already present; skipping reflection probe")
                        }
                    } catch (t: Throwable) {
                        Log.w("AppNavHost", "reflection-based verifyData extraction failed: ${t.message}")
                    } finally {
                        // navigate to ProfileSetup (done in finally so navigation always occurs)
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
            val loginVm: LoginViewModel? = if (loginBackEntry != null) hiltViewModel(loginBackEntry) else null

            // --- read one-time JSON string from the login back entry (if any) ---
            val jsonFromLoginEntry: String? = remember(loginBackEntry) {
                runCatching { loginBackEntry?.savedStateHandle?.get<String>("verifyDataJson") }.getOrNull()
            }

            // Try to parse the JSON (do not remove here yet)
            val parsedVerifyFromJson: VerifyOtpData? = remember(jsonFromLoginEntry) {
                if (jsonFromLoginEntry.isNullOrBlank()) null
                else runCatching {
                    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                    moshi.adapter(VerifyOtpData::class.java).fromJson(jsonFromLoginEntry)
                }.getOrNull()
            }

            // If parsed OK, remove the one-time saved JSON so it won't be reused
            LaunchedEffect(parsedVerifyFromJson, loginBackEntry) {
                if (parsedVerifyFromJson != null) {
                    try { loginBackEntry?.savedStateHandle?.remove<String>("verifyDataJson") } catch (_: Throwable) { }
                    Log.d("AppNavHost", "Consumed and removed verifyDataJson from savedStateHandle")
                }
            }

            // Build verifyData by preferring parsed JSON; else fallback to reflection on the login VM
            val verifyData: VerifyOtpData? = remember(loginVm, parsedVerifyFromJson, loginBackEntry) {
                // 1) if parsed from JSON, use it
                if (parsedVerifyFromJson != null) return@remember parsedVerifyFromJson

                // 2) if no login VM, nothing to do
                if (loginVm == null) return@remember null

                // 3) reflection probing (same robust logic as earlier)
                var found: VerifyOtpData? = null
                val candidatesDirect = listOf("verifyOtpData", "verifyResponse", "verifyResponseData", "verify", "verifyOtp", "verifyData", "otpVerifyResponse")
                candidatesDirect.forEach { name ->
                    try {
                        val prop = loginVm::class.memberProperties.firstOrNull { it.name.equals(name, ignoreCase = true) }
                        val value = prop?.getter?.call(loginVm)
                        if (value is VerifyOtpData) found = value
                        if (value is kotlinx.coroutines.flow.StateFlow<*>) {
                            val inner = value.value
                            if (inner is VerifyOtpData) found = inner
                        }
                        if (value is androidx.lifecycle.LiveData<*>) {
                            val inner = value.value
                            if (inner is VerifyOtpData) found = inner
                        }
                    } catch (_: Throwable) { }
                }
                if (found != null) return@remember found

                loginVm::class.memberProperties.forEach { prop ->
                    try {
                        val v = prop.getter.call(loginVm)
                        if (v is VerifyOtpData) found = v
                        if (v is kotlinx.coroutines.flow.StateFlow<*>) {
                            val inner = v.value
                            if (inner is VerifyOtpData) found = inner
                        }
                        if (v is androidx.lifecycle.LiveData<*>) {
                            val inner = v.value
                            if (inner is VerifyOtpData) found = inner
                        }
                    } catch (_: Throwable) { }
                }

                // inspect uiState for nested VerifyOtpData
                runCatching {
                    val uiStateVal = runCatching { loginVm.uiState.value }.getOrNull()
                    uiStateVal?.let { st ->
                        st::class.memberProperties.forEach { p ->
                            try {
                                val valP = p.getter.call(st)
                                if (valP is VerifyOtpData) found = valP
                            } catch (_: Throwable) { }
                        }
                    }
                }

                found
            }

            // --- NEW: read profileCacheJson from the Home entry and parse into ProfileCacheDto ---
            val homeEntry = runCatching { navController.getBackStackEntry(Route.Home.path) }.getOrNull()
            val profileJsonFromHome = remember(homeEntry) {
                runCatching { homeEntry?.savedStateHandle?.get<String>("profileCacheJson") }.getOrNull()
            }

            val initialProfileCache: ProfileCacheDto? = remember(profileJsonFromHome) {
                profileJsonFromHome?.let { json ->
                    try {
                        val moshi = Moshi.Builder()
                            .add(KotlinJsonAdapterFactory())
                            .build()
                        moshi.adapter(ProfileCacheDto::class.java).fromJson(json)
                    } catch (e: Throwable) {
                        Log.w("AppNavHost", "failed to parse profileCacheJson: ${e.message}")
                        null
                    }
                }
            }

            // remove the saved value after consuming it (optional, but avoids stale reuse)
            LaunchedEffect(initialProfileCache, homeEntry) {
                if (initialProfileCache != null) {
                    try { homeEntry?.savedStateHandle?.remove<String>("profileCacheJson") } catch (_: Throwable) { }
                }
            }

            Log.d("PROFILE_PREFILL", "verifyData present? = ${verifyData != null} ; profileCache present? = ${initialProfileCache != null}")

            val prefs = context.getSharedPreferences("museapp_auth_prefs", Context.MODE_PRIVATE)
            val verifyJson = prefs.getString("verify_response_json", null)
            // Finally call ProfileSetupScreen and pass both forms (raw JSON and parsed object)
            ProfileSetupScreen(
                viewModel = profileVm,
                state = profileVm.state,
                onEvent = profileVm::onEvent,
                initialVerifyJson = verifyJson,   // raw JSON (nullable)
                initialVerifyData = verifyData,           // parsed object if available
                initialProfileCache = initialProfileCache,
                onContinue = {
                    // try to pop back to previous destination (if any)
                    val popped = navController.popBackStack()
                    if (!popped) {
                        // no previous entry -> go Home
                        navController.navigate(Route.Home.path) {
                            popUpTo(Route.ProfileSetup.path) { inclusive = true }
                        }
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

        composable(PROFILE_DETAIL) { backStackEntry ->
            val orchestratorVm: ProfileFetchOrchestratorViewModel = hiltViewModel()
            val profileSetupViewModel: ProfileSetupViewModel = hiltViewModel()

            LaunchedEffect(Unit) {
                Log.d("AppNavHost", "PROFILE_DETAIL LaunchedEffect start")

                // do heavy / IO work on IO dispatcher to avoid races with Room writes
                val profilePayload: ProfileCacheDto? = withContext(Dispatchers.IO) {
                    Log.d("AppNavHost", "IO: reading cached from orchestrator.getCachedProfileFromRoom()")
                    var cached = runCatching { orchestratorVm.getCachedProfileFromRoom() }.onFailure {
                        Log.w("AppNavHost", "IO: getCachedProfileFromRoom failed: ${it.message}")
                    }.getOrNull()

                    Log.d("AppNavHost", "IO: initial cached=${cached?.data?.id} name=${cached?.data?.fullName} epochCandidate=${cached?.data?.updatedAt}")

                    // 2) if missing, fetch from network and cache (usecase should persist)
                    if (cached == null) {
                        Log.d("AppNavHost", "IO: no cached -> calling fetchAndCacheProfileFromApi()")
                        val fetched = runCatching { orchestratorVm.fetchAndCacheProfileFromApi() }
                            .onFailure { Log.w("AppNavHost", "IO: fetchAndCacheProfileFromApi failed: ${it.message}") }
                            .getOrNull()

                        Log.d("AppNavHost", "IO: fetched=${fetched?.data?.id} name=${fetched?.data?.fullName} iso=${fetched?.data?.updatedAt}")

                        if (fetched != null) {
                            cached = fetched
                        } else {
                            Log.d("AppNavHost", "IO: fetch returned null -> re-reading Room for persisted value")
                            cached = runCatching { orchestratorVm.getCachedProfileFromRoom() }
                                .onFailure { Log.w("AppNavHost", "IO: second getCachedProfileFromRoom failed: ${it.message}") }
                                .getOrNull()
                            Log.d("AppNavHost", "IO: re-read cached=${cached?.data?.id} name=${cached?.data?.fullName} epochCandidate=${cached?.data?.updatedAt}")
                        }
                    } else {
                        Log.d("AppNavHost", "IO: cached exists, doing one more read to confirm freshness")
                        val maybeFresh = runCatching { orchestratorVm.getCachedProfileFromRoom() }
                            .onFailure { Log.w("AppNavHost", "IO: maybeFresh read failed: ${it.message}") }
                            .getOrNull()
                        Log.d("AppNavHost", "IO: maybeFresh=${maybeFresh?.data?.id} name=${maybeFresh?.data?.fullName} iso=${maybeFresh?.data?.updatedAt}")
                        if (maybeFresh != null) cached = maybeFresh
                    }

                    cached
                } // withContext(IO)

                Log.d("AppNavHost", "PROFILE_DETAIL finished IO; profilePayload id=${profilePayload?.data?.id} name=${profilePayload?.data?.fullName} iso=${profilePayload?.data?.updatedAt}")

                try {
                    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                    val adapter = moshi.adapter(ProfileCacheDto::class.java)
                    val json = profilePayload?.let { adapter.toJson(it) }
                    Log.d("AppNavHost", "Saving profileCacheJson into Home savedStateHandle; jsonLen=${json?.length ?: 0}")
                    val homeEntry = runCatching { navController.getBackStackEntry(Route.Home.path) }.getOrNull()
                    homeEntry?.savedStateHandle?.set("profileCacheJson", json)
                } catch (t: Throwable) {
                    Log.w("AppNavHost", "serialize/save profileCacheJson failed: ${t.message}")
                }

                navController.navigate(Route.ProfileSetup.path) {
                    popUpTo(PROFILE_DETAIL) { inclusive = true }
                    launchSingleTop = true
                }

                Log.d("AppNavHost", "PROFILE_DETAIL navigation to ProfileSetup requested")
            }
        }

        composable(ProfileDestinations.PROFILE_SUBSCRIPTIONS) { DummyFullScreen(title = "My subscriptions") }
        composable(ProfileDestinations.SUBSCRIBE_PRO) { DummyFullScreen(title = "Subscribe to Pro") }
        composable(ProfileDestinations.REFER_APP) { DummyFullScreen(title = "Refer the App") }
        composable(ProfileDestinations.RATE_APP) { DummyFullScreen(title = "Rate App") }
        composable(ProfileDestinations.ABOUT_US) { DummyFullScreen(title = "About Us") }
        composable(ProfileDestinations.TERMS) { DummyFullScreen(title = "Terms & Conditions") }
        composable(ProfileDestinations.PRIVACY) { DummyFullScreen(title = "Privacy Policy") }
        composable(ProfileDestinations.NOTIFICATION_PREFS) { DummyFullScreen(title = "Notification Preferences") }
        composable(ProfileDestinations.FAQ) { DummyFullScreen(title = "FAQs") }
        composable(ProfileDestinations.MORE_APPS) { DummyFullScreen(title = "More Apps") }
    }
}
