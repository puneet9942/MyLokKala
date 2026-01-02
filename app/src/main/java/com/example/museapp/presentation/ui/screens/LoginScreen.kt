package com.example.museapp.presentation.ui.screens

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.util.Log
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.OutlinedTextField
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.museapp.util.ValidationUtils
import com.example.museapp.util.PhoneUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.zIndex
import com.example.museapp.presentation.feature.login.LoginEvent
import com.example.museapp.presentation.feature.login.LoginUiState
import com.example.museapp.presentation.feature.login.LoginViewModel
import com.example.museapp.presentation.ui.components.appButtonColors
import com.example.museapp.presentation.ui.components.appTextFieldColors
import com.example.museapp.ui.theme.AppTypography
import com.example.museapp.util.UserConsentHelper
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.GetPhoneNumberHintIntentRequest
import com.google.android.gms.auth.api.phone.SmsRetriever

// New imports for permission + location caching
import com.example.museapp.util.LocationHelper
import com.example.museapp.util.SharedPrefUtils
import com.example.museapp.util.AppConstants
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import android.location.Location
import java.lang.SecurityException

/**
 * Complete LoginScreen with:
 * - country picker click area
 * - phone hint (Identity API)
 * - SMS User Consent auto-fill (UserConsentHelper)
 * - stable navigation on loginSuccess via snapshotFlow
 *
 * Additionally: requests runtime location permission on first composition (once) and caches last_lat/last_lng.
 *
 * Styling:
 *  - Uses appTextFieldColors(), appButtonColors(), AppTypography and existing style helpers.
 *  - No style changes compared to your original file.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    state: StateFlow<LoginUiState>? = null,
    uiState: LoginUiState? = null,
    onPhoneChanged: (String) -> Unit = {},
    onCountryPicker: () -> Unit, // host must provide picker behaviour
    onContinue: () -> Unit = {},
    onLoggedIn: () -> Unit = {},
    onEvent: ((LoginEvent) -> Unit)? = null
) {
    // fallback VM only if caller didn't provide a state
    val fallbackVm: LoginViewModel? = if (state == null) hiltViewModel() else null

    // collect reactive state (either caller's flow or fallback VM's flow)
    val stateFlowToCollect = state ?: fallbackVm?.uiState
    val observedState by (stateFlowToCollect?.collectAsState() ?: remember { mutableStateOf<LoginUiState?>(null) })
    val innerState: LoginUiState? = uiState ?: observedState

    // event dispatcher: prefer caller-supplied onEvent, otherwise fallback to VM
    val effectiveOnEvent: (LoginEvent) -> Unit = onEvent ?: { ev -> fallbackVm?.onEvent(ev) ?: Unit }

    // UI values from state
    val phone = innerState?.phone ?: ""
    val otp = innerState?.otp ?: ""
    val countryCode = innerState?.countryCode ?: "+91"
    val loading = innerState?.loading ?: false
    val step = innerState?.step ?: LoginUiState.Step.EnterPhone
    val loginSuccess = innerState?.loginSuccess ?: false
    val iso = innerState?.iso ?: ValidationUtils.getCountryIsoFromCode(countryCode) ?: "IN"
    val vmError = innerState?.error

    // focus + keyboard
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // resend timer
    var resendSeconds by remember { mutableStateOf(30) }
    var resendJob by remember { mutableStateOf<Job?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // local UI flags
    var pendingVerify by remember { mutableStateOf(false) }
    var otpErrorMessage by remember { mutableStateOf<String?>(null) }

    // When entering OTP step start countdown & focus
    LaunchedEffect(step) {
        resendJob?.cancel()
        otpErrorMessage = null
        pendingVerify = false
        if (step == LoginUiState.Step.EnterOtp) {
            resendSeconds = 30
            resendJob = coroutineScope.launch {
                while (resendSeconds > 0 && isActive) {
                    delay(1000L)
                    resendSeconds--
                }
            }
            focusRequester.requestFocus()
        } else {
            resendJob?.cancel()
            resendSeconds = 0
        }
    }

    // Auto-verify: when otp reaches expected length and not loading, dispatch VerifyOtp
    LaunchedEffect(otp) {
        if (step == LoginUiState.Step.EnterOtp && otp.length >= 6 && !loading) {
            pendingVerify = true
            otpErrorMessage = null
            effectiveOnEvent(LoginEvent.VerifyOtp)
        }
    }

    // Observe loading + loginSuccess to detect verify failure/success
    LaunchedEffect(vmError, loginSuccess, pendingVerify) {
        if (loginSuccess) {
            pendingVerify = false
            return@LaunchedEffect
        }
        if (pendingVerify && vmError != null) {
            otpErrorMessage = vmError
            pendingVerify = false
            effectiveOnEvent(LoginEvent.ClearError)   // reset VM error
            effectiveOnEvent(LoginEvent.OtpChanged("")) // clear input for retry
            focusRequester.requestFocus()
        }
    }

    val onLoggedInRef = rememberUpdatedState(onLoggedIn)
    LaunchedEffect(loginSuccess) {
        if (loginSuccess) {
            Log.d("LoginScreen", "loginSuccess=true -> navigate")
            onLoggedInRef.value()
        }
    }

    // Continue button enablement
    val cleanedLocal = phone.trim().replace("^0+".toRegex(), "")
    val fullPhoneForValidation = countryCode.trim() + cleanedLocal
    val continueEnabled = when (step) {
        LoginUiState.Step.EnterPhone -> ValidationUtils.isValidPhoneForCountry(fullPhoneForValidation, iso) && !loading
        LoginUiState.Step.EnterOtp -> otp.length >= 6 && !loading
    }

    val configuration = LocalConfiguration.current
    val bannerHeightDp = (configuration.screenHeightDp * 0.40).dp // 40% height

    // Context for Identity & UserConsent
    val context = LocalContext.current

    // -------------------- Permission at screen start (conditional: only once) --------------------
    // SharedPref key to track if we've already asked for location permission
    val PREF_KEY_LOCATION_REQUESTED = "pref_loc_permission_requested"

    // Launcher for the system permission dialog; uses LocationHelper.requestLocationPermission below
    var permissionRequestedLocal by rememberSaveable { mutableStateOf(false) }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        coroutineScope.launch {
            try {
                if (granted) {
                    // permission granted: fetch and cache location safely
                    val loc = safeGetLastKnownLocation(context)
                    cacheLocationIfAvailable(context, loc)
                } else {
                    // permission denied: ensure defaults cached so other flows have values
                    cacheLocationIfAvailable(context, null)
                }
            } finally {
                // mark that we've asked at least once so we won't prompt again
                try {
                    SharedPrefUtils.putString(context, PREF_KEY_LOCATION_REQUESTED, "1")
                } catch (_: Throwable) { /* ignore */ }
            }
        }
    }

    // Fire once when Login screen appears OR if flag is unset (first install)
    LaunchedEffect(Unit) {
        if (!permissionRequestedLocal) {
            permissionRequestedLocal = true
            try {
                val alreadyAsked = SharedPrefUtils.getString(context, PREF_KEY_LOCATION_REQUESTED)
                val asked = !alreadyAsked.isNullOrEmpty() && alreadyAsked == "1"

                if (!asked) {
                    // Not asked before: now either fetch (if permission already granted) or request
                    if (LocationHelper.hasLocationPermission(context)) {
                        // Already granted -> fetch & cache and mark asked
                        coroutineScope.launch {
                            val loc = safeGetLastKnownLocation(context)
                            cacheLocationIfAvailable(context, loc)
                            try {
                                SharedPrefUtils.putString(context, PREF_KEY_LOCATION_REQUESTED, "1")
                            } catch (_: Throwable) { /* ignore */ }
                        }
                    } else {
                        // Not granted -> launch system permission dialog via your helper
                        LocationHelper.requestLocationPermission(locationLauncher)
                        // note: we mark 'asked' only after user responds (in launcher callback)
                    }
                } else {
                    // already asked previously: no prompt. But if permission currently exists, fetch & cache.
                    if (LocationHelper.hasLocationPermission(context)) {
                        coroutineScope.launch {
                            val loc = safeGetLastKnownLocation(context)
                            cacheLocationIfAvailable(context, loc)
                        }
                    }
                }
            } catch (t: Throwable) {
                Log.w("LoginScreen", "permission request check failed: ${t.message}")
                // Do not block login flow - ensure defaults cached
                try {
                    val alreadyAsked = SharedPrefUtils.getString(context, PREF_KEY_LOCATION_REQUESTED)
                    val asked = !alreadyAsked.isNullOrEmpty() && alreadyAsked == "1"
                    if (!asked) {
                        SharedPrefUtils.putString(context, PREF_KEY_LOCATION_REQUESTED, "1")
                        cacheLocationIfAvailable(context, null)
                    }
                } catch (_: Throwable) { /* ignore */ }
            }
        }
    }

    // -------------------- User Consent (SMS auto-fill) --------------------
    // Launcher for the consent activity returned by SmsRetriever / User Consent API
    val consentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            // SmsRetriever.EXTRA_SMS_MESSAGE contains the SMS body (per User Consent API)
            val message = result.data?.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE)
            message?.let { smsText ->
                // extract OTP with a regex (adjust the length if your OTP is not 6)
                val otpRegex = Regex("""\b(\d{4,6})\b""")
                val match = otpRegex.find(smsText)
                val code = match?.groups?.get(1)?.value ?: smsText.filter { it.isDigit() }.take(6)
                if (code.isNotBlank()) {
                    val digitsOnly = code.filter { ch -> ch.isDigit() }
                    // update VM/UI and trigger verify if full length
                    effectiveOnEvent(LoginEvent.OtpChanged(digitsOnly))
                    if (digitsOnly.length >= 6 && !loading) {
                        effectiveOnEvent(LoginEvent.VerifyOtp)
                    }
                }
            }
        } else {
            // user denied consent or canceled; nothing to do
            Log.d("LoginScreen", "consentLauncher: user cancelled or denied")
        }
    }

    // Start/stop listening while on OTP step. Use named parameters to avoid trailing-lambda binding issues.
    DisposableEffect(key1 = step) {
        if (step == LoginUiState.Step.EnterOtp) {
            try {
                UserConsentHelper.startListening(
                    context = context,
                    onConsentIntent = { consentIntent: Intent ->
                        try {
                            consentLauncher.launch(consentIntent)
                        } catch (t: Throwable) {
                            Log.w("LoginScreen", "Failed to launch consent intent: ${t.message}")
                        }
                    },
                    otpLengthMin = 6,
                    otpLengthMax = 6
                )
            } catch (t: Throwable) {
                Log.w("LoginScreen", "UserConsentHelper.startListening threw: ${t.message}")
            }
        }

        onDispose {
            try {
                UserConsentHelper.stopListening(context)
            } catch (t: Throwable) {
                // ignore
            }
        }
    }

    // -------------------- Phone Hint (Identity) --------------------
    // Activity result launcher for phone hint IntentSender
    val phoneHintLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val data: Intent? = result.data
                val selectedPhone = Identity.getSignInClient(context).getPhoneNumberFromIntent(data)
                selectedPhone?.let { raw ->
                    // parse using your PhoneUtils helper (adjust parsePhoneHint signature)
                    val parsed = try {
                        PhoneUtils.parsePhoneHint(raw, iso.ifBlank { "US" })
                    } catch (t: Throwable) {
                        null
                    }
                    parsed?.countryDialCode?.let { dial ->
                        effectiveOnEvent(LoginEvent.CountryChanged(dial))
                    }
                    val national = parsed?.nationalNumber ?: raw.filter { it.isDigit() }
                    effectiveOnEvent(LoginEvent.PhoneChanged(national))
                    onPhoneChanged(national)
                }
            } catch (t: Throwable) {
                // defensive fallback - extract digits
                try {
                    val data: Intent? = result.data
                    val raw = Identity.getSignInClient(context).getPhoneNumberFromIntent(data)
                    val digits = raw?.filter { it.isDigit() } ?: ""
                    effectiveOnEvent(LoginEvent.PhoneChanged(digits))
                    onPhoneChanged(digits)
                } catch (_: Throwable) {
                    Log.w("LoginScreen", "Phone hint parse fallback failed")
                }
            }
        } else {
            // cancelled or not available
            Log.d("LoginScreen", "phoneHintLauncher: cancelled or failed (resultCode=${result.resultCode})")
        }
    }

    fun showPhoneHintPicker() {
        try {
            val request = GetPhoneNumberHintIntentRequest.builder().build()
            Identity.getSignInClient(context)
                .getPhoneNumberHintIntent(request)
                .addOnSuccessListener { result ->
                    try {
                        val intentSender: IntentSender? = result.getIntentSender()
                        if (intentSender != null) {
                            val req = IntentSenderRequest.Builder(intentSender).build()
                            phoneHintLauncher.launch(req)
                        }
                    } catch (e: Exception) {
                        Log.w("LoginScreen", "launch phone hint failed: ${e.message}")
                    }
                }
                .addOnFailureListener { ex ->
                    Log.d("LoginScreen", "getPhoneNumberHintIntent failed: ${ex.message}")
                }
        } catch (e: Exception) {
            Log.w("LoginScreen", "showPhoneHintPicker error: ${e.message}")
        }
    }

    // optionally auto-launch phone hint when entering EnterPhone step (small delay to avoid "not attached" races)
    var phoneHintLaunched by rememberSaveable { mutableStateOf(false) }

// replace existing LaunchedEffect(step) block that calls showPhoneHintPicker with:
    LaunchedEffect(step) {
        if (step == LoginUiState.Step.EnterPhone && !phoneHintLaunched) {
            phoneHintLaunched = true
            coroutineScope.launch {
                delay(300L)
                try { showPhoneHintPicker() } catch (t: Throwable) { Log.w("LoginScreen","auto phone hint failed:${t.message}") }
            }
        }
        // when leaving EnterPhone, keep flag true so it won't relaunch on small recomposes
    }

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    // -------------------- UI --------------------
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(bannerHeightDp)
                        .padding(top = topInset)
                ) {
                    Image(
                        painter = painterResource(id =com.example.museapp.R.drawable.backdrop ),
                        contentDescription = "Login banner",
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (step == LoginUiState.Step.EnterPhone) "Log in or sign up" else "We have sent a verification code to",
                        style = AppTypography.titleMedium,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (step == LoginUiState.Step.EnterOtp) {
                        // phone display (countryCode gets updated from observed state reliably)
                        Text(
                            text = "$countryCode $phone",
                            style = AppTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Responsive OTP boxes
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            val maxW = maxWidth
                            val spacing = 12.dp
                            val totalSpacing = spacing * 5
                            val candidate = (maxW - totalSpacing) / 6f
                            val boxSize = when {
                                candidate > 72.dp -> 72.dp
                                candidate < 40.dp -> 40.dp
                                else -> candidate
                            }
                            val totalBoxesWidth = boxSize * 6 + totalSpacing

                            Box(modifier = Modifier.width(totalBoxesWidth), contentAlignment = Alignment.Center) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(spacing),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val displayChars = otp.padEnd(6, ' ').take(6).toCharArray()
                                    for (i in 0 until 6) {
                                        val ch = displayChars[i]
                                        Box(
                                            modifier = Modifier
                                                .size(boxSize)
                                                .shadow(4.dp, RoundedCornerShape(10.dp))
                                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                                                .border(BorderStroke(1.dp, if (otpErrorMessage != null) Color.Red else Color(0xFFCCCCCC)), RoundedCornerShape(10.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (ch == ' ') "" else ch.toString(),
                                                style = AppTypography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = (boxSize.value * 0.36).sp,
                                                color = if (otpErrorMessage != null) Color.Red else MaterialTheme.colorScheme.onSurface,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }

                                // Overlay invisible BasicTextField exactly over the boxes
                                BasicTextField(
                                    value = otp,
                                    onValueChange = { new ->
                                        val digitsOnly = new.filter { it.isDigit() }.take(6)
                                        if (otpErrorMessage != null && digitsOnly.isNotEmpty()) {
                                            otpErrorMessage = null
                                        }
                                        effectiveOnEvent(LoginEvent.OtpChanged(digitsOnly))
                                    },
                                    modifier = Modifier
                                        .width(totalBoxesWidth)
                                        .height(boxSize)
                                        .focusRequester(focusRequester)
                                        .focusable(true),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                    textStyle = TextStyle(color = Color.Transparent),
                                    cursorBrush = SolidColor(Color.Transparent)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // error msg from failed verify
                        otpErrorMessage?.let { msg ->
                            Text(text = msg, color = Color.Red, style = AppTypography.bodySmall)
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        Text(
                            text = "Check text messages for your OTP",
                            style = AppTypography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Resend row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = "Didn't get the OTP? ", style = AppTypography.bodyMedium)
                            if (resendSeconds > 0) {
                                Text(
                                    text = "Resend SMS in ${resendSeconds}s",
                                    style = AppTypography.bodyMedium.copy(color = Color.Gray)
                                )
                            } else {
                                Text(
                                    text = "Resend SMS",
                                    style = AppTypography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.clickable {
                                        effectiveOnEvent(LoginEvent.RequestOtp)
                                        // restart countdown
                                        resendSeconds = 30
                                        resendJob?.cancel()
                                        resendJob = coroutineScope.launch {
                                            while (resendSeconds > 0 && isActive) {
                                                delay(1000L)
                                                resendSeconds--
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    } else {
                        // phone entry with country picker visible
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Surface with full clickable area (IconButton + whole-surface clickable)
                            Surface(
                                modifier = Modifier
                                    .width(96.dp)
                                    .height(56.dp)
                                    .shadow(2.dp, RoundedCornerShape(12.dp))
                                    .zIndex(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable(
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() }
                                        ) {
                                            Log.d("LoginScreen", "country tile clicked (surface)")
                                            onCountryPicker()
                                        }
                                        .padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = countryCode, style = AppTypography.titleSmall)
                                    Spacer(modifier = Modifier.weight(1f))
                                    IconButton(
                                        onClick = {
                                            Log.d("LoginScreen", "country icon clicked")
                                            onCountryPicker()
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Select country"
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            OutlinedTextField(
                                value = phone,
                                onValueChange = { new ->
                                    onPhoneChanged(new)
                                    effectiveOnEvent(LoginEvent.PhoneChanged(new))
                                },
                                placeholder = { Text(text = "Enter Phone Number", style = AppTypography.titleSmall) },
                                singleLine = true,
                                colors = appTextFieldColors(), // preserved styling
                                modifier = Modifier
                                    .height(56.dp)
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                textStyle = AppTypography.titleMedium // preserved typography
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                onContinue()
                                otpErrorMessage = null
                                effectiveOnEvent(LoginEvent.RequestOtp)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            enabled = continueEnabled,
                            colors = appButtonColors(), // preserved styling
                            shape = RoundedCornerShape(12.dp)

                        ) {
                            Text(
                                text = "Continue",
                                style = AppTypography.titleMedium.copy(color = MaterialTheme.colorScheme.onPrimary)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

/**
 * Safely obtain last known location.
 * Returns null if permission missing, or anything goes wrong.
 * We check permission via LocationHelper.hasLocationPermission(context) and guard SecurityException.
 */
@Suppress("BlockingMethodInNonBlockingContext")
private suspend fun safeGetLastKnownLocation(context: android.content.Context): Location? {
    if (!LocationHelper.hasLocationPermission(context)) return null

    return suspendCancellableCoroutine { cont ->
        try {
            val fused = LocationServices.getFusedLocationProviderClient(context)
            fused.lastLocation
                .addOnSuccessListener { loc -> cont.resume(loc) }
                .addOnFailureListener { _ -> cont.resume(null) }
        } catch (se: SecurityException) {
            // permission revoked between check and call
            cont.resume(null)
        } catch (t: Throwable) {
            cont.resume(null)
        }
    }
}

/**
 * Cache location into SharedPrefUtils if non-null; otherwise ensure defaults are set.
 * Keys: last_lat, last_lng (used elsewhere across app)
 */
private fun cacheLocationIfAvailable(context: android.content.Context, location: Location?) {
    try {
        if (location != null) {
            SharedPrefUtils.putString(context, "last_lat", location.latitude.toString())
            SharedPrefUtils.putString(context, "last_lng", location.longitude.toString())
        } else {
            val lat = SharedPrefUtils.getString(context, "last_lat")
            val lng = SharedPrefUtils.getString(context, "last_lng")
            if (lat.isNullOrEmpty() || lng.isNullOrEmpty()) {
                SharedPrefUtils.putString(context, "last_lat", AppConstants.DEFAULT_LAT.toString())
                SharedPrefUtils.putString(context, "last_lng", AppConstants.DEFAULT_LNG.toString())
            }
        }
    } catch (_: Throwable) {
        // swallow caching errors (do not crash the login flow)
    }
}
