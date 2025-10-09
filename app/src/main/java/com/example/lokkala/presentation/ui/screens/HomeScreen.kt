package com.example.lokkala.presentation.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.lokkala.presentation.feature.home.HomeViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

// simple sealed for bottom tabs
sealed class BottomTab(val route: String, val icon: ImageVector, val label: String) {
    object Home : BottomTab("home_tab", Icons.Default.Home, "Home")
    object Saved : BottomTab("saved_tab", Icons.Default.Favorite, "Saved")
    object Messages : BottomTab("messages_tab", Icons.Default.Email, "Messages")
    object Profile : BottomTab("profile_tab", Icons.Default.Person, "Profile")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    // Outer nav controller for the whole Home screen (not the app root)
    val outerNav = rememberNavController()
    val bottomTabs = listOf(BottomTab.Home, BottomTab.Saved, BottomTab.Messages, BottomTab.Profile)

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Explore") })
        },
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by outerNav.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                bottomTabs.forEach { tab ->
                    NavigationBarItem(
                        icon = { androidx.compose.material3.Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        selected = currentRoute == tab.route,
                        onClick = {
                            if (currentRoute != tab.route) {
                                outerNav.navigate(tab.route) {
                                    popUpTo(outerNav.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFFFF6D00),
                            selectedTextColor = Color(0xFFFF6D00),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController = outerNav, startDestination = BottomTab.Home.route, modifier = Modifier.padding(innerPadding)) {
            // Home tab: contains nested inner nav to handle details navigation and to share the same HomeViewModel instance
            composable(BottomTab.Home.route) {
                // create HomeViewModel once here and pass it down to both list and details screens
                val homeViewModel: HomeViewModel = hiltViewModel()
                val innerNav = rememberNavController()
                NavHost(navController = innerNav, startDestination = "home_list") {
                    composable("home_list") {
                        HomeTabContent(
                            viewModel = homeViewModel,
                            onNavigateToDetails = { adId -> innerNav.navigate("details/$adId") }
                        )
                    }
                    composable("details/{adId}") { backStackEntry ->
                        val adId = requireNotNull(backStackEntry.arguments?.getString("adId"))
                        DetailsScreen(adId = adId, homeViewModel = homeViewModel, onBack = { innerNav.popBackStack() })
                    }
                }
            }

            composable(BottomTab.Saved.route) {
                TabContentScreen("Saved Tab")
            }
            composable(BottomTab.Messages.route) {
                TabContentScreen("Messages Tab")
            }
            composable(BottomTab.Profile.route) {
                TabContentScreen("Profile Tab")
            }
        }
    }
}

@Composable
fun TabContentScreen(title: String) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Top) {
        Text(text = title, modifier = Modifier.padding(24.dp))
    }
}