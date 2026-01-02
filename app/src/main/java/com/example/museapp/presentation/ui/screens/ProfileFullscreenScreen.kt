package com.example.museapp.presentation.ui.screens

import android.app.DatePickerDialog
import android.net.Uri
import android.widget.DatePicker
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.example.museapp.presentation.feature.profile.ProfileEvent

import com.example.museapp.ui.theme.AppTypography
import com.example.museapp.ui.theme.PrimaryColor
import com.google.accompanist.flowlayout.FlowRow
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun ProfileFullscreenScreen(
//    onBack: () -> Unit,
//    viewModel: ProfileSetupViewModel = hiltViewModel()
//) {
//    val ui by viewModel.state.collectAsState()
//
//    // local UI state
//    var editingMobile by remember { mutableStateOf(false) }
//    var editingInterests by remember { mutableStateOf(false) }
//
//    val context = LocalContext.current
//
//    // Image picker
//    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
//        viewModel.onEvent(ProfileEvent.ProfilePicChanged(uri))
//    }
//
//    // Date picker initialization / format
//    val calendar = Calendar.getInstance()
//    val dobFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
//    LaunchedEffect(ui.dob) {
//        try {
//            if (ui.dob.isNotBlank()) {
//                val d = dobFormat.parse(ui.dob)
//                if (d != null) calendar.time = d
//            }
//        } catch (_: Exception) {
//        }
//    }
//
//    // helper to show date picker (avoid duplicating code)
//    fun showDatePicker() {
//        val initialYear = calendar.get(Calendar.YEAR)
//        val initialMonth = calendar.get(Calendar.MONTH)
//        val initialDay = calendar.get(Calendar.DAY_OF_MONTH)
//        val dp = DatePickerDialog(
//            context,
//            { _: DatePicker, y: Int, m: Int, d: Int ->
//                val c = Calendar.getInstance()
//                c.set(Calendar.YEAR, y)
//                c.set(Calendar.MONTH, m)
//                c.set(Calendar.DAY_OF_MONTH, d)
//                viewModel.onEvent(ProfileEvent.DobChanged(dobFormat.format(c.time)))
//            },
//            initialYear,
//            initialMonth,
//            initialDay
//        )
//        dp.show()
//    }
//
//    // bottom bar animation state
//    var showBottomBar by remember { mutableStateOf(false) }
//    LaunchedEffect(Unit) {
//        // small delay to allow enter animation feel natural
//        delay(80)
//        showBottomBar = true
//    }
//
//    Scaffold(
//        topBar = {
//            CenterAlignedTopAppBar(
//                title = { Text("Your Profile", style = AppTypography.titleMedium) },
//                navigationIcon = {
//                    IconButton(onClick = onBack) {
//                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
//                    }
//                }
//            )
//        },
//        // Sticky bottom button: AnimatedVisibility for entry/exit transitions
//        bottomBar = {
//            AnimatedVisibility(
//                visible = showBottomBar,
//                enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
//                exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
//            ) {
//                // OUTER: full white area (no orangish tint)
//                Surface(
//                    tonalElevation = 4.dp,
//                    shadowElevation = 0.dp,
//                    color = Color.White // <- explicit white to avoid theme tint
//                ) {
//                    Divider(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .height(1.dp),
//                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
//                    )
//                    // outer padding to create white "card" look around the pill
//                    Box(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .background(Color.White) // ensure full white background
//                            .padding(horizontal = 12.dp, vertical = 12.dp),
//                        contentAlignment = Alignment.Center
//                    ) {
//                        // inner rounded rect background stays white as well
//                        Box(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(horizontal = 12.dp)
//                                .background(
//                                    color = Color.White, // explicit white
//                                    shape = RoundedCornerShape(28.dp)
//                                )
//                                .padding(10.dp)
//                        ) {
//                            // ALWAYS enabled (as requested) and calls Submit unconditionally
//                            Button(
//                                onClick = { viewModel.onEvent(ProfileEvent.Submit) },
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .height(56.dp),
//                                shape = RoundedCornerShape(28.dp),
//                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
//                            ) {
//                                Text("Update profile", style = AppTypography.titleMedium, color = MaterialTheme.colorScheme.onPrimary)
//                            }
//                        }
//                    }
//                }
//            }
//        },
//        content = { innerPadding ->
//            Column(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .verticalScroll(rememberScrollState())
//                    .padding(innerPadding)
//                    .padding(20.dp)
//            ) {
//                // Profile image (centered) with edit
//                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
//                    Box(
//                        modifier = Modifier
//                            .size(110.dp)
//                            .clip(CircleShape)
//                            .border(3.dp, PrimaryColor, CircleShape)
//                            .clickable { launcher.launch("image/*") },
//                        contentAlignment = Alignment.Center
//                    ) {
//                        if (ui.profilePicUri != null) {
//                            Image(
//                                painter = rememberAsyncImagePainter(ui.profilePicUri),
//                                contentDescription = "Profile picture",
//                                modifier = Modifier.fillMaxSize(),
//                                contentScale = ContentScale.Crop
//                            )
//                        } else {
//                            Icon(
//                                imageVector = Icons.Default.AccountCircle,
//                                contentDescription = "No profile picture",
//                                modifier = Modifier.size(64.dp),
//                                tint = PrimaryColor
//                            )
//                        }
//                    }
//
//                    // small edit pencil icon overlay
//                    IconButton(
//                        onClick = { launcher.launch("image/*") },
//                        modifier = Modifier
//                            .align(Alignment.TopEnd)
//                            .offset(x = (40).dp, y = (70).dp)
//                            .size(36.dp)
//                            .background(color = Color.White, shape = CircleShape)
//                    ) {
//                        Icon(Icons.Default.Edit, contentDescription = "Edit photo")
//                    }
//                }
//
//                Spacer(modifier = Modifier.height(18.dp))
//
//                // Name
//                OutlinedTextField(
//                    value = ui.name,
//                    onValueChange = { viewModel.onEvent(ProfileEvent.NameChanged(it)) },
//                    label = { Text("Name", style = AppTypography.labelSmall) },
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .heightIn(min = 56.dp)
//                )
//
//                Spacer(modifier = Modifier.height(12.dp))
//
//                // Mobile with CHANGE link to the right (inline edit)
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    if (!editingMobile) {
//                        OutlinedTextField(
//                            value = ui.mobile,
//                            onValueChange = { /* read-only unless editing */ },
//                            label = { Text("Mobile", style = AppTypography.labelSmall) },
//                            modifier = Modifier
//                                .weight(1f)
//                                .heightIn(min = 56.dp),
//                            enabled = false,
//                            readOnly = true
//                        )
//
//                        TextButton(onClick = { editingMobile = true }) {
//                            Text("CHANGE", style = AppTypography.titleSmall, color = PrimaryColor)
//                        }
//                    } else {
//                        OutlinedTextField(
//                            value = ui.mobile,
//                            onValueChange = { viewModel.onEvent(ProfileEvent.MobileChanged(it)) },
//                            label = { Text("Mobile", style = AppTypography.labelSmall) },
//                            modifier = Modifier
//                                .weight(1f)
//                                .heightIn(min = 56.dp),
//                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
//                            singleLine = true,
//                            trailingIcon = {
//                                IconButton(onClick = { editingMobile = false }) {
//                                    Icon(Icons.Default.Close, contentDescription = "Done")
//                                }
//                            }
//                        )
//                    }
//                }
//
//                Spacer(modifier = Modifier.height(12.dp))
//
//                // Date of birth - looks enabled but is readOnly; clicking opens DatePicker
//                OutlinedTextField(
//                    value = ui.dob,
//                    onValueChange = { /* read-only - changed via datepicker */ },
//                    label = { Text("Date of birth", style = AppTypography.labelSmall) },
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .heightIn(min = 56.dp)
//                        .clickable { showDatePicker() },
//                    enabled = true,         // keep it enabled so it doesn't look disabled
//                    readOnly = true,        // but still not editable by keyboard
//                    trailingIcon = {
//                        IconButton(onClick = { showDatePicker() }) {
//                            Icon(Icons.Default.DateRange, contentDescription = "Pick date")
//                        }
//                    }
//                )
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                // Interests - show all selected chips in view mode; toggle to edit to show full list
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Text("My interests", style = AppTypography.titleSmall, modifier = Modifier.weight(1f))
//                    TextButton(onClick = { editingInterests = !editingInterests }) {
//                        Text(if (editingInterests) "DONE" else "EDIT", style = AppTypography.titleSmall, color = PrimaryColor)
//                    }
//                }
//
//                Spacer(modifier = Modifier.height(8.dp))
//
//                if (!editingInterests) {
//                    // show ALL selected chips (not limited)
//                    val display = if (ui.interests.isEmpty()) listOf("No interests selected") else ui.interests
//                    FlowRow(
//                        mainAxisSpacing = 8.dp,
//                        crossAxisSpacing = 8.dp,
//                        modifier = Modifier.fillMaxWidth()
//                    ) {
//                        display.forEach { interest ->
//                            AssistChip(
//                                onClick = { /* maybe view detail? */ },
//                                label = { Text(interest, style = AppTypography.titleSmall) },
//                                enabled = ui.interests.isNotEmpty(),
//                                trailingIcon = {},
//                                colors = AssistChipDefaults.assistChipColors(
//                                    containerColor = PrimaryColor.copy(alpha = 0.14f),
//                                    labelColor = PrimaryColor
//                                ),
//                                modifier = Modifier.heightIn(min = 36.dp)
//                            )
//                        }
//                    }
//                } else {
//                    // Editing: full available interests list using FilterChip toggles
//                    FlowRow(
//                        mainAxisSpacing = 8.dp,
//                        crossAxisSpacing = 8.dp,
//                        modifier = Modifier.fillMaxWidth()
//                    ) {
//                        ui.availableInterests.forEach { interest ->
//                            val selected = ui.interests.contains(interest)
//                            FilterChip(
//                                selected = selected,
//                                onClick = { viewModel.onEvent(ProfileEvent.InterestToggled(interest)) },
//                                label = {
//                                    Text(
//                                        interest,
//                                        maxLines = 1,
//                                        style = AppTypography.titleSmall,
//                                        modifier = Modifier.padding(horizontal = 6.dp)
//                                    )
//                                },
//                                modifier = Modifier.height(36.dp),
//                                colors = FilterChipDefaults.filterChipColors(
//                                    selectedContainerColor = PrimaryColor.copy(alpha = 0.14f),
//                                    selectedLabelColor = PrimaryColor
//                                )
//                            )
//                        }
//                    }
//
//                    // optional custom interest + add button (mirror ProfileSetupScreen)
//                    Spacer(modifier = Modifier.height(8.dp))
//                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
//                        OutlinedTextField(
//                            value = ui.customInterest,
//                            onValueChange = { viewModel.onEvent(ProfileEvent.CustomInterestChanged(it)) },
//                            modifier = Modifier.weight(1f),
//                            label = { Text("Add interest", style = AppTypography.labelSmall) },
//                            singleLine = true
//                        )
//                        Spacer(modifier = Modifier.width(8.dp))
//                        Button(onClick = { viewModel.onEvent(ProfileEvent.AddCustomInterest) }, modifier = Modifier.heightIn(min = 56.dp)) {
//                            Text("Add")
//                        }
//                    }
//                    ui.customInterestError?.let { err ->
//                        Text(err, style = AppTypography.bodyLarge, color = MaterialTheme.colorScheme.error)
//                    }
//                }
//
//                // bottom spacer so content doesn't stick to bottom bar
//                Spacer(modifier = Modifier.height(88.dp))
//            }
//        }
//    )
//}
