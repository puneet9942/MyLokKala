package com.example.lokkala.presentation.feature.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.lokkala.util.CameraImagePicker
import com.example.lokkala.util.LocationHelper
import kotlinx.coroutines.flow.StateFlow
import com.google.accompanist.flowlayout.FlowRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    state: StateFlow<ProfileUiState>,
    onEvent: (ProfileEvent) -> Unit,
    onContinue: () -> Unit
) {
    val ui by state.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    var showImagePickerDialog by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onEvent(ProfileEvent.ProfilePicChanged(it)) }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraImageUri != null) {
            onEvent(ProfileEvent.ProfilePicChanged(cameraImageUri))
        }
    }

    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // Go to Home even if permission is denied
        onContinue()
    }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Set up your profile") }) }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Image + Pencil
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .clickable { showImagePickerDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (ui.profilePicUri != null) {
                        Image(
                            painter = rememberAsyncImagePainter(ui.profilePicUri),
                            contentDescription = "Profile picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = "Add photo",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    // Pencil icon overlay
                    IconButton(
                        onClick = { showImagePickerDialog = true },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 8.dp, y = (-8).dp)
                            .size(28.dp)
                            .background(Color.White, shape = CircleShape)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = ui.name,
                    onValueChange = { onEvent(ProfileEvent.NameChanged(it)) },
                    label = { Text("Your Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(24.dp))

                Text("What interests you?", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Select categories you're interested in to personalize your experience",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Spacer(Modifier.height(16.dp))

                // Interest grid with FlowRow
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    mainAxisSpacing = 12.dp,
                    crossAxisSpacing = 12.dp
                ) {
                    ui.availableInterests.forEach { interest ->
                        val selected = ui.interests.contains(interest)
                        FilterChip(
                            selected = selected,
                            onClick = { onEvent(ProfileEvent.InterestToggled(interest)) },
                            label = { Text(interest, maxLines = 2) }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))

                // Custom Interest
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = ui.customInterest,
                        onValueChange = { onEvent(ProfileEvent.CustomInterestChanged(it)) },
                        placeholder = { Text("Add custom interest") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        isError = ui.customInterestError != null
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onEvent(ProfileEvent.AddCustomInterest) },
                        enabled = ui.customInterest.isNotBlank()
                    ) { Text("+") }
                }
                ui.customInterestError?.let { error ->
                    Text(
                        text = error,
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))

                Row(Modifier.fillMaxWidth()) { Text("Selected interests:") }
                FlowRow(
                    Modifier.fillMaxWidth(),
                    mainAxisSpacing = 8.dp,
                    crossAxisSpacing = 8.dp
                ) {
                    ui.interests.forEach { interest ->
                        AssistChip(
                            onClick = { onEvent(ProfileEvent.InterestToggled(interest)) },
                            label = { Text(interest, maxLines = 2) },
                            trailingIcon = { Text("✕") },
                            modifier = Modifier.heightIn(min = 32.dp)
                        )
                    }
                }
                Spacer(Modifier.height(64.dp))
            }

            Button(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(24.dp),
                enabled = ui.name.isNotBlank() && ui.interests.isNotEmpty(),
                onClick = { showLocationDialog = true }
            ) { Text("Continue") }
        }
    }

    // Gallery/Camera Picker Dialog
    if (showImagePickerDialog) {
        AlertDialog(
            onDismissRequest = { showImagePickerDialog = false },
            title = { Text("Choose Photo From") },
            text = {
                Column {
                    Button(
                        onClick = {
                            showImagePickerDialog = false
                            cameraImageUri = CameraImagePicker.createImageUri(context)
                            cameraImageUri?.let { cameraLauncher.launch(it) }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Camera") }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            showImagePickerDialog = false
                            galleryLauncher.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Gallery") }
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }

    // Location Permission Dialog
    if (showLocationDialog) {
        AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            title = { Text("Allow location?") },
            text = { Text("We need your location to personalize your experience. Please allow access.") },
            confirmButton = {
                TextButton(onClick = {
                    showLocationDialog = false
                    // Always request location, but go to Home in callback no matter what
                    LocationHelper.requestLocationPermission(locationPermissionLauncher)
                }) { Text("Allow") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showLocationDialog = false
                    // Go to Home immediately if denied
                    onContinue()
                }) { Text("Deny") }
            }
        )
    }
}
