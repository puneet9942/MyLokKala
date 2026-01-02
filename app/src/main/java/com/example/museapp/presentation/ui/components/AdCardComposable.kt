package com.example.museapp.presentation.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.museapp.domain.model.Ad
import com.example.museapp.ui.theme.AppTypography
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow

/**
 * A defensive ad card that works even if some fields are null or named differently.
 */
@Composable
fun AdCardComposable(
    ad: Ad,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onViewDetails: () -> Unit,
    currentLat: Double?,
    currentLng: Double?,
    backgroundColor: Color = Color.Transparent
) {
    // Determine image URL - try a few likely fields
//    val imageUrl = ad.imageUrl ?: ad.user?.photo ?: ad.photos?.firstOrNull()
//
//    val title = ad.title ?: ad.user?.fullName ?: "Unknown"
//    val subtitle = ad.subtitle ?: ad.user?.profileDescription ?: ad.profileDescription ?: ""
//    val priceRange = if (ad.priceMin != null && ad.priceMax != null) {
//        "₹${ad.priceMin} - ₹${ad.priceMax}"
//    } else ad.price ?: ""
//
//    val rating = ad.averageRating ?: ad.rating ?: 0.0
//    val reviewsCount = ad.totalRatings ?: ad.reviewsCount ?: 0
//
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .clickable { onViewDetails() }
//            .background(backgroundColor),
//        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
//    ) {
//        Column(modifier = Modifier.fillMaxWidth()) {
//            // Image header
//            if (!imageUrl.isNullOrBlank()) {
//                Image(
//                    painter = rememberAsyncImagePainter(imageUrl),
//                    contentDescription = title,
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .height(160.dp),
//                    contentScale = ContentScale.Crop
//                )
//            } else {
//                Box(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .height(160.dp)
//                        .background(Color.LightGray)
//                )
//            }
//
//            Spacer(modifier = Modifier.height(8.dp))
//
//            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
//                Text(title, style = AppTypography.titleMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
//                if (subtitle.isNotBlank()) {
//                    Spacer(modifier = Modifier.height(4.dp))
//                    Text(subtitle, style = AppTypography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
//                }
//
//                Spacer(modifier = Modifier.height(8.dp))
//
//                Row(verticalAlignment = Alignment.CenterVertically) {
//                    // Location distance handled by Detail/consumer; placeholder here
//                    if (rating > 0.0) {
//                        Text("⭐ $rating", style = AppTypography.bodyMedium)
//                        Spacer(modifier = Modifier.width(8.dp))
//                        Text("($reviewsCount)", style = AppTypography.bodySmall)
//                    }
//                }
//
//                Spacer(modifier = Modifier.height(12.dp))
//
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceBetween,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Text(priceRange, style = AppTypography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
//                    IconButton(onClick = onFavoriteToggle) {
//                        Icon(
//                            imageVector = Icons.Default.FavoriteBorder,
//                            contentDescription = "fav"
//                        )
//                    }
//                }
//            }
//        }
//    }
}
