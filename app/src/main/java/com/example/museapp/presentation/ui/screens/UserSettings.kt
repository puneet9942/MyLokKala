package com.example.museapp.presentation.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.museapp.ui.theme.AppTypography
import com.example.museapp.ui.theme.PrimaryColor
import androidx.compose.ui.graphics.Color

/** Routes used by this Profile module (add these to your NavHost). */
object ProfileDestinations {
    const val PROFILE_FULL = "profile_fullscreen"
    const val PROFILE_DETAIL = "profile_detail"
    const val PROFILE_WISHLIST = "profile_wishlist"
    const val PROFILE_SUBSCRIPTIONS = "profile_subscriptions"
    const val SUBSCRIBE_PRO = "subscribe_pro"
    const val REFER_APP = "refer_app"
    const val FEEDBACK = "feedback"
    const val RATE_APP = "rate_app"
    const val ABOUT_US = "about_us"
    const val TERMS = "terms"
    const val PRIVACY = "privacy"
    const val NOTIFICATION_PREFS = "notification_prefs"
    const val MORE_APPS = "more_apps"
    const val FAQ = "faq_apps"
    const val LOGOUT_CONFIRM = "logout_confirm"
    const val MOBILE_VERIFY = "mobile_verify"
}

/** Small reusable row item used in the Profile list */
@Composable
private fun ProfileMenuItem(
    label: String,
    subtitle: String? = null,
    leading: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leading != null) {
                Box(modifier = Modifier.padding(end = 12.dp)) { leading() }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, style = AppTypography.titleMedium)
                subtitle?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = it, style = AppTypography.bodySmall, color = Color.Gray)
                }
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Open",
                tint = Color.Gray
            )
        }
        Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)
    }
}

/** The full-screen Profile page — lists sections per your PDF (blank/dummy screens for navigation) */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavHostController,
    onLogout: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var showLogoutConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("User Settings", style = AppTypography.headlineSmall) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        // Use LazyColumn so the screen scrolls vertically when content is taller than viewport
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.Top
        ) {
            item {
                // Your Information header + items
                Text("Your Information", style = AppTypography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(Modifier.height(8.dp))
            }

            item {
                ProfileMenuItem(
                    label = "Profile",
                    subtitle = "View / edit profile",
                    leading = { Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryColor) }
                ) {
                    navController.navigate(ProfileDestinations.PROFILE_DETAIL)
                }
            }

            item {
                ProfileMenuItem(
                    label = "My Wishlist",
                    subtitle = "Saved items",
                    leading = { Icon(Icons.Default.Favorite, contentDescription = null, tint = PrimaryColor) }
                ) {
                    // Navigate to a top-level route that renders HomeScreen — avoids navigating into Home's inner NavGraph directly.
                    // AppNavHost includes a route handler for "home/saved" (see AppNavHost below).
                    navController.navigate("home/saved") {
                        launchSingleTop = true
                        popUpTo("home") { inclusive = false }
                    }
                }
            }

            item {
                ProfileMenuItem(
                    label = "My subscriptions",
                    subtitle = "Manage your subscriptions",
                    leading = { Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryColor) }
                ) {
                    navController.navigate(ProfileDestinations.PROFILE_SUBSCRIPTIONS)
                }
            }

            item { Spacer(Modifier.height(16.dp)) }

            item {
                Text("Subscriptions", style = AppTypography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(Modifier.height(8.dp))
            }

            item {
                ProfileMenuItem(
                    label = "Subscribe to Pro User",
                    subtitle = "Get Pro benefits",
                    leading = { Icon(Icons.Default.Star, contentDescription = null, tint = PrimaryColor) }
                ) {
                    navController.navigate(ProfileDestinations.SUBSCRIBE_PRO)
                }
            }

            item { Spacer(Modifier.height(16.dp)) }

            item {
                Text("Other Information", style = AppTypography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(Modifier.height(8.dp))
            }

            item {
                ProfileMenuItem(
                    label = "Share the App",
                    subtitle = "Tell your friends",
                    leading = { Icon(Icons.Default.Share, contentDescription = null, tint = PrimaryColor) }
                ) {
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, "Check out this app: <your-playstore-link>")
                        type = "text/plain"
                    }
                    val chooser = Intent.createChooser(shareIntent, "Share app")
                    context.startActivity(chooser)
                }
            }

            item {
                ProfileMenuItem(
                    label = "Refer the App",
                    subtitle = "Refer and earn",
                    leading = { Icon(Icons.Default.Refresh, contentDescription = null, tint = PrimaryColor) }
                ) {
                    navController.navigate(ProfileDestinations.REFER_APP)
                }
            }

            item {
                ProfileMenuItem(
                    label = "Send Feedback",
                    subtitle = "Help us improve",
                    leading = { Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryColor) }
                ) {
                    navController.navigate(ProfileDestinations.FEEDBACK)
                }
            }

            item {
                ProfileMenuItem(
                    label = "FAQ",
                    subtitle = "Allow us to assist you",
                    leading = { Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryColor) }
                ) {
                    navController.navigate(ProfileDestinations.FAQ)
                }
            }

            item {
                ProfileMenuItem(
                    label = "Rate App",
                    subtitle = "Rate us on Play Store",
                    leading = { Icon(Icons.Default.ThumbUp, contentDescription = null, tint = PrimaryColor) }
                ) {
                    navController.navigate(ProfileDestinations.RATE_APP)
                }
            }

            item {
                ProfileMenuItem(
                    label = "About Us",
                    subtitle = "Who we are",
                    leading = { Icon(Icons.Default.Info, contentDescription = null, tint = PrimaryColor) }
                ) {
                    navController.navigate(ProfileDestinations.ABOUT_US)
                }
            }

            item {
                ProfileMenuItem(
                    label = "Terms and Conditions",
                    subtitle = null,
                    leading = { Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryColor) }
                ) {
                    navController.navigate(ProfileDestinations.TERMS)
                }
            }

            item {
                ProfileMenuItem(
                    label = "Privacy Policy",
                    subtitle = null,
                    leading = { Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryColor) }
                ) {
                    navController.navigate(ProfileDestinations.PRIVACY)
                }
            }

            item {
                ProfileMenuItem(
                    label = "Notification Preferences",
                    subtitle = "Control what notifications you receive",
                    leading = { Icon(Icons.Default.Notifications, contentDescription = null, tint = PrimaryColor) }
                ) {
                    navController.navigate(ProfileDestinations.NOTIFICATION_PREFS)
                }
            }

            item {
                ProfileMenuItem(
                    label = "More Apps",
                    subtitle = "Other apps by us",
                    leading = { Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryColor) }
                ) {
                    navController.navigate(ProfileDestinations.MORE_APPS)
                }
            }

            item { Spacer(Modifier.height(16.dp)) }

            item {
                ProfileMenuItem(
                    label = "Log out",
                    subtitle = null,
                    leading = { Icon(Icons.Default.ExitToApp, contentDescription = null, tint = PrimaryColor) }
                ) {
                    showLogoutConfirm = true
                }
            }

            // bottom gap + version text
            item {
                Spacer(Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(text = "Version 1.0.0", style = AppTypography.bodySmall, color = Color.Gray)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // Logout confirmation dialog
    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Log out") },
            text = { Text("Are you sure you want to log out?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutConfirm = false
                    if (onLogout != null) onLogout() else navController.popBackStack()
                }) { Text("Yes") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) { Text("No") }
            }
        )
    }
}

/** Simple generic "dummy" page you can reuse for placeholders. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DummyFullScreen(title: String, subtitle: String = "Coming soon") {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text(title, style = AppTypography.headlineSmall) })
        },
        containerColor = Color.White
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(Modifier.height(16.dp))
                Text(title, style = AppTypography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text(subtitle, style = AppTypography.bodyLarge, color = Color.Gray)
            }
        }
    }
}
