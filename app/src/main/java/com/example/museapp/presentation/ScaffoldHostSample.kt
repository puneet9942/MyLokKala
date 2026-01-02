package com.example.museapp.presentation.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.Scaffold
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.museapp.ui.theme.AppTypography
import com.example.museapp.ui.theme.PrimaryColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaffoldHostSample(startOnSplash: Boolean = true) {
    val navController = rememberNavController()
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route

    val showTopBar = currentRoute != "splash"

    Scaffold(
        topBar = {
            if (showTopBar) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "LokKala",
                            style = AppTypography.titleLarge,
                            color = Color.White
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = PrimaryColor
                    )
                )
            }
        }
    ) { innerPadding ->
        Surface(modifier = Modifier.padding(innerPadding)) {
            NavHost(navController = navController, startDestination = if (startOnSplash) "splash" else "home") {
                composable("splash") { /* your SplashScreen composable here */ }
                composable("home") { /* your home screen */ }
            }
        }
    }
}
