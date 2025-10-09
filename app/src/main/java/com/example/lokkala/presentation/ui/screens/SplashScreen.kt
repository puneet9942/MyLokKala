package com.example.lokkala.presentation.ui.screens

import com.example.lokkala.R // Added this import
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinish: () -> Unit) {
    LaunchedEffect(Unit) { delay(800); onFinish() }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_background), // Use your actual image name
            contentDescription = "Splash background",
            modifier = Modifier.fillMaxSize()
        )
        // If you want to overlay a logo or app name, add another Box here
        // Example:
        // Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        //     Text("LocalArtists", ...)
        // }
    }
}