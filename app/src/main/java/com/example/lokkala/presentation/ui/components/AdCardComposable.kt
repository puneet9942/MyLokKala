package com.example.lokkala.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.lokkala.domain.model.Ad
import com.example.lokkala.util.DistanceUtils
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Arrangement

@Composable
fun AdCardComposable(
    ad: Ad,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onViewDetails: () -> Unit,
    currentLat: Double,
    currentLng: Double
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .height(180.dp)
                    .fillMaxWidth()
            ) {
                AsyncImage(
                    model = ad.user.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF27B4B)
                ) {
                    Text(
                        text = ad.primarySkill,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        color = Color.White
                    )
                }

                IconButton(
                    onClick = onFavoriteToggle,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.9f))
                ) {
                    if (isFavorite) {
                        Icon(Icons.Filled.Favorite, contentDescription = "fav", tint = Color.Red)
                    } else {
                        Icon(Icons.Outlined.FavoriteBorder, contentDescription = "not fav", tint = Color.DarkGray)
                    }
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = ad.user.name,
                    fontWeight = FontWeight.Bold
                )
                if (!ad.user.subtitle.isNullOrBlank()) {
                    Text(text = ad.user.subtitle, color = Color.Gray)
                }

                Spacer(modifier = Modifier.height(8.dp))

                val km = DistanceUtils.distanceKm(currentLat, currentLng, ad.user.lat, ad.user.lng)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = DistanceUtils.formatDistance(km))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "★ ${ad.user.rating} (${ad.user.reviewsCount})")
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "₹${ad.priceMin} - ₹${ad.priceMax}", color = Color(0xFFF27B4B))
                    Text(
                        text = "View Details →",
                        modifier = Modifier.clickable { onViewDetails() },
                        color = Color(0xFFF27B4B)
                    )
                }
            }
        }
    }
}