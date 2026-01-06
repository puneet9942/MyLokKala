package com.example.museapp.presentation.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.museapp.domain.model.CacheUser
import com.example.museapp.presentation.feature.profile.ProfileCacheEvent
import com.example.museapp.presentation.profile.ProfileCacheDisplayViewModel
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

/**
 * Fixed LazyColumn / LazyRow usage to avoid "item(...) cannot be called in this context"
 * and to ensure the screen is scrollable and displays media and all user attributes.
 *
 * Requires Coil compose dependency for AsyncImage:
 * implementation("io.coil-kt:coil-compose:2.4.0")
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileCacheDisplayScreen(
    viewModel: ProfileCacheDisplayViewModel = hiltViewModel(),
    onUpdate: () -> Unit = { viewModel.onEvent(ProfileCacheEvent.RefreshProfile) }
) {
    val state = viewModel.uiState.collectAsState().value
    val ctx = LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "Profile") },
                actions = {
                    Button(onClick = onUpdate, modifier = Modifier.padding(end = 8.dp)) {
                        Text(text = "Update")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            if (state.isLoading) {
                item {
                    CircularProgressIndicator()
                }
                return@LazyColumn
            }

            if (state.error != null) {
                item {
                    Text(text = "Error: ${state.error}")
                    Button(
                        onClick = { viewModel.onEvent(ProfileCacheEvent.Retry(state.error)) },
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        Text(text = "Retry")
                    }
                }
                return@LazyColumn
            }

            if (state.user == null) {
                item {
                    Text(text = "No profile available")
                    Button(
                        onClick = { viewModel.onEvent(ProfileCacheEvent.LoadProfile) },
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        Text(text = "Load Profile")
                    }
                }
                return@LazyColumn
            }

            // From here: state.user != null
            val user = state.user

            // Avatar
            item {
                val avatarUrl = findFirstStringAttribute(
                    user,
                    listOf("avatar", "avatarUrl", "profilePic", "profile_picture", "photo", "picture", "imageUrl")
                )
                if (!avatarUrl.isNullOrBlank()) {
                    Surface(
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .height(128.dp)
                            .aspectRatio(1f),
                        shape = CircleShape
                    ) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "Profile picture",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(CircleShape)
                        )
                    }
                }
            }

            // Images carousel
            val imageList = findCollectionOfStrings(user, listOf("images", "photos", "gallery", "imageUrls", "photoUrls"))
            if (!imageList.isNullOrEmpty()) {
                item {
                    Text(text = "Images", modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
                }
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(imageList) { url ->
                            Card(
                                modifier = Modifier
                                    .height(140.dp)
                                    .aspectRatio(1f)
                                    .clickable {
                                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    }
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = "Image",
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            // Videos list
            val videoList = findCollectionOfStrings(user, listOf("videos", "videoUrls", "video_links", "videoUrlsList"))
            if (!videoList.isNullOrEmpty()) {
                item {
                    Text(text = "Videos", modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
                }
                items(videoList) { videoUrl ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable {
                                try {
                                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl)))
                                } catch (_: Throwable) {
                                }
                            }
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                            // try to load thumbnail (if URL is image); otherwise coil will show placeholder
                            AsyncImage(
                                model = videoUrl,
                                contentDescription = "Video thumbnail",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                            )
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                }
            }

            // All other attributes
            item {
                ProfileCacheReadOnlyContentAllAttributes(user = user, isFromCache = state.isFromCache)
            }
        }
    }
}

/**
 * Render all readable attributes (excluding media) as read-only fields.
 * Reflection-based; skips media properties that are handled above.
 */
@Composable
private fun ProfileCacheReadOnlyContentAllAttributes(user: CacheUser, isFromCache: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(PaddingValues(8.dp)),
        horizontalAlignment = Alignment.Start
    ) {
        OutlinedTextField(
            value = if (isFromCache) "Cached" else "Network",
            onValueChange = { /* read-only */ },
            label = { Text("Source") },
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
        )

        val props = try {
            user::class.memberProperties
        } catch (t: Throwable) {
            emptyList<KProperty1<CacheUser, *>>()
        }

        val skip = setOf(
            "images", "photos", "gallery", "imageUrls", "photoUrls",
            "videos", "videoUrls", "avatar", "avatarUrl", "profilePic",
            "profile_picture", "photo", "picture", "imageUrl"
        )

        props.sortedBy { it.name }.forEach { prop ->
            if (skip.contains(prop.name)) return@forEach

            val rawValue = try {
                @Suppress("UNCHECKED_CAST")
                (prop as KProperty1<CacheUser, *>).get(user)
            } catch (t: Throwable) {
                null
            }

            val formatted = formatAttributeValue(rawValue)
            if (formatted.isNullOrBlank()) return@forEach

            OutlinedTextField(
                value = formatted,
                onValueChange = { /* read-only */ },
                label = { Text(camelToLabel(prop.name)) },
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            )
        }
    }
}

// ---------- helper functions (same as before) ----------

private fun findFirstStringAttribute(user: CacheUser, names: List<String>): String? {
    val props = try { user::class.memberProperties } catch (_: Throwable) { emptyList<KProperty1<CacheUser, *>>() }
    for (n in names) {
        val p = props.find { it.name.equals(n, ignoreCase = true) } ?: continue
        val raw = try { p.getter.call(user) } catch (_: Throwable) { null }
        val formatted = formatAttributeValue(raw)
        if (!formatted.isNullOrBlank()) return formatted
    }
    return null
}

private fun findCollectionOfStrings(user: CacheUser, names: List<String>): List<String>? {
    val props = try { user::class.memberProperties } catch (_: Throwable) { emptyList<KProperty1<CacheUser, *>>() }
    for (n in names) {
        val p = props.find { it.name.equals(n, ignoreCase = true) } ?: continue
        val raw = try { p.getter.call(user) } catch (_: Throwable) { null }
        when (raw) {
            is Collection<*> -> {
                val list = raw.mapNotNull { formatAttributeValue(it) }.filter { it.isNotBlank() }
                if (list.isNotEmpty()) return list
            }
            is Array<*> -> {
                val list = raw.mapNotNull { formatAttributeValue(it) }.filter { it.isNotBlank() }
                if (list.isNotEmpty()) return list
            }
            is String -> {
                val parts = raw.split(',').map { it.trim() }.filter { it.isNotBlank() }
                if (parts.isNotEmpty()) return parts
            }
        }
    }
    return null
}

private fun formatAttributeValue(value: Any?): String? {
    if (value == null) return null

    return try {
        when (value) {
            is String -> value
            is CharSequence -> value.toString()
            is Number -> value.toString()
            is Boolean -> value.toString()
            is Collection<*> -> value.filterNotNull().joinToString(", ") { formatAttributeValue(it) ?: it.toString() }
            is Array<*> -> value.filterNotNull().joinToString(", ") { formatAttributeValue(it) ?: it.toString() }
            else -> {
                val kClass = value::class
                val memberProps = try {
                    kClass.memberProperties
                } catch (t: Throwable) {
                    null
                }

                if (memberProps == null || memberProps.isEmpty()) {
                    value.toString()
                } else {
                    memberProps.mapNotNull { p ->
                        val v = try { p.getter.call(value) } catch (_: Throwable) { null }
                        val fv = formatAttributeValue(v)
                        if (fv.isNullOrBlank()) null else "${camelToLabel(p.name)}: $fv"
                    }.joinToString("; ")
                }
            }
        }
    } catch (t: Throwable) {
        value.toString()
    }
}

private fun camelToLabel(name: String): String {
    if (name.contains('_')) {
        return name.split('_').joinToString(" ") { it.replaceFirstChar { ch -> ch.uppercaseChar() } }
    }
    val result = name.replace(Regex("([a-z])([A-Z])"), "$1 $2")
    return result.split(' ').joinToString(" ") { it.replaceFirstChar { ch -> ch.uppercaseChar() } }
}
