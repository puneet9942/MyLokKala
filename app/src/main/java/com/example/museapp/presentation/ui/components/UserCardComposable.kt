package com.example.museapp.presentation.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.museapp.domain.model.User
import com.example.museapp.ui.theme.AppTypography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import com.example.museapp.util.DistanceUtils
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.zIndex
import androidx.compose.ui.res.painterResource
import com.example.museapp.R

/**
 * Compact, left-image user card with improved spacing and consistent height.
 *
 * UI-only. Preserves callbacks and variable names used across the app.
 *
 * Note: uses the passed-in `profileDescription` parameter when non-blank, otherwise
 * falls back to user.profileDescription. Sanitizes text and always renders the description area.
 */
@Composable
fun UserCardComposable(
    user: User,
    distanceKm: Double?,                     // precomputed distance (optional)
    onFavorite: () -> Unit,
    onViewProfile: () -> Unit,
    currentLat: Double? = null,
    currentLng: Double? = null,
    isFavorite: Boolean = false,
    selectedSkill: String = "",
    profileDescription: String = "", // prefer this param when provided
    knownSkills: List<String> = emptyList()  // list of known skills (for "Other")
) {
    val imageUrl = user.photo
    val title = user.fullName ?: "Unknown"

    // choose text from parameter first, otherwise from user domain model
    val rawProfileSource = if (profileDescription.isNotBlank()) profileDescription else (user.profileDescription ?: "")

    // sanitize the profile description (remove hidden chars / newlines, collapse whitespace)
    val sanitizedProfileDescription = rawProfileSource
        .replace("\u00AD", " ") // soft hyphen
        .replace("\u200B", " ") // zero width space
        .replace("\u200C", " ") // zero width non-joiner
        .replace("\u200D", " ") // zero width joiner
        .replace("\n", " ")
        .replace("\r", " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    // Always render description area (may be blank). Styled smaller and muted.
    val profileTextToDisplay = sanitizedProfileDescription

    val priceRange = if (user.priceMin != null && user.priceMax != null) "₹${user.priceMin} - ₹${user.priceMax}" else ""

    // final distance: use provided distanceKm, otherwise compute using current coords if available
    val finalDistance: Double? = distanceKm ?: run {
        val lat = user.lat
        val lng = user.lng
        if (lat != null && lng != null && currentLat != null && currentLng != null) {
            DistanceUtils.distanceKm(currentLat, currentLng, lat, lng)
        } else null
    }

    // normalize and dedupe user's interest names
    val allInterestNames: List<String> = user.interests
        .mapNotNull { it.name?.trim() }
        .filter { it.isNotEmpty() }
        .distinctBy { it.lowercase() }

    // determine which interests to show (kept same logic as before)
    val displayedInterests: List<String> = when {
        selectedSkill.isBlank() || selectedSkill.equals("All", ignoreCase = true) -> {
            allInterestNames.take(3) // compact: show up to 3
        }
        selectedSkill.equals("Other", ignoreCase = true) -> {
            val knownLower = knownSkills.map { it.trim().lowercase() }.toSet()
            if (knownLower.isEmpty()) allInterestNames.take(3)
            else allInterestNames.filter { it.trim().lowercase() !in knownLower }.take(3)
        }
        else -> {
            val sel = selectedSkill.trim()
            allInterestNames.filter { it.equals(sel, ignoreCase = true) }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable { onViewProfile() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        // Reduced internal padding and minHeight for compact look
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF4EEF6))
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .defaultMinSize(minHeight = 112.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: profile image box (reduced size to 112dp)
            Box(
                modifier = Modifier
                    .size(width = 112.dp, height = 112.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF5E35B1)),
                contentAlignment = Alignment.TopEnd
            ) {
                if (!imageUrl.isNullOrBlank()) {
                    Image(
                        painter = rememberAsyncImagePainter(imageUrl),
                        contentDescription = title,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // fallback to app launcher icon for now
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "placeholder",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                // Favorite heart overlay on image (slightly smaller)
                IconButton(
                    onClick = onFavorite,
                    modifier = Modifier
                        .padding(6.dp)
                        .size(30.dp)
                        .background(Color.White.copy(alpha = 0.98f), shape = CircleShape)
                        .zIndex(1f)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Right column: content with tighter spacing
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Title (name)
                Text(
                    text = title,
                    style = AppTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Profile description — smaller, muted
                Text(
                    text = profileTextToDisplay,
                    style = AppTypography.bodySmall.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                // Chips row (smaller chips)
                val interestsToShow = displayedInterests
                if (interestsToShow.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        interestsToShow.forEach { text ->
                            if (text.isNotBlank()) {
                                Surface(
                                    color = Color.White.copy(alpha = 0.96f),
                                    shape = RoundedCornerShape(8.dp),
                                    tonalElevation = 0.dp,
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 24.dp)
                                ) {
                                    Text(
                                        text = text,
                                        style = AppTypography.bodySmall.copy(fontSize = 12.sp),
                                        color = Color(0xFF5E35B1),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // Distance + rating row — compact & aligned
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    finalDistance?.let { d ->
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = "distance",
                            tint = Color(0xFF5E35B1),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(String.format("%.1f km", d), style = AppTypography.bodySmall)
                        Spacer(modifier = Modifier.width(10.dp))
                    }

                    val rating = user.averageRating ?: 0.0
                    val reviews = user.totalRatings ?: 0
                    if (rating > 0.0 || reviews > 0) {
                        Text("★ ${"%.1f".format(rating)}", style = AppTypography.bodySmall)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("($reviews)", style = AppTypography.bodySmall)
                    }
                }

                // Price (left) + View button (right) on a single reduced row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (priceRange.isNotBlank()) {
                        Text(
                            text = priceRange,
                            style = AppTypography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFF5E35B1),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    TextButton(
                        onClick = onViewProfile,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("View", style = AppTypography.bodySmall, color = Color(0xFF5E35B1))
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "go",
                                tint = Color(0xFF5E35B1),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
