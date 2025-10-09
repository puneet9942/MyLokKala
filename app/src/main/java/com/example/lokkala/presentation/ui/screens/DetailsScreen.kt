package com.example.lokkala.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lokkala.presentation.feature.home.HomeViewModel
import coil.compose.AsyncImage
import androidx.compose.material3.Button

@Composable
fun DetailsScreen(
    adId: String,
    homeViewModel: HomeViewModel,
    onBack: () -> Unit = {}
) {
    // Look up ad synchronously from the ViewModel's in-memory list
    val ad = remember(adId) { homeViewModel.findAdById(adId) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = onBack) { Text("Back") }
        Spacer(modifier = Modifier.height(12.dp))
        if (ad == null) {
            Text("Ad not found")
        } else {
            AsyncImage(model = ad.user.imageUrl, contentDescription = null, modifier = Modifier.fillMaxWidth().height(220.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = ad.user.name)
            Text(text = ad.primarySkill)
            Text(text = "₹${ad.priceMin} - ₹${ad.priceMax}")
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = ad.description ?: "")
        }
    }
}