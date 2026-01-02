package com.example.museapp.presentation.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.museapp.domain.model.Ad
import com.example.museapp.ui.theme.AppTypography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color

@Composable
fun DetailsScreen(ad: Ad, onBack: () -> Unit = {}) {
//    val imageUrl = ad.imageUrl ?: ad.user?.photo ?: ad.photos?.firstOrNull()
//    val title = ad.title ?: ad.user?.fullName ?: "Unknown"
//    val subtitle = ad.subtitle ?: ad.user?.profileDescription ?: ad.profileDescription ?: ""
//    val rating = ad.averageRating ?: ad.rating ?: 0.0
//    val reviewsCount = ad.totalRatings ?: ad.reviewsCount ?: 0
//
//    Scaffold(topBar = {
//        TopAppBar(title = { Text(title) }, navigationIcon = {
//            TextButton(onClick = onBack) { Text("Back") }
//        })
//    }) { padding ->
//        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
//            if (!imageUrl.isNullOrBlank()) {
//                Image(
//                    painter = rememberAsyncImagePainter(imageUrl),
//                    contentDescription = title,
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .height(220.dp)
//                )
//            } else {
//                Box(modifier = Modifier
//                    .fillMaxWidth()
//                    .height(220.dp)
//                    .background(Color.LightGray))
//            }
//
//            Spacer(modifier = Modifier.height(12.dp))
//
//            Column(modifier = Modifier.padding(12.dp)) {
//                Text(title, style = AppTypography.titleLarge.copy(fontWeight = FontWeight.Bold), maxLines = 2, overflow = TextOverflow.Ellipsis)
//                if (subtitle.isNotBlank()) {
//                    Spacer(modifier = Modifier.height(8.dp))
//                    Text(subtitle, style = AppTypography.bodyLarge)
//                }
//
//                Spacer(modifier = Modifier.height(12.dp))
//                Row(verticalAlignment = Alignment.CenterVertically) {
//                    Text("Rating: $rating", style = AppTypography.bodyLarge)
//                    Spacer(modifier = Modifier.width(8.dp))
//                    Text("($reviewsCount reviews)", style = AppTypography.bodyMedium)
//                }
//            }
//        }
//    }
}
