package com.example.lokkala.presentation.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Home : BottomNavItem("home", Icons.Default.Search, "Home")
    object Saved : BottomNavItem("saved", Icons.Default.Favorite, "Saved")
    object Messages : BottomNavItem("messages", Icons.Default.Email, "Messages")
    object Profile : BottomNavItem("profile", Icons.Default.Person, "Profile")
}