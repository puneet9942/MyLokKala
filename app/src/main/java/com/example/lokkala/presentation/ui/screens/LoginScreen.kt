package com.example.lokkala.presentation.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.lokkala.presentation.feature.login.LoginEvent
import com.example.lokkala.presentation.feature.login.LoginUiState
import com.example.lokkala.ui.theme.Purple40
import com.example.lokkala.util.ValidationUtils
import com.google.i18n.phonenumbers.PhoneNumberUtil
import kotlinx.coroutines.flow.StateFlow

@Composable
fun LoginScreen(
    state: StateFlow<LoginUiState>,
    onEvent: (LoginEvent) -> Unit,
    onLoggedIn: () -> Unit,
    onCountryPicker: () -> Unit
) {
    val ui by state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val fullPhone = ui.countryCode + ui.phone // e.g., "+91" + "1234567890"
    val iso = ValidationUtils.getCountryIsoFromCode(ui.countryCode) // e.g., "IN"
    val isPhoneValid = ValidationUtils.isValidPhoneForCountry(
        ui.countryCode + ui.phone,
        ValidationUtils.getCountryIsoFromCode(ui.countryCode) ?: ""
    )
    // isPhoneValid = ValidationUtils.isValidPhoneForCountry(ui.countryCode + ui.phone, ValidationUtils.getCountryIsoFromCode(ui.countryCode) ?: "")

    LaunchedEffect(ui.loginSuccess) {
        if (ui.loginSuccess) onLoggedIn()
    }
    LaunchedEffect(ui.successMessage) {
        ui.successMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { TopAppBar(title = { Text("Login") }) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1.4f)
                        .fillMaxWidth()
                        .defaultMinSize(minWidth = 90.dp, minHeight = 56.dp)
                ) {
                    OutlinedTextField(
                        value = ui.countryCode,
                        onValueChange = {}, // not editable
                        label = { Text("CC") },
                        readOnly = true,
                        enabled = true,
                        singleLine = true,
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Pick country"
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp)
                    )
                    // Full-size clickable overlay
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { onCountryPicker() }
                    )
                }
                OutlinedTextField(
                    value = ui.phone,
                    onValueChange = { onEvent(LoginEvent.PhoneChanged(it)) },
                    label = { Text("Phone") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(3f)
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 56.dp)
                )


            }

            Spacer(Modifier.height(24.dp))

            when (ui.step) {
                LoginUiState.Step.EnterPhone -> {
                    Button(
                        enabled = !ui.loading && isPhoneValid,
                        onClick = { onEvent(LoginEvent.RequestOtp) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(Purple40.value), // Purple 400 for example
                            contentColor = Color.White          // Text/Icon color
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Continue") }
                    // "Change country" button is now redundant (since picker is on field itself)
                }
                LoginUiState.Step.EnterOtp -> {
                    OutlinedTextField(
                        value = ui.otp,
                        onValueChange = { onEvent(LoginEvent.OtpChanged(it)) },
                        label = { Text("Enter OTP (try 123456 in FakeRepo)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { onEvent(LoginEvent.RequestOtp) }) { Text("Resend") }
                        TextButton(onClick = { /* Help action if needed */ }) { Text("Didn't get OTP?") }
                    }
                    Button(
                        enabled = !ui.loading && ui.otp.length == 6,
                        onClick = { onEvent(LoginEvent.VerifyOtp) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Verify & Continue") }
                }
            }

            if (ui.loading) {
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            if (ui.error != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    ui.error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
