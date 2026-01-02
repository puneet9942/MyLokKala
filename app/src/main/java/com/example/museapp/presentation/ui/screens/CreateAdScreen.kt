package com.example.museapp.presentation.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.indication
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Checkbox
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.google.accompanist.flowlayout.FlowRow
import com.example.museapp.presentation.feature.createad.CreateAdEvent
import com.example.museapp.presentation.feature.createad.CreateAdViewModel
import com.example.museapp.presentation.ui.components.appTextFieldColors
import com.example.museapp.ui.theme.AppTypography
import com.example.museapp.ui.theme.PrimaryColor
import com.example.museapp.util.CameraImagePicker
import kotlin.math.roundToInt
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.ui.input.pointer.pointerInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAdScreen(
    onBack: () -> Unit = {},
    viewModel: CreateAdViewModel = hiltViewModel()
) {
    val ui by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var addDialogOpen by remember { mutableStateOf(false) }

    // gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri>? ->
        uris?.let {
            val asNullable: List<Uri?> = it.map { u -> u as Uri? }
            viewModel.onEvent(CreateAdEvent.AddImages(asNullable))
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraImageUri != null) {
            viewModel.onEvent(CreateAdEvent.AddImageUri(cameraImageUri))
        }
    }

    // compute nav-bar bottom inset to reserve that space below the visible band
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Create Ad", style = AppTypography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            // Surface is the visible band (white). We keep the nav inset OUTSIDE this band
            Surface(
                color = Color.White,
                tonalElevation = 6.dp,
                shadowElevation = 0.dp
            ) {
                Column {
                    // subtle divider line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = { viewModel.onEvent(CreateAdEvent.Submit) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(28.dp)
                        ) {
                            if (ui.loading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text("Publish Ad", style = AppTypography.titleMedium, color = Color.White)
                        }
                    }

                    // reserve nav-bar height below the visible band so button never gets overlapped
                    Spacer(modifier = Modifier.height(navBarBottom))
                }
            }
        },
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.Top
            ) {
                // Ad title
                Text(
                    text = "Ad title",
                    style = AppTypography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = ui.title,
                    onValueChange = { viewModel.onEvent(CreateAdEvent.TitleChanged(it)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    placeholder = { /* none */ },
                    colors = appTextFieldColors()
                )
                ui.titleError?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(text = it, color = MaterialTheme.colorScheme.error, style = AppTypography.bodySmall)
                }

                Spacer(Modifier.height(14.dp))

                // Description
                Text(
                    text = "Description",
                    style = AppTypography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = ui.description,
                    onValueChange = { viewModel.onEvent(CreateAdEvent.DescriptionChanged(it)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    placeholder = { /* none */ },
                    colors = appTextFieldColors()
                )

                Spacer(Modifier.height(14.dp))

                // Skills
                Text(
                    text = "Skills (choose primary skill relevant to ad)",
                    style = AppTypography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(mainAxisSpacing = 8.dp, crossAxisSpacing = 8.dp) {
                    ui.availableSkills.forEach { skill ->
                        val selected = ui.selectedSkill == skill
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.onEvent(CreateAdEvent.SkillSelected(skill)) },
                            label = {
                                Text(
                                    text = skill,
                                    maxLines = 1,
                                    style = AppTypography.titleSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )
                            },
                            modifier = Modifier.height(36.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryColor.copy(alpha = 0.14f),
                                selectedLabelColor = PrimaryColor
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        )
                    }
                }
                ui.skillError?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(text = it, color = MaterialTheme.colorScheme.error, style = AppTypography.bodySmall)
                }

                Spacer(Modifier.height(14.dp))

                // Checkbox: aligned with left edge of fields
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = ui.wantToAddPrice,
                        onCheckedChange = { viewModel.onEvent(CreateAdEvent.WantToAddPriceToggled(it)) },
                        colors = androidx.compose.material.CheckboxDefaults.colors(checkedColor = PrimaryColor),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("I want to add price for my service", style = AppTypography.bodyLarge)
                }

                Spacer(Modifier.height(18.dp)) // extra spacing before pricing types

                // Pricing UI
                if (ui.wantToAddPrice) {
                    Text(text = "Pricing type", style = AppTypography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val chipModifier = Modifier.weight(1f).height(42.dp)
                        PricingOptionChip(
                            text = "Fixed",
                            selected = ui.pricingType == "FIXED",
                            onClick = { viewModel.onEvent(CreateAdEvent.PricingTypeSelected("FIXED")) },
                            modifier = chipModifier
                        )
                        PricingOptionChip(
                            text = "Hourly",
                            selected = ui.pricingType == "HOURLY",
                            onClick = { viewModel.onEvent(CreateAdEvent.PricingTypeSelected("HOURLY")) },
                            modifier = chipModifier
                        )
                        PricingOptionChip(
                            text = "Variable",
                            selected = ui.pricingType == "VARIABLE",
                            onClick = { viewModel.onEvent(CreateAdEvent.PricingTypeSelected("VARIABLE")) },
                            modifier = chipModifier
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    when (ui.pricingType) {
                        "FIXED", "HOURLY" -> {
                            Text(text = "Standard price (INR)", style = AppTypography.titleMedium)
                            Spacer(Modifier.height(6.dp))
                            OutlinedTextField(
                                value = ui.standardPrice,
                                onValueChange = { viewModel.onEvent(CreateAdEvent.StandardPriceChanged(it)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = appTextFieldColors()
                            )
                            ui.priceError?.let {
                                Spacer(Modifier.height(6.dp))
                                Text(text = it, color = MaterialTheme.colorScheme.error, style = AppTypography.bodySmall)
                            }
                        }
                        "VARIABLE" -> {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "Min (INR)", style = AppTypography.titleMedium)
                                    Spacer(Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = ui.minPrice,
                                        onValueChange = { viewModel.onEvent(CreateAdEvent.MinPriceChanged(it)) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 52.dp),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = appTextFieldColors()
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "Max (INR)", style = AppTypography.titleMedium)
                                    Spacer(Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = ui.maxPrice,
                                        onValueChange = { viewModel.onEvent(CreateAdEvent.MaxPriceChanged(it)) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 52.dp),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = appTextFieldColors()
                                    )
                                }
                            }
                            ui.priceError?.let {
                                Spacer(Modifier.height(6.dp))
                                Text(text = it, color = MaterialTheme.colorScheme.error, style = AppTypography.bodySmall)
                            }
                        }
                        else -> { /* nothing */ }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                Text(text = "Travel radius (in km)", style = AppTypography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(Modifier.height(6.dp))

                val density = LocalDensity.current
                var trackWidthPx by remember { mutableStateOf(1f) }
                var isSliding by remember { mutableStateOf(false) }

                val trackTopDp = 24.dp
                val trackHeight = 10.dp

                // helper to map x -> 0..100 int
                fun xToKm(x: Float, totalPx: Float): Int {
                    if (totalPx <= 0f) return 0
                    val v = (x / totalPx) * 100f
                    return v.coerceIn(0f, 100f).roundToInt()
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .onGloballyPositioned { coords ->
                            trackWidthPx = coords.size.width.toFloat()
                        }
                ) {
                    // endpoints
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("0", style = AppTypography.bodySmall)
                        Text("100", style = AppTypography.bodySmall)
                    }

                    // track background + progress
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = trackTopDp)
                            .height(trackHeight)
                            .clip(RoundedCornerShape(24.dp))
                            .background(PrimaryColor.copy(alpha = 0.18f))
                    ) {
                        val fraction = (ui.travelRadiusKm.coerceIn(0, 100).toFloat()) / 100f
                        val progWidth = (fraction * trackWidthPx).coerceIn(0f, trackWidthPx)
                        Box(
                            modifier = Modifier
                                .height(trackHeight)
                                .width(with(density) { progWidth.toDp() })
                                .clip(RoundedCornerShape(24.dp))
                                .background(PrimaryColor)
                        )
                    }

                    // interactive overlay: taps and drags (no Slider, no ripple)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = trackTopDp)
                            .height(trackHeight)
                            // handle taps
                            .pointerInput(trackWidthPx) {
                                detectTapGestures { offset ->
                                    val km = xToKm(offset.x, trackWidthPx)
                                    viewModel.onEvent(CreateAdEvent.TravelRadiusChanged(km))
                                }
                            }
                            // handle drags
                            .pointerInput(trackWidthPx) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        isSliding = true
                                        val km = xToKm(offset.x, trackWidthPx)
                                        viewModel.onEvent(CreateAdEvent.TravelRadiusChanged(km))
                                    },
                                    onDrag = { change, _ ->
                                        // position is in local coordinates
                                        val x = change.position.x
                                        val km = xToKm(x, trackWidthPx)
                                        viewModel.onEvent(CreateAdEvent.TravelRadiusChanged(km))
                                        change.consume()
                                    },
                                    onDragEnd = { isSliding = false },
                                    onDragCancel = { isSliding = false }
                                )
                            }
                    )

                    // compute thumb center
                    val fractionDot = (ui.travelRadiusKm.coerceIn(0, 100).toFloat()) / 100f
                    val thumbCenterX = (fractionDot * trackWidthPx).coerceIn(0f, trackWidthPx)

                    val trackTopPx = with(density) { trackTopDp.toPx() }
                    val trackHeightPx = with(density) { trackHeight.toPx() }
                    val thumbCenterY = trackTopPx + trackHeightPx / 2f

                    // thumb visuals (outer circle + inner white dot)
                    val thumbOuterDp = 16.dp
                    val thumbOuterPx = with(density) { thumbOuterDp.toPx() }
                    val innerDotDp = 8.dp

                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    x = (thumbCenterX - thumbOuterPx / 2f).roundToInt(),
                                    y = (thumbCenterY - thumbOuterPx / 2f).roundToInt()
                                )
                            }
                            .size(thumbOuterDp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(thumbOuterDp)
                                .background(color = PrimaryColor, shape = CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(innerDotDp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }

                    // floating value bubble (clamped to edges), slightly larger while sliding
                    val bubbleWidth = if (isSliding) 64.dp else 56.dp
                    val bubbleHeight = if (isSliding) 36.dp else 32.dp
                    val bubbleWidthPx = with(density) { bubbleWidth.toPx() }
                    val clampedCenterX = min(max(thumbCenterX, bubbleWidthPx / 2f), trackWidthPx - bubbleWidthPx / 2f)

                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    x = (clampedCenterX - bubbleWidthPx / 2f).roundToInt(),
                                    y = (-with(density) { bubbleHeight.toPx() }).roundToInt()
                                )
                            }
                            .size(width = bubbleWidth, height = bubbleHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            tonalElevation = 6.dp,
                            color = PrimaryColor
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = bubbleWidth, height = bubbleHeight)
                                    .padding(6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "${ui.travelRadiusKm} km", style = AppTypography.bodyMedium, color = Color.White)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                // Images header and add button
                Text(
                    text = "Showcase your Art",
                    style = AppTypography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(8.dp))

                if (ui.imageUris.size < 5) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Surface(
                            onClick = { addDialogOpen = true },
                            shape = RoundedCornerShape(12.dp),
                            color = PrimaryColor,
                            modifier = Modifier
                                .size(width = 140.dp, height = 56.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text("Add Image", style = AppTypography.titleSmall, color = Color.White)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Image duplicate error
                ui.imageError?.let { err ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(text = err, color = MaterialTheme.colorScheme.error, style = AppTypography.bodySmall, modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.onEvent(CreateAdEvent.ClearImageError) }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                if (ui.imageUris.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        items(ui.imageUris.size) { idx ->
                            val uri = ui.imageUris[idx]
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))),
                                contentAlignment = Alignment.TopEnd
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(uri),
                                    contentDescription = "Selected image",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                IconButton(
                                    onClick = { viewModel.onEvent(CreateAdEvent.RemoveImageAt(idx)) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Remove", tint = Color.White)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                } else {
                    Text(
                        text = "No images yet — add examples of your work",
                        style = AppTypography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.height(8.dp))
                }

                // spacer so content isn't hidden by bottom bar
                Spacer(modifier = Modifier.height(72.dp))
            }

            // Add dialog (camera/gallery options)
            if (addDialogOpen) {
                AlertDialog(
                    onDismissRequest = { addDialogOpen = false },
                    title = { Text(text = "Add image", style = AppTypography.titleMedium) },
                    text = { Text("Choose an option to add a photo", style = AppTypography.bodyLarge) },
                    confirmButton = {
                        TextButton(onClick = {
                            galleryLauncher.launch("image/*")
                            addDialogOpen = false
                        }) {
                            Text("Choose from gallery", style = AppTypography.titleMedium)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            cameraImageUri = CameraImagePicker.createImageUri(context)
                            cameraImageUri?.let { cameraLauncher.launch(it) }
                            addDialogOpen = false
                        }) {
                            Text("Take photo", style = AppTypography.titleMedium)
                        }
                    }
                )
            }
        }
    )
}

/** Reusable pricing option chip - consistent look with the app */
@Composable
private fun PricingOptionChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = if (selected) 2.dp else 0.dp,
        color = if (selected) PrimaryColor.copy(alpha = 0.18f) else Color.White,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
            Text(text = text, style = AppTypography.titleSmall, color = if (selected) PrimaryColor else MaterialTheme.colorScheme.onSurface)
        }
    }
}
