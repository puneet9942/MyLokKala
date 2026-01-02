package com.example.museapp.presentation.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import androidx.compose.runtime.SideEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.museapp.presentation.feature.home.HomeTabContent
import com.example.museapp.presentation.feature.home.HomeViewModel
import com.example.museapp.presentation.feature.favorites.FavoritesViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.museapp.ui.theme.PrimaryColor

sealed class BottomTab(val route: String, val icon: ImageVector, val label: String) {
    object Home : BottomTab("home_tab", Icons.Default.Home, "Home")
    object Saved : BottomTab("saved_tab", Icons.Default.Favorite, "Saved")
    object Messages : BottomTab("messages_tab", Icons.Default.Email, "Messages")
    object MyAds : BottomTab("account_tab", Icons.Default.Lock, "My Ads")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    rootNavController: NavHostController,
    initialTab: String? = null
) {
    val systemUi = rememberSystemUiController()
    SideEffect {
        systemUi.setStatusBarColor(Color.White, darkIcons = true)
        systemUi.setNavigationBarColor(Color.White, darkIcons = true)
    }

    val outerNav = rememberNavController()

    // create one shared FavoritesViewModel at the HomeScreen scope
    val favoritesViewModel: FavoritesViewModel = hiltViewModel()

    LaunchedEffect(initialTab) {
        initialTab?.let { tabRoute ->
            outerNav.navigate(tabRoute) {
                popUpTo(outerNav.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val bottomTabs = listOf(BottomTab.Home, BottomTab.Saved, BottomTab.Messages, BottomTab.MyAds)
    val navBackStackEntry by outerNav.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = Color.White,
        topBar = {},
        bottomBar = {
            Column(Modifier.fillMaxWidth()) {
                Divider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 1.dp,
                    color = Color.Gray
                )
                CustomBottomBar(
                    tabs = bottomTabs,
                    navBackStackEntry = navBackStackEntry,
                    currentRoute = currentRoute
                ) { tab ->
                    val start = outerNav.graph.findStartDestination().id
                    if (currentRoute == tab.route) {
                        outerNav.navigate(tab.route) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    } else {
                        outerNav.navigate(tab.route) {
                            popUpTo(start) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(innerPadding)
        ) {
            NavHost(
                navController = outerNav,
                startDestination = BottomTab.Home.route,
                modifier = Modifier.fillMaxSize()
            ) {
                // Home tab nested graph
                navigation(route = BottomTab.Home.route, startDestination = "homelist") {
                    composable("homelist") {
                        HomeTabContent(
                            homeViewModel = homeViewModel,
                            // pass the shared favoritesViewModel instance
                            favoritesViewModel = favoritesViewModel,
                            onCreateAdClicked = { rootNavController.navigate("create_ad") },
                            onNavigateToDetails = { adId ->
                                val encoded = Uri.encode(adId)
                                runCatching { outerNav.navigate("details/$encoded") }
                                    .onFailure {
                                        runCatching { outerNav.navigate("${BottomTab.Home.route}/details/$encoded") }
                                    }
                            },
                            onProfileClicked = {
                                rootNavController.navigate("profile_fullscreen") {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable("create_ad") {
                        CreateAdScreen(onBack = { outerNav.popBackStack() })
                    }

                    composable("details/{adId}") { backStackEntry ->
                        val adId = backStackEntry.arguments?.getString("adId")?.let(Uri::decode) ?: return@composable
                        // DetailsScreen(adId = adId, homeViewModel = homeViewModel) { outerNav.popBackStack() }
                    }
                }

                // Saved tab nested graph
                navigation(route = BottomTab.Saved.route, startDestination = "savedlist") {
                    composable("savedlist") {
                        SavedTabContent(
                            homeViewModel = homeViewModel,
                            // pass same shared favoritesViewModel to Saved
                            favoritesViewModel = favoritesViewModel,
                            onNavigateToDetails = { adId ->
                                val encoded = Uri.encode(adId)
                                runCatching { outerNav.navigate("details/$encoded") }
                                    .onFailure {
                                        runCatching { outerNav.navigate("${BottomTab.Saved.route}/details/$encoded") }
                                    }
                            }
                        )
                    }
                    composable("details/{adId}") { backStackEntry ->
                        val adId = backStackEntry.arguments?.getString("adId")?.let(Uri::decode) ?: return@composable
                        // DetailsScreen(adId = adId, homeViewModel = homeViewModel) { outerNav.popBackStack() }
                    }
                }

                // placeholders for other tabs
                composable(BottomTab.Messages.route) { TabContentScreen("Messages") }
                composable(BottomTab.MyAds.route) { TabContentScreen("My Ads") }
            }
        }
    }
}

@Composable
fun TabContentScreen(title: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, modifier = Modifier.padding(24.dp))
    }
}

@Composable
fun CustomBottomBar(
    tabs: List<BottomTab>,
    navBackStackEntry: NavBackStackEntry?,
    currentRoute: String?,
    onTabSelected: (BottomTab) -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 0.dp,
        windowInsets = NavigationBarDefaults.windowInsets
    ) {
        tabs.forEach { tab ->
            val selected = navBackStackEntry?.destination?.hierarchy?.any { it.route == tab.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(tab) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label, fontSize = 12.sp) },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryColor,
                    selectedTextColor = PrimaryColor,
                    indicatorColor = Color.Transparent,
                    unselectedIconColor = Color(0xFF414141),
                    unselectedTextColor = Color(0xFF414141)
                )
            )
        }
    }
}
