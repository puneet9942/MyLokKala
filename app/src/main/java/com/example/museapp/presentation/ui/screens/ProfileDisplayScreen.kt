package com.example.museapp.presentation.feature.profile

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import java.io.File
import java.text.NumberFormat
import java.util.*

@Composable
fun ProfileDisplayScreen(
    viewModel: ProfileDisplayViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    if (state.loading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        // DEBUG: show raw mainPhoto string so you can see why image doesn't load
       // Text(text = "rawMainPhoto: ${state.rawMainPhoto ?: "-"}", modifier = Modifier.padding(bottom = 8.dp))
        Text(text = "mainPhoto(normalized): ${state.mainPhoto ?: "-"}", modifier = Modifier.padding(bottom = 12.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        ) {
            ProfileImage(imageSource = state.mainPhoto, modifier = Modifier.fillMaxSize())
        }

        Spacer(modifier = Modifier.height(12.dp))

        FieldRow(label = "Name", value = state.fullName)
        FieldRow(label = "Title", value = state.profileDescription)
        FieldRow(label = "Bio", value = state.bio)
        FieldRow(label = "Phone", value = state.phone)
        FieldRow(label = "DOB", value = state.dob)
        FieldRow(label = "Gender", value = state.gender)
        FieldRow(label = "Event manager", value = if (state.isEventManager) "Yes" else "No")

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        FieldRow(label = "Latitude", value = state.lat?.toString())
        FieldRow(label = "Longitude", value = state.lng?.toString())

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        FieldRow(label = "Pricing type", value = state.pricingType)
        FieldRow(
            label = "Standard price",
            value = state.standardPrice?.let { NumberFormat.getCurrencyInstance().format(it) }
        )
        FieldRow(label = "Price min", value = state.priceMin?.let { String.format(Locale.getDefault(), "%.2f", it) })
        FieldRow(label = "Price max", value = state.priceMax?.let { String.format(Locale.getDefault(), "%.2f", it) })
        FieldRow(label = "Travel radius (km)", value = state.travelRadius?.let { String.format(Locale.getDefault(), "%.1f", it) })

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        FieldRow(label = "Facebook", value = state.facebookId)
        FieldRow(label = "Twitter", value = state.twitterId)
        FieldRow(label = "Instagram", value = state.instagramId)
        FieldRow(label = "LinkedIn", value = state.linkedinId)
        FieldRow(label = "YouTube", value = state.youtubeId)

        val ratingText = buildString {
            append(state.averageRating?.let { String.format(Locale.getDefault(), "%.1f", it) } ?: "-")
            state.totalRatings?.let { append(" ($it)") }
        }
        FieldRow(label = "Rating", value = ratingText)

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        // DEBUG: show raw interests JSON
      //  Text(text = "rawInterestsJson: ${state.rawInterestsJson ?: "-"}", modifier = Modifier.padding(vertical = 6.dp))

        Text(
            text = "Interests: ${
                if (state.interests.isEmpty()) "-" else state.interests.joinToString { it.name ?: it.remoteId ?: it.id?.toString() ?: "-" }
            }"
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (state.photos.isNotEmpty()) {
            Text(text = "Photos")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 8.dp)
            ) {
                for (p in state.photos) {
                    Card(
                        modifier = Modifier
                            .size(100.dp)
                            .padding(end = 8.dp)
                            .clickable { /* TODO: show full image */ }
                    ) {
                        ProfileImage(imageSource = p, modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (state.videos.isNotEmpty()) {
            Text(text = "Videos")
            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                state.videos.forEach { v ->
                    Text(
                        text = v,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { /* TODO: open video player */ })
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ProfileImage(imageSource: String?, modifier: Modifier = Modifier) {
    if (imageSource.isNullOrBlank()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(text = "No image", modifier = Modifier.padding(8.dp))
        }
        return
    }

    // handle base64 data URIs
    if (imageSource.startsWith("data:", ignoreCase = true) && imageSource.contains("base64,")) {
        val base64Part = imageSource.substringAfter("base64,", "")
        val bitmap = remember(base64Part) {
            try {
                val bytes = Base64.decode(base64Part, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (t: Throwable) {
                null
            }
        }
        if (bitmap != null) {
            Image(bitmap = bitmap.asImageBitmap(), contentDescription = "Profile image", modifier = modifier, contentScale = ContentScale.Crop)
            return
        } else {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                Text(text = "Image decode failed", modifier = Modifier.padding(8.dp))
            }
            return
        }
    }

    // build model for Coil
    val painterModel: Any = when {
        imageSource.startsWith("content://", ignoreCase = true) -> Uri.parse(imageSource)
        imageSource.startsWith("file://", ignoreCase = true) -> Uri.parse(imageSource)
        imageSource.startsWith("/") -> File(imageSource)             // absolute path
        imageSource.startsWith("http://", ignoreCase = true) -> imageSource
        imageSource.startsWith("https://", ignoreCase = true) -> imageSource
        // server-relative path like "/media/abc.jpg" or plain "abc.jpg"
        // If you have a base URL for your server, set it here:
        else -> {
            // TODO: replace with your server base URL if you have one, e.g. "https://api.myserver.com"
            val baseUrl: String? = null
            if (!baseUrl.isNullOrBlank()) {
                if (imageSource.startsWith("/")) baseUrl.trimEnd('/') + imageSource else "$baseUrl/${imageSource}"
            } else {
                // fallback: pass raw string through (Coil likely will fail, but debug UI shows the raw value)
                imageSource
            }
        }
    }

    Image(
        painter = rememberAsyncImagePainter(model = painterModel),
        contentDescription = "Profile image",
        modifier = modifier,
        contentScale = ContentScale.Crop
    )
}

@Composable
private fun FieldRow(label: String, value: String?) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(text = "$label:", modifier = Modifier.width(140.dp))
        Text(text = value.takeIf { !it.isNullOrBlank() } ?: "-")
    }
}
