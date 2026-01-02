package com.example.museapp.presentation.ui.screens

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult

import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.SwitchDefaults
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import coil.compose.rememberAsyncImagePainter
import coil.compose.AsyncImagePainter
import com.example.museapp.data.auth.dto.VerifyOtpData
import com.example.museapp.presentation.feature.profile.ProfileEvent
import com.example.museapp.presentation.feature.profile.ProfileSetupViewModel
import com.example.museapp.presentation.feature.profile.ProfileUiState
import com.example.museapp.ui.theme.AppTypography
import com.example.museapp.ui.theme.PrimaryColor
import com.example.museapp.util.InterestValidator
import com.example.museapp.util.VideoUtils
import com.example.museapp.util.filterDigitsAndDot
import com.example.museapp.util.saveBitmapToCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import kotlin.collections.get

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    state: StateFlow<ProfileUiState>? = null,
    viewModel: ProfileSetupViewModel? = null,
    onEvent: ((ProfileEvent) -> Unit)? = null,
    onContinue: (() -> Unit)? = null,
    onFinished: (() -> Unit)? = null,
    initialVerifyData: VerifyOtpData? = null
) {
    val context = LocalContext.current

    val actualStateFlow = state ?: viewModel!!.state
    val actualOnEvent: (ProfileEvent) -> Unit = onEvent ?: { viewModel!!.onEvent(it) }

    LaunchedEffect(initialVerifyData) {
        if (initialVerifyData != null) {
            viewModel?.prefillFromVerify(initialVerifyData)
            initialVerifyData.user?.fullName?.let { actualOnEvent(ProfileEvent.NameChanged(it)) }
            initialVerifyData.user?.dob?.let { raw ->
                val formatted = try {
                    OffsetDateTime.parse(raw)
                        .toLocalDate()
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                } catch (_: Exception) {
                    raw
                }
                actualOnEvent(ProfileEvent.DobChanged(formatted))
            }
        }
    }

    val ui by actualStateFlow.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Activity result launchers -- handlers will perform duplicate checks and show snackbars when needed
    val takePreviewLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp: Bitmap? ->
        bmp?.let {
            val uri = saveBitmapToCache(context, it)
            // prevent duplicate photos
            if (ui.photos.any { existing -> existing == uri }) {
                coroutineScope.launch { snackbarHostState.showSnackbar("This photo is already added") }
            } else {
                actualOnEvent(ProfileEvent.AddPhotos(listOf(uri)))
            }
        }
    }

    val galleryPhotosLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val newUris = uris.filterNot { incoming -> ui.photos.any { existing -> existing == incoming } }
            val duplicates = uris.size - newUris.size
            if (duplicates > 0) coroutineScope.launch { snackbarHostState.showSnackbar("Same photo was already added") }
            if (newUris.isNotEmpty()) actualOnEvent(ProfileEvent.AddPhotos(newUris))
        }
    }

    val galleryVideosLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            // persist permission (best effort)
            uris.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) { /* ignore */ }
            }

            val newUris = uris.filterNot { incoming -> ui.videos.any { existing -> existing == incoming } }
            val duplicates = uris.size - newUris.size
            if (duplicates > 0) coroutineScope.launch { snackbarHostState.showSnackbar("Same videos were already added") }
            if (newUris.isNotEmpty()) actualOnEvent(ProfileEvent.AddVideos(newUris))
        }
    }

    val profilePicGalleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            actualOnEvent(ProfileEvent.ProfilePicChanged(it))
        }
    }

    val takeProfilePicLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp: Bitmap? ->
        bmp?.let {
            val uri = saveBitmapToCache(context, it)
            actualOnEvent(ProfileEvent.ProfilePicChanged(uri))
        }
    }

    var showImageOptions by remember { mutableStateOf(false) }
    var showProfilePicOptions by remember { mutableStateOf(false) }

    // *** NEW: local toggle state to ask whether user wants to become an artist (default OFF) ***
    // We keep this local and only call the existing Submit event if user chooses not to become an artist.
    val wantToBecomeArtistState = remember { mutableStateOf(false) } // default false reduces friction for onlookers

    // When artist toggled OFF, clear event-manager flag to avoid inconsistent data
    LaunchedEffect(wantToBecomeArtistState.value) {
        if (!wantToBecomeArtistState.value) {
            actualOnEvent(ProfileEvent.SetEventManager(false))
        }
    }

    // small local map to track video thumbnail loaded states (ui-only)
    val videoLoadState = remember { mutableStateMapOf<Uri, Boolean>() }

    // effective maxStep depends on whether user wants to become an artist
    val effectiveMaxStep = if (wantToBecomeArtistState.value) getMaxStep() else 1

    // synchronous quick validation for enabling Next / Submit button
    fun isStepValidSync(step: Int, state: ProfileUiState): Boolean {
        return when (step) {
            1 -> state.name.isNotBlank() && state.dob.isNotBlank() && isNameValidQuick(state.name) && isDobValidQuick(state.dob)
            2 -> {
                // step 2 mandatory fields: description, biography, at least one interest
                val hasDescription = state.description.isNotBlank() && state.description.length <= 50
                val hasBiography = state.biography.isNotBlank()
                val hasSkills = state.interests.isNotEmpty()
                hasDescription && hasBiography && hasSkills
            }
            3 -> {
                // step 3 mandatory fields: pricing chosen and valid, travel radius present and numeric
                val pricing = state.pricingType.trim().lowercase()
                if (pricing.isBlank()) return false
                val pricingOk = when (pricing) {
                    "fixed", "hourly" -> state.standardPrice.isNotBlank()
                    "variable" -> {
                        val minOk = state.minPrice.isNotBlank()
                        val maxOk = state.maxPrice.isNotBlank()
                        if (!minOk || !maxOk) false else {
                            val min = state.minPrice.toDoubleOrNull()
                            val max = state.maxPrice.toDoubleOrNull()
                            min != null && max != null && min <= max
                        }
                    }
                    else -> false
                }
                val travelOk = state.travelRadiusKm.isNotBlank() && state.travelRadiusKm.toDoubleOrNull() != null
                pricingOk && travelOk
            }
            4 -> true
            else -> true
        }
    }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Profile setup", style = AppTypography.headlineSmall) }) },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    action = {
                        data.actionLabel?.let { TextButton(onClick = { /* nothing special */ }) { Text(it, style = AppTypography.labelLarge) } }
                    },
                    modifier = Modifier.padding(8.dp),
                    backgroundColor = Color(0xFFD32F2F),
                    contentColor = Color.White
                ) {
                    Text(text = data.message, style = AppTypography.bodyMedium)
                }
            }
        }
    ) { innerPadding ->
        val scrollState = rememberScrollState()

        // Provide default text style + content color so unstyled Texts and placeholders inherit app typography
        CompositionLocalProvider(LocalContentColor provides Color.Black) {
            ProvideTextStyle(value = AppTypography.bodyMedium) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(20.dp)
                        .padding(innerPadding)
                        .animateContentSize()
                ) {
                    val maxStep = effectiveMaxStep
                    // Dots row + linear progress underneath for quick "scroll bar" like indicator
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            repeat(maxStep) { i ->
                                val selected = ui.step == (i + 1)
                                Box(
                                    modifier = Modifier
                                        .padding(6.dp)
                                        .size(if (selected) 14.dp else 10.dp)
                                        .clip(CircleShape)
                                        .background(if (selected) PrimaryColor else Color.LightGray)
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        val progress = if (maxStep > 1) ((ui.step - 1).toFloat() / (maxStep - 1).toFloat()).coerceIn(0f, 1f) else 0f
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .padding(horizontal = 40.dp),
                            color = PrimaryColor,
                            backgroundColor = Color(0xFFEFEFEF)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // Step 1
                    AnimatedVisibility(visible = ui.step == 1, enter = fadeIn() + slideInVertically { it / 3 }, exit = fadeOut() + slideOutVertically { it / 3 }) {
                        StepOne(
                            ui = ui,
                            onEvent = actualOnEvent,
                            profilePicGalleryLauncher = profilePicGalleryLauncher,
                            takeProfilePicLauncher = takeProfilePicLauncher,
                            showProfilePicSetter = { showProfilePicOptions = it },
                            context = context,
                            snackbarHostState = snackbarHostState,
                            // pass artist choice state to StepOne for the new toggle
                            wantToBecomeArtistState = wantToBecomeArtistState
                        )
                    }

                    // Step 2
                    AnimatedVisibility(
                        visible = ui.step == 2 && wantToBecomeArtistState.value,
                        enter = fadeIn() + slideInVertically { it / 3 },
                        exit = fadeOut() + slideOutVertically { it / 3 }
                    ) {
                        val availableInterestsFromVm = viewModel?.availableInterests?.collectAsState(initial = emptyList())?.value ?: emptyList()
                        StepTwo(ui = ui, onEvent = actualOnEvent, available = availableInterestsFromVm)
                    }

                    // Step 3
                    AnimatedVisibility(
                        visible = ui.step == 3 && wantToBecomeArtistState.value,
                        enter = fadeIn() + slideInVertically { it / 3 },
                        exit = fadeOut() + slideOutVertically { it / 3 }
                    ) {
                        StepThree(
                            ui = ui,
                            onEvent = actualOnEvent
                        )
                    }

                    // Step 4
                    AnimatedVisibility(
                        visible = ui.step == 4 && wantToBecomeArtistState.value,
                        enter = fadeIn() + slideInVertically { it / 3 },
                        exit = fadeOut() + slideOutVertically { it / 3 }
                    ) {
                        StepFour(
                            ui = ui,
                            onEvent = actualOnEvent,
                            showImageOptionsSetter = { showImageOptions = it },
                            takePreviewLauncher = takeProfilePicLauncher,
                            galleryPhotosLauncher = galleryPhotosLauncher,
                            galleryVideosLauncher = galleryVideosLauncher, // pass the launcher variable (NOT the type)
                            videoLoadState = videoLoadState,
                            snackbarHostState = snackbarHostState
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    // --- Changed: Back button removed when on step 1 ---
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        if (ui.step > 1) {
                            Button(
                                onClick = { actualOnEvent(ProfileEvent.PrevStep) },
                                enabled = ui.step > 1,
                                colors = ButtonDefaults.buttonColors(backgroundColor = PrimaryColor, contentColor = Color.White),
                                modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                            ) {
                                Text("Back", style = AppTypography.labelLarge)
                            }
                        } else {
                            // keep spacing so Next/Submit stays right aligned
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        if (ui.step in 1..(maxStep - 1)) {
                            val nextEnabled = isStepValidSync(ui.step, ui)
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val err = validateForStep(ui.step, ui)
                                        if (err != null) {
                                            snackbarHostState.showSnackbar(err)
                                        } else {
                                            // NEW: If we're on step 1 and user does NOT want to become an artist,
                                            // then submit now and navigate away (same as final Submit).
                                            if (ui.step == 1 && !wantToBecomeArtistState.value) {
                                                // New client-side pre-submit validation to avoid server 500 for bad media
                                                val ok = hasOnlyAllowedImageFiles(context, ui)
                                                if (!ok) {
                                                    snackbarHostState.showSnackbar("Only image files (JPEG, JPG, PNG) are allowed!")
                                                } else {
                                                    actualOnEvent(ProfileEvent.Submit)
                                                    onContinue?.invoke()
                                                }
                                            } else {
                                                actualOnEvent(ProfileEvent.NextStep)
                                            }
                                        }
                                    }
                                },
                                enabled = nextEnabled,
                                colors = ButtonDefaults.buttonColors(backgroundColor = PrimaryColor, contentColor = Color.White),
                                modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                            ) {
                                Text("Next", style = AppTypography.labelLarge)
                            }
                        } else {
                            // final submit: ensure we only allow submit when current step is valid
                            val submitEnabled = isStepValidSync(ui.step, ui)
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val err = validateForStep(ui.step, ui)
                                        if (err != null) {
                                            snackbarHostState.showSnackbar(err)
                                        } else {
                                            // New client-side pre-submit validation to avoid server 500 for bad media
                                            val ok = hasOnlyAllowedImageFiles(context, ui)
                                            if (!ok) {
                                                snackbarHostState.showSnackbar("Only image files (JPEG, JPG, PNG) are allowed!")
                                            } else {
                                                actualOnEvent(ProfileEvent.Submit)
                                                onContinue?.invoke()
                                            }
                                        }
                                    }
                                },
                                enabled = submitEnabled,
                                colors = ButtonDefaults.buttonColors(backgroundColor = PrimaryColor, contentColor = Color.White),
                                modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                            ) {
                                Text("Submit", style = AppTypography.labelLarge)
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    LaunchedEffect(ui.error) {
                        ui.error?.let {
                            snackbarHostState.showSnackbar(it)
                            actualOnEvent(ProfileEvent.ClearError)
                        }
                    }
                } // end Column
            } // end ProvideTextStyle
        } // end CompositionLocalProvider
    }

    // image options dialog (Step 4)
    if (showImageOptions) {
        AlertDialog(
            onDismissRequest = { showImageOptions = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        galleryPhotosLauncher.launch("image/*")
                        showImageOptions = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = PrimaryColor)
                ) { Text("Gallery (Photos)", style = AppTypography.labelLarge) }
            },
            dismissButton = {
                Column {
                    TextButton(
                        onClick = {
                            takePreviewLauncher.launch(null)
                            showImageOptions = false
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = PrimaryColor)
                    ) { Text("Camera (Photo)", style = AppTypography.labelLarge) }

                    TextButton(
                        onClick = {
                            galleryVideosLauncher.launch(arrayOf("video/*"))
                            showImageOptions = false
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = PrimaryColor)
                    ) { Text("Gallery (Videos)", style = AppTypography.labelLarge) }
                }
            },
            title = { Text("Add media", style = AppTypography.titleSmall) },
            text = { Text("Choose photos or videos", style = AppTypography.bodySmall) }
        )
    }

    // profile pic selection dialog (Step 1)
    if (showProfilePicOptions) {
        AlertDialog(
            onDismissRequest = { showProfilePicOptions = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        takeProfilePicLauncher.launch(null)
                        showProfilePicOptions = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = PrimaryColor)
                ) { Text("Camera (Photo)", style = AppTypography.labelLarge) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        profilePicGalleryLauncher.launch("image/*")
                        showProfilePicOptions = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = PrimaryColor)
                ) { Text("Gallery (Photo)", style = AppTypography.labelLarge) }
            },
            title = { Text("Set profile picture", style = AppTypography.titleSmall) },
            text = { Text("Choose camera or gallery to set your profile picture", style = AppTypography.bodySmall) }
        )
    }
}

/* -------------------------
   Step composables
   ------------------------- */

@Composable
private fun StepOne(
    ui: ProfileUiState,
    onEvent: (ProfileEvent) -> Unit,
    profilePicGalleryLauncher: ManagedActivityResultLauncher<String, Uri?>,
    takeProfilePicLauncher: ManagedActivityResultLauncher<Void?, Bitmap?>,
    showProfilePicSetter: (Boolean) -> Unit,
    context: Context,
    snackbarHostState: SnackbarHostState,
    wantToBecomeArtistState: MutableState<Boolean>
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(92.dp)
                .clip(CircleShape)
                .background(Color.Transparent)
                .clickable { showProfilePicSetter(true) },
            contentAlignment = Alignment.Center
        ) {
            if (ui.profilePicUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(ui.profilePicUri),
                    contentDescription = "Profile",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.AccountCircle, contentDescription = "Add photo", modifier = Modifier.size(48.dp), tint = PrimaryColor)
            }
            IconButton(onClick = { showProfilePicSetter(true) }, modifier = Modifier.offset(y = (-8).dp).size(28.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = PrimaryColor)
            }
        }

        Spacer(Modifier.height(12.dp))

        val coroutineScope = rememberCoroutineScope()
        var nameFieldHasFocus by remember { mutableStateOf(false) }
        val nameError = remember(ui.name) { validateNameDetailed(ui.name) }

        // OutlinedTextField with focus-change validation + snackbar on blur
        OutlinedTextField(
            value = ui.name,
            onValueChange = { onEvent(ProfileEvent.NameChanged(it)) },
            label = { Row { Text("Full name", style = AppTypography.bodyMedium); Spacer(Modifier.width(6.dp)); Text("*", color = Color.Red, style = AppTypography.bodyMedium) } },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    // if focus was lost, validate and show snackbar if invalid
                    if (nameFieldHasFocus && !focusState.isFocused) {
                        val err = validateNameDetailed(ui.name)
                        if (err != null) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(err)
                            }
                        }
                    }
                    nameFieldHasFocus = focusState.isFocused
                },
            isError = nameError != null,
            textStyle = AppTypography.bodyMedium,
            placeholder = { Text("e.g., John Doe", style = AppTypography.bodyMedium) },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = Color.Black,
                focusedBorderColor = if (nameError == null) PrimaryColor else Color(0xFFD32F2F),
                unfocusedBorderColor = Color(0xFFDDDDDD),
                focusedLabelColor = if (nameError == null) PrimaryColor else Color(0xFFD32F2F),
                cursorColor = PrimaryColor
            )
        )

        Spacer(Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            val coroutineScope = rememberCoroutineScope()
            val focusManager = LocalFocusManager.current
            val keyboardController = LocalSoftwareKeyboardController.current

            OutlinedTextField(
                value = ui.dob,
                onValueChange = { /* readOnly - VM drives this */ },
                readOnly = true,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                label = { Row { Text("Date of birth", style = AppTypography.bodyMedium); Spacer(Modifier.width(6.dp)); Text("*", color = Color.Red, style = AppTypography.bodyMedium) } },
                placeholder = { Text("DD/MM/YYYY", style = AppTypography.bodyMedium) },
                textStyle = AppTypography.bodyMedium,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = Color.Black,
                    focusedBorderColor = PrimaryColor,
                    unfocusedBorderColor = Color(0xFFDDDDDD),
                    focusedLabelColor = PrimaryColor,
                    cursorColor = PrimaryColor
                )
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(Unit) {
                        detectTapGestures {
                            // dismiss keyboard / clear focus immediately
                            focusManager.clearFocus(force = true)
                            keyboardController?.hide()

                            openDatePicker(context) { picked ->
                                val err = validateDobDetailed(picked)
                                if (err == null) {
                                    onEvent(ProfileEvent.DobChanged(picked))
                                } else {
                                    coroutineScope.launch {
                                        val result = snackbarHostState.showSnackbar(err, actionLabel = "Back")
                                        if (result == SnackbarResult.ActionPerformed) {
                                            onEvent(ProfileEvent.PrevStep)
                                        }
                                    }
                                }
                            }
                        }
                    }
            )
        }

        Spacer(Modifier.height(8.dp))

        // NEW: Ask user if they want to become an artist.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Are you an artist?", style = AppTypography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(Modifier.height(4.dp))
                Text("Share your talent with the world.", style = AppTypography.bodySmall, color = Color.Gray)
            }
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = wantToBecomeArtistState.value,
                onCheckedChange = { wantToBecomeArtistState.value = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PrimaryColor,
                    checkedTrackColor = PrimaryColor.copy(alpha = 0.5f),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color.LightGray
                )
            )
        }

        Spacer(Modifier.height(8.dp))

        // Show Event Manager option only if "Are you an artist?" is checked true
        if (wantToBecomeArtistState.value) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Are you an event manager?", style = AppTypography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = ui.isEventManager == true,
                    onCheckedChange = { onEvent(ProfileEvent.SetEventManager(it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PrimaryColor,
                        checkedTrackColor = PrimaryColor.copy(alpha = 0.5f),
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color.LightGray
                    )
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        if (ui.isEventManager != true) {
            GenderDropdown(selected = ui.gender ?: "", onSelect = { onEvent(ProfileEvent.GenderChanged(it)) })
        }
    }
}

@Composable
private fun GenderDropdown(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val displayText = if (selected.isBlank()) "" else selected.replaceFirstChar { it.uppercase() }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            label = { Text("Gender", style = AppTypography.bodyMedium) },
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Open gender options", tint = PrimaryColor)
                }
            },
            textStyle = AppTypography.bodyMedium,
            placeholder = { Text("", style = AppTypography.bodyMedium) },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = Color.Black,
                focusedBorderColor = PrimaryColor,
                unfocusedBorderColor = Color(0xFFDDDDDD),
                focusedLabelColor = PrimaryColor,
                cursorColor = PrimaryColor
            )
        )

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf("Male", "Female", "Others").forEach { opt ->
                DropdownMenuItem(onClick = {
                    expanded = false
                    onSelect(opt.lowercase())
                }) {
                    Text(opt, style = AppTypography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun StepTwo(ui: ProfileUiState, onEvent: (ProfileEvent) -> Unit, available: List<String>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Removed the visible header "Profile summary" as requested.
        Spacer(Modifier.height(0.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Spacer(modifier = Modifier.weight(1f))
            // keep the character counter (was there before)
            Text("${ui.description.length}/50", style = AppTypography.bodySmall, color = Color.Gray)
        }
        Spacer(Modifier.height(4.dp))

        OutlinedTextField(
            value = ui.description,
            onValueChange = { if (it.length <= 50) onEvent(ProfileEvent.DescriptionChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Row { Text("Short summary", style = AppTypography.bodyMedium); Spacer(Modifier.width(6.dp)); Text("*", color = Color.Red, style = AppTypography.bodyMedium) } },
            placeholder = { Text("Short summary about you (max 50 chars)", style = AppTypography.bodyMedium) },
            textStyle = AppTypography.bodyMedium,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = Color.Black,
                focusedBorderColor = PrimaryColor,
                unfocusedBorderColor = Color(0xFFDDDDDD),
                focusedLabelColor = PrimaryColor,
                cursorColor = PrimaryColor
            )
        )
        Spacer(Modifier.height(12.dp))

        // Removed the visible header "Biography" as requested.
        Spacer(Modifier.height(0.dp))

        OutlinedTextField(
            value = ui.biography,
            onValueChange = { onEvent(ProfileEvent.BiographyChanged(it)) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp, max = 140.dp),
            label = { Row { Text("Biography", style = AppTypography.bodyMedium); Spacer(Modifier.width(6.dp)); Text("*", color = Color.Red, style = AppTypography.bodyMedium) } },
            placeholder = { Text("Tell us more about your experience / specialties", style = AppTypography.bodyMedium) },
            textStyle = AppTypography.bodyMedium,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = Color.Black,
                focusedBorderColor = PrimaryColor,
                unfocusedBorderColor = Color(0xFFDDDDDD),
                focusedLabelColor = PrimaryColor,
                cursorColor = PrimaryColor
            )
        )

        Spacer(Modifier.height(12.dp))
        // Skills header kept as before
        Text("Your skills / expertise", style = AppTypography.bodyMedium.copy(fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(8.dp))

        FlowRowInterests(
            available = available,
            selected = ui.interests,
            onToggle = { interest -> onEvent(ProfileEvent.InterestToggled(interest)) }
        )

        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            val customText = ui.customInterest ?: ""
            val normalizedUi = remember(customText) { customText.replace(Regex("\\s+"), " ").trim() }
            val isCustomValid = remember(customText) {
                InterestValidator.isValidInterest(normalizedUi)
            }

            OutlinedTextField(
                value = ui.customInterest ?: "",
                onValueChange = { onEvent(ProfileEvent.CustomInterestChanged(it)) },
                modifier = Modifier.weight(1f),
                label = { Text("Add custom interest", style = AppTypography.bodyMedium) },
                placeholder = { Text("e.g., Yoga", style = AppTypography.bodyMedium) },
                singleLine = true,
                isError = customText.isNotEmpty() && !isCustomValid,
                textStyle = AppTypography.bodyMedium,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = Color.Black,
                    focusedBorderColor = PrimaryColor,
                    unfocusedBorderColor = Color(0xFFDDDDDD),
                    focusedLabelColor = PrimaryColor,
                    cursorColor = PrimaryColor
                )
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { onEvent(ProfileEvent.AddCustomInterest) },
                enabled = isCustomValid,
                colors = ButtonDefaults.buttonColors(backgroundColor = PrimaryColor, contentColor = Color.White),
                modifier = Modifier.defaultMinSize(minHeight = 44.dp)
            ) {
                Text("Add", style = AppTypography.labelLarge)
            }
        }
    }
}

@Composable
private fun OriginalStyleChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val indication = LocalIndication.current

    val backgroundColor = if (selected) PrimaryColor else Color.Transparent
    val contentColor = if (selected) Color.White else LocalContentColor.current
    val borderColor = if (selected) PrimaryColor else Color(0xFFDDDDDD)

    Box(
        modifier = Modifier
            .defaultMinSize(minHeight = 36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(backgroundColor)
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = indication,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = contentColor,
            style = AppTypography.bodyMedium
        )
    }
}

@Composable
private fun FlowRowInterests(
    available: List<String>,
    selected: List<String>,
    onToggle: (String) -> Unit
) {
    Column {
        available.chunked(3).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { item ->
                    val sel = selected.contains(item)
                    OriginalStyleChip(text = item, selected = sel) {
                        onToggle(item)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StepThree(
    ui: ProfileUiState,
    onEvent: (ProfileEvent) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Tell your demand", style = AppTypography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(8.dp))

        // Removed "I am here to lookout..." toggle fully.
        // Always show pricing and travel radius for artists.

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val options = listOf("fixed", "hourly", "variable")
            options.forEach { p ->
                val selected = ui.pricingType.equals(p, ignoreCase = true)
                OriginalStyleChip(
                    text = p.replaceFirstChar { it.uppercase() },
                    selected = selected,
                    onClick = { onEvent(ProfileEvent.PricingTypeSelected(p)) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        when (ui.pricingType.lowercase()) {
            "fixed" -> {
                OutlinedTextField(
                    value = ui.standardPrice,
                    onValueChange = { onEvent(ProfileEvent.StandardPriceChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Row { Text("Fixed price", style = AppTypography.bodyMedium); Spacer(Modifier.width(6.dp)); Text("*", color = Color.Red, style = AppTypography.bodyMedium) } },
                    placeholder = { Text("e.g., 1000", style = AppTypography.bodyMedium) },
                    textStyle = AppTypography.bodyMedium,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color.Black,
                        focusedBorderColor = PrimaryColor,
                        unfocusedBorderColor = Color(0xFFDDDDDD),
                        focusedLabelColor = PrimaryColor,
                        cursorColor = PrimaryColor
                    )
                )
            }
            "hourly" -> {
                OutlinedTextField(
                    value = ui.standardPrice,
                    onValueChange = { onEvent(ProfileEvent.StandardPriceChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Row { Text("Hourly rate", style = AppTypography.bodyMedium); Spacer(Modifier.width(6.dp)); Text("*", color = Color.Red, style = AppTypography.bodyMedium) } },
                    placeholder = { Text("e.g., 200 / hour", style = AppTypography.bodyMedium) },
                    textStyle = AppTypography.bodyMedium,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color.Black,
                        focusedBorderColor = PrimaryColor,
                        unfocusedBorderColor = Color(0xFFDDDDDD),
                        focusedLabelColor = PrimaryColor,
                        cursorColor = PrimaryColor
                    )
                )
            }
            "variable" -> {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = ui.minPrice,
                        onValueChange = { onEvent(ProfileEvent.MinPriceChanged(it)) },
                        modifier = Modifier.weight(1f),
                        label = { Row { Text("Min price", style = AppTypography.bodyMedium); Spacer(Modifier.width(6.dp)); Text("*", color = Color.Red, style = AppTypography.bodyMedium) } },
                        placeholder = { Text("e.g., 500", style = AppTypography.bodyMedium) },
                        textStyle = AppTypography.bodyMedium,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            textColor = Color.Black,
                            focusedBorderColor = PrimaryColor,
                            unfocusedBorderColor = Color(0xFFDDDDDD),
                            focusedLabelColor = PrimaryColor,
                            cursorColor = PrimaryColor
                        )
                    )
                    OutlinedTextField(
                        value = ui.maxPrice,
                        onValueChange = { onEvent(ProfileEvent.MaxPriceChanged(it)) },
                        modifier = Modifier.weight(1f),
                        label = { Row { Text("Max price", style = AppTypography.bodyMedium); Spacer(Modifier.width(6.dp)); Text("*", color = Color.Red, style = AppTypography.bodyMedium) } },
                        placeholder = { Text("e.g., 2000", style = AppTypography.bodyMedium) },
                        textStyle = AppTypography.bodyMedium,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            textColor = Color.Black,
                            focusedBorderColor = PrimaryColor,
                            unfocusedBorderColor = Color(0xFFDDDDDD),
                            focusedLabelColor = PrimaryColor,
                            cursorColor = PrimaryColor
                        )
                    )
                }
            }
            else -> {
                OutlinedTextField(
                    value = ui.standardPrice,
                    onValueChange = { onEvent(ProfileEvent.StandardPriceChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Price", style = AppTypography.bodyMedium) },
                    placeholder = { Text("enter price", style = AppTypography.bodyMedium) },
                    textStyle = AppTypography.bodyMedium,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color.Black,
                        focusedBorderColor = PrimaryColor,
                        unfocusedBorderColor = Color(0xFFDDDDDD),
                        focusedLabelColor = PrimaryColor,
                        cursorColor = PrimaryColor
                    )
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = ui.travelRadiusKm,
            onValueChange = {
                val filtered = it.filterDigitsAndDot()
                onEvent(ProfileEvent.TravelRadiusChanged(filtered))
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Row { Text("Travel radius (km)", style = AppTypography.bodyMedium); Spacer(Modifier.width(6.dp)); Text("*", color = Color.Red, style = AppTypography.bodyMedium) } },
            placeholder = { Text("e.g., 10", style = AppTypography.bodyMedium) },
            textStyle = AppTypography.bodyMedium,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = Color.Black,
                focusedBorderColor = PrimaryColor,
                unfocusedBorderColor = Color(0xFFDDDDDD),
                focusedLabelColor = PrimaryColor,
                cursorColor = PrimaryColor
            )
        )

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun StepFour(
    ui: ProfileUiState,
    onEvent: (ProfileEvent) -> Unit,
    showImageOptionsSetter: (Boolean) -> Unit,
    takePreviewLauncher: ManagedActivityResultLauncher<Void?, Bitmap?>,
    galleryPhotosLauncher: ManagedActivityResultLauncher<String, List<Uri>>,
    galleryVideosLauncher: ManagedActivityResultLauncher<Array<String>, List<Uri>>,
    videoLoadState: MutableMap<Uri, Boolean>,
    snackbarHostState: SnackbarHostState
) {
    val coroutineScope = rememberCoroutineScope()
    val ctx = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Row {
            Text("Add photos", style = AppTypography.bodyMedium.copy(fontWeight = FontWeight.Bold))
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { showImageOptionsSetter(true) }, colors = ButtonDefaults.textButtonColors(contentColor = PrimaryColor)) {
                Text("Add", style = AppTypography.labelLarge)
            }
        }
        Spacer(Modifier.height(12.dp))

        if (ui.photos.isNotEmpty()) {
            LazyRow {
                itemsIndexed(ui.photos) { _, uri ->
                    Box(modifier = Modifier.size(96.dp).padding(end = 8.dp)) {
                        Image(painter = rememberAsyncImagePainter(uri), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)

                        // smaller close button (reduced size)
                        IconButton(
                            onClick = { onEvent(ProfileEvent.RemovePhoto(uri)) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(22.dp)
                                .background(Color.White.copy(alpha = 0.9f), shape = CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove photo", tint = Color.Black, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        } else {
            Text("No photos added yet", style = AppTypography.bodySmall, color = Color.Gray)
        }

        // inside StepFour() where you render ui.videos
        if (ui.videos.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("Videos", style = AppTypography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            LazyRow {
                itemsIndexed(ui.videos) { _, uri ->
                    // Defensive: handle nullable URIs by showing placeholder and skipping thumbnail loading.
                    if (uri == null) {
                        Box(modifier = Modifier.size(120.dp).padding(end = 8.dp)) {
                            Box(modifier = Modifier.fillMaxSize().background(Color.LightGray))
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "play", modifier = Modifier.align(Alignment.Center).size(40.dp))
                        }
                        return@itemsIndexed
                    }

                    // produceState runs a suspend producer; safe to call suspend load function here.
                    val thumbBitmap by produceState<android.graphics.Bitmap?>(initialValue = null, key1 = uri) {
                        // This try/catch is inside the suspend producer lambda (allowed). It will not wrap composable calls.
                        value = try {
                            VideoUtils.loadOrCreateVideoThumbnail(ctx, uri)
                        } catch (t: Throwable) {
                            // log if you want, or ignore to return null
                            null
                        }
                    }

                    // update UI load map so loader shows until thumb is ready
                    LaunchedEffect(uri, thumbBitmap) {
                        videoLoadState[uri] = thumbBitmap != null
                    }

                    Box(modifier = Modifier.size(120.dp).padding(end = 8.dp)) {
                        if (thumbBitmap != null) {
                            Image(bitmap = thumbBitmap!!.asImageBitmap(), contentDescription = "video thumb", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            // placeholder while generating thumbnail
                            Box(modifier = Modifier.fillMaxSize().background(Color.LightGray))
                        }

                        // central play icon
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "play", modifier = Modifier.align(Alignment.Center).size(40.dp))

                        // loader overlay while thumbnail is being created
                        if (videoLoadState[uri] != true) {
                            Box(modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(28.dp)
                                .background(Color(0xAA000000))
                            ) {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), strokeWidth = 2.dp, color = PrimaryColor)
                            }
                        }

                        // smaller close button
                        IconButton(
                            onClick = { onEvent(ProfileEvent.RemoveVideo(uri)); videoLoadState.remove(uri) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(22.dp)
                                .background(Color.White.copy(alpha = 0.9f), shape = CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove video", tint = Color.Black, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        } else {
            Text("No videos added yet", style = AppTypography.bodySmall, color = Color.Gray)
        }

        Spacer(Modifier.height(12.dp))
        Text("Social links", style = AppTypography.bodyMedium.copy(fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = ui.instaId ?: "",
            onValueChange = { onEvent(ProfileEvent.InstaChanged(it)) },
            label = { Text("Instagram", style = AppTypography.bodyMedium) },
            placeholder = { Text("e.g., instagram_handle", style = AppTypography.bodyMedium) },
            modifier = Modifier.fillMaxWidth(),
            textStyle = AppTypography.bodyMedium,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = Color.Black,
                focusedBorderColor = PrimaryColor,
                unfocusedBorderColor = Color(0xFFDDDDDD),
                focusedLabelColor = PrimaryColor,
                cursorColor = PrimaryColor
            )
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = ui.twitterId ?: "",
            onValueChange = { onEvent(ProfileEvent.TwitterChanged(it)) },
            label = { Text("Twitter", style = AppTypography.bodyMedium) },
            placeholder = { Text("e.g., twitter_handle", style = AppTypography.bodyMedium) },
            modifier = Modifier.fillMaxWidth(),
            textStyle = AppTypography.bodyMedium,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = Color.Black,
                focusedBorderColor = PrimaryColor,
                unfocusedBorderColor = Color(0xFFDDDDDD),
                focusedLabelColor = PrimaryColor,
                cursorColor = PrimaryColor
            )
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = ui.youtubeId ?: "",
            onValueChange = { onEvent(ProfileEvent.YoutubeChanged(it)) },
            label = { Text("YouTube", style = AppTypography.bodyMedium) },
            placeholder = { Text("e.g., youtube_channel", style = AppTypography.bodyMedium) },
            modifier = Modifier.fillMaxWidth(),
            textStyle = AppTypography.bodyMedium,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = Color.Black,
                focusedBorderColor = PrimaryColor,
                unfocusedBorderColor = Color(0xFFDDDDDD),
                focusedLabelColor = PrimaryColor,
                cursorColor = PrimaryColor
            )
        )
        Spacer(Modifier.height(8.dp))
        // Added Facebook input
        OutlinedTextField(
            value = ui.facebookId ?: "",
            onValueChange = { onEvent(ProfileEvent.FacebookChanged(it)) },
            label = { Text("Facebook", style = AppTypography.bodyMedium) },
            placeholder = { Text("e.g., facebook_profile", style = AppTypography.bodyMedium) },
            modifier = Modifier.fillMaxWidth(),
            textStyle = AppTypography.bodyMedium,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = Color.Black,
                focusedBorderColor = PrimaryColor,
                unfocusedBorderColor = Color(0xFFDDDDDD),
                focusedLabelColor = PrimaryColor,
                cursorColor = PrimaryColor
            )
        )
    }
}

/* -------------------------
   Utilities
   ------------------------- */

private fun openDatePicker(context: Context, onDateSelected: (String) -> Unit = {}) {
    val c = Calendar.getInstance()
    val year = c.get(Calendar.YEAR)
    val month = c.get(Calendar.MONTH)
    val day = c.get(Calendar.DAY_OF_MONTH)
    val dpd = DatePickerDialog(context, { _, y, m, d ->
        val mm = (m + 1).let { if (it < 10) "0$it" else it.toString() }
        val dd = if (d < 10) "0$d" else "$d"
        onDateSelected("$dd/$mm/$y")
    }, year, month, day)
    dpd.show()
}

private fun getMaxStep(): Int {
    try {
        val cls = ProfileUiState::class.java
        try {
            val f = cls.getField("MAX_STEP")
            return f.getInt(null)
        } catch (_: Throwable) { }
        try {
            val companionField = cls.getDeclaredField("Companion")
            companionField.isAccessible = true
            val companionInstance = companionField.get(null)
            val companionClass = companionInstance.javaClass
            val f2 = companionClass.getField("MAX_STEP")
            val value = f2.getInt(companionInstance)
            return value
        } catch (_: Throwable) { }
    } catch (_: Throwable) { }
    return 4
}

/**
 * Validation helpers
 */

private fun isNameValidQuick(name: String?): Boolean {
    if (name == null) return false
    val trimmed = name.trim()
    if (trimmed.length < 3 || trimmed.length > 100) return false
    // no digits allowed; allow letters, spaces, apostrophe, dot, hyphen
    val matches = Regex("^[A-Za-z .'\\-]+\$").matches(trimmed)
    return matches
}

private fun validateNameDetailed(name: String?): String? {
    if (name == null) return "Please enter your full name"
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return "Please enter your full name"
    if (trimmed.length < 3) return "Name must be at least 3 characters"
    if (trimmed.length > 100) return "Name is too long"
    if (!Regex("^[A-Za-z .'\\-]+\$").matches(trimmed)) return "Name contains invalid characters"
    // optional: ensure not single-letter words
    val words = trimmed.split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (words.any { it.length == 1 }) return "Please enter a valid full name"
    return null
}

private fun isDobValidQuick(dob: String?): Boolean {
    if (dob.isNullOrBlank()) return false
    return try {
        val pf = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val parsed = LocalDate.parse(dob, pf)
        // valid only if parsed date is on or before the date exactly 5 years ago
        !parsed.isAfter(LocalDate.now().minusYears(5))
    } catch (_: Exception) {
        false
    }
}

private fun validateDobDetailed(dob: String?): String? {
    if (dob.isNullOrBlank()) return "Please select date of birth"
    return try {
        val pf = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val parsed = LocalDate.parse(dob, pf)
        val minAllowed = LocalDate.now().minusYears(16)
        if (parsed.isAfter(minAllowed)) "You must be at least 16 years old to use the app"
        else null
    } catch (e: Exception) {
        "Invalid date format"
    }
}

/**
 * New helper to check allowed image types before submit.
 * Conservative extension-based check to prevent accidental non-image upload.
 */
private suspend fun hasOnlyAllowedImageFiles(context: Context, state: ProfileUiState): Boolean {
    return withContext(Dispatchers.IO) {
        val allowedExt = listOf("jpg", "jpeg", "png")

        fun extensionFromPath(path: String?): String? {
            return try {
                if (path == null) return null
                MimeTypeMap.getFileExtensionFromUrl(path)?.lowercase()
            } catch (_: Throwable) {
                null
            }
        }

        suspend fun isUriImageCandidate(any: Any?): Boolean {
            try {
                when (any) {
                    null -> return true // nothing to check
                    is Uri -> {
                        // 1) check mime type via ContentResolver
                        val mime = try { context.contentResolver.getType(any) } catch (_: Throwable) { null }
                        if (!mime.isNullOrBlank()) {
                            if (mime.lowercase().startsWith("image/")) return true
                        }

                        // 2) fallback: try extension from URI path or last path segment
                        val path = any.lastPathSegment ?: any.path ?: any.toString()
                        val extFromPath = extensionFromPath(path)
                        if (!extFromPath.isNullOrBlank() && allowedExt.contains(extFromPath)) return true

                        // 3) final fallback: attempt to open the stream and decode as bitmap
                        return try {
                            context.contentResolver.openInputStream(any)?.use { stream ->
                                val bmp = BitmapFactory.decodeStream(stream)
                                bmp != null
                            } ?: false
                        } catch (_: Throwable) {
                            false
                        }
                    }
                    is String -> {
                        val s = any.lowercase()
                        // try parsing as Uri then reuse above check
                        try {
                            val parsed = Uri.parse(any)
                            if (parsed.scheme == "content" || parsed.scheme == "file" || parsed.scheme == "android.resource") {
                                return isUriImageCandidate(parsed)
                            }
                        } catch (_: Throwable) { /* ignore */ }
                        // fallback: extension check on string
                        return allowedExt.any { s.endsWith(".$it") || s.contains(".$it") }
                    }
                    else -> {
                        val s = any.toString().lowercase()
                        return allowedExt.any { s.endsWith(".$it") || s.contains(".$it") }
                    }
                }
            } catch (_: Throwable) {
                return false
            }
        }

        // check profile pic
        try {
            state.profilePicUri?.let { if (!isUriImageCandidate(it)) return@withContext false }
        } catch (_: Throwable) { return@withContext false }

        // check photos collection
        try {
            if (state.photos.isNotEmpty()) {
                for (p in state.photos) {
                    if (!isUriImageCandidate(p)) return@withContext false
                }
            }
        } catch (_: Throwable) { return@withContext false }

        // Note: videos intentionally ignored here; your API expects images only for update endpoint.
        return@withContext true
    }
}

private suspend fun validateForStep(step: Int, state: ProfileUiState): String? {
    return when (step) {
        1 -> {
            validateNameDetailed(state.name) ?: validateDobDetailed(state.dob)
        }
        2 -> {
            // enforce: description present and <=50, biography present, at least one interest
            if (state.description.isBlank()) return "Please enter profile summary"
            if (state.description.length > 50) return "Profile Summary must be at most 50 characters"
            if (state.biography.isBlank()) return "Please enter biography"
            if (state.interests.isEmpty()) return "Please add at least one skill / expertise"
            null
        }
        3 -> {
            // pricing and travel radius mandatory
            val pricing = state.pricingType.trim().lowercase()
            if (pricing.isBlank()) return "Please select a pricing type"
            when (pricing) {
                "fixed", "hourly" -> {
                    if (state.standardPrice.isBlank()) return "Please enter price"
                    if (state.standardPrice.toDoubleOrNull() == null) return "Please enter valid numeric price"
                }
                "variable" -> {
                    if (state.minPrice.isBlank() || state.maxPrice.isBlank()) return "Please enter min and max price"
                    val min = state.minPrice.toDoubleOrNull() ?: return "Please enter valid numeric min price"
                    val max = state.maxPrice.toDoubleOrNull() ?: return "Please enter valid numeric max price"
                    if (min > max) return "Min price must be less than or equal to max price"
                }
                else -> return "Please select a valid pricing type"
            }
            if (state.travelRadiusKm.isBlank()) return "Please enter travel radius"
            if (state.travelRadiusKm.toDoubleOrNull() == null) return "Please enter valid travel radius"
            null
        }
        4 -> null
        else -> null
    }



}


