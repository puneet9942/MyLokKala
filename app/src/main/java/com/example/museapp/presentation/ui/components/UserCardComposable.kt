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

@Composable
fun UserCardComposable(
    user: User,
    distanceKm: Double?,                     // precomputed distance (optional)
    onFavorite: () -> Unit,
    onViewProfile: () -> Unit,
    currentLat: Double? = null,
    currentLng: Double? = null,
    isFavorite: Boolean = false,
    selectedSkill: String = "",              // selected skill from chip bar
    knownSkills: List<String> = emptyList()  // list of known skills (for "Other")
) {
    val imageUrl = user.photo
    val title = user.fullName ?: "Unknown"
    val subtitle = user.profileDescription ?: ""
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

    // MINIMAL logic: when a specific skill is selected, show only that one interest for the user.
    val displayedInterests: List<String> = when {
        // blank or "All" -> original behavior (show up to 4)
        selectedSkill.isBlank() || selectedSkill.equals("All", ignoreCase = true) -> {
            allInterestNames.take(4)
        }

        // "Other" -> show interests not in knownSkills
        selectedSkill.equals("Other", ignoreCase = true) -> {
            val knownLower = knownSkills.map { it.trim().lowercase() }.toSet()
            if (knownLower.isEmpty()) allInterestNames.take(4)
            else allInterestNames.filter { it.trim().lowercase() !in knownLower }.take(4)
        }

        // specific skill selected -> show only that interest (if user has it), otherwise show nothing
        else -> {
            val sel = selectedSkill.trim()
            allInterestNames.filter { it.equals(sel, ignoreCase = true) }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewProfile() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Image area with overlays
            Box(modifier = Modifier.fillMaxWidth()) {
                if (!imageUrl.isNullOrBlank()) {
                    Image(
                        painter = rememberAsyncImagePainter(imageUrl),
                        contentDescription = title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // placeholder purple header (keeps previous look)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                            .background(Color(0xFF5E35B1))
                    )
                }

                // Heart icon top-right (overlay)
                IconButton(
                    onClick = onFavorite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.95f), shape = RoundedCornerShape(20.dp))
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // chips row overlapping image bottom-left
                val interestsToShow = displayedInterests
                if (interestsToShow.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 12.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        interestsToShow.forEach { text ->
                            if (text.isNotBlank()) {
                                Surface(
                                    color = Color.White.copy(alpha = 0.88f),
                                    shape = RoundedCornerShape(8.dp),
                                    tonalElevation = 0.dp,
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 32.dp)
                                ) {
                                    Text(
                                        text = text,
                                        style = AppTypography.bodyMedium.copy(fontSize = 14.sp),
                                        color = Color(0xFF5E35B1),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Details area below image
            Column(modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF4EEF6))
                .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = title,
                    style = AppTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = subtitle,
                        style = AppTypography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // distance + rating row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    finalDistance?.let { d ->
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = "distance",
                            tint = Color(0xFF5E35B1),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(String.format("%.1f km", d), style = AppTypography.bodyMedium)
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    val rating = user.averageRating ?: 0.0
                    val reviews = user.totalRatings ?: 0
                    if (rating > 0.0 || reviews > 0) {
                        Text("★ ${"%.1f".format(rating)}", style = AppTypography.bodyMedium)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("($reviews)", style = AppTypography.bodySmall)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Divider(color = Color.LightGray, thickness = 1.dp)

                Spacer(modifier = Modifier.height(12.dp))

                // bottom row: price left, view profile right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = priceRange,
                        style = AppTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF5E35B1)
                    )

                    TextButton(onClick = onViewProfile) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("View Profile", style = AppTypography.bodyMedium, color = Color(0xFF5E35B1))
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "go", tint = Color(0xFF5E35B1))
                        }
                    }
                }
            }
        }
    }
}
