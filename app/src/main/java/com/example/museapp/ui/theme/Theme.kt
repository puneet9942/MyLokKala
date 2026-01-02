package com.example.museapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.material.MaterialTheme as LegacyMaterialTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.museapp.R
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.example.museapp.ui.LocalActivity
import androidx.compose.ui.graphics.toArgb

// --------------------------
// Colors (top-level, pure values)
// ---------------
val PrimaryColor = Color(0xFF6030A8)  // Warm Orange (medium)
val SecondaryColor = Color(0xFF6030A8)  // Deep Burnt Orange
val TertiaryColor = Color(0xFFFF3D7F)   // Deep Violet

val PrimaryDark = Color(0xFFFF8C42)
val SecondaryDark = Color(0xFFCC5803)
val TertiaryDark = Color(0xFFFF3D7F)

// --------------------------
// Material3 color schemes (pure)
// Explicitly set background & surface to white for the light scheme
private val LightColorScheme = lightColorScheme(
    primary = PrimaryColor,
    secondary = SecondaryColor,
    tertiary = TertiaryColor,
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    secondary = SecondaryDark,
    tertiary = TertiaryDark
)

// --------------------------
// App font and typography (pure vals only; no composable calls)
// Ensure res/font/app_font_regular.ttf exists
val AppFontFamily = FontFamily(
    Font(R.font.poppins, FontWeight.Normal)
)


val AppTypography: Typography = run {
    val d = Typography() // Material3 defaults (all exact token values)
    Typography(
        displayLarge  = d.displayLarge.copy(fontFamily = AppFontFamily),
        displayMedium = d.displayMedium.copy(fontFamily = AppFontFamily),
        displaySmall  = d.displaySmall.copy(fontFamily = AppFontFamily),
        headlineLarge = d.headlineLarge.copy(fontFamily = AppFontFamily),
        headlineMedium = d.headlineMedium.copy(fontFamily = AppFontFamily),
        headlineSmall = d.headlineSmall.copy(fontFamily = AppFontFamily),
        titleLarge = d.titleLarge.copy(fontFamily = AppFontFamily),
        titleMedium = d.titleMedium.copy(fontFamily = AppFontFamily),
        titleSmall = d.titleSmall.copy(fontFamily = AppFontFamily),
        bodyLarge = d.bodyLarge.copy(fontFamily = AppFontFamily),
        bodyMedium = d.bodyMedium.copy(fontFamily = AppFontFamily),
        bodySmall = d.bodySmall.copy(fontFamily = AppFontFamily),
        labelLarge = d.labelLarge.copy(fontFamily = AppFontFamily),
        labelMedium = d.labelMedium.copy(fontFamily = AppFontFamily),
        labelSmall = d.labelSmall.copy(fontFamily = AppFontFamily)
    )
}

// --------------------------
// Legacy Material (v2) colors (pure)
private val LegacyLightColors = lightColors(
    primary = PrimaryColor,
    primaryVariant = SecondaryColor,
    secondary = TertiaryColor
)

private val LegacyDarkColors = darkColors(
    primary = PrimaryDark,
    primaryVariant = SecondaryDark,
    secondary = TertiaryDark
)

// --------------------------
// Theme composable — only here we call composable APIs
@Composable
fun LokKalaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // composable-only calls are inside this function
    val ctx = LocalContext.current
    val activity: Activity? = LocalActivity.current
    val systemUiController = rememberSystemUiController()

    // For light theme we avoid system dynamic color to prevent wallpaper-driven tint.
    // For dark theme we can still use dynamicDarkColorScheme if available.
    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(ctx) else LightColorScheme
    } else {
        if (darkTheme) DarkColorScheme else LightColorScheme
    }

    val statusBarColor = if (darkTheme) SecondaryDark else SecondaryColor
    // Do NOT override navigation bar color here (restore system default)
    val useDarkIcons = !darkTheme

    SideEffect {
        // System bar colors via Accompanist
        systemUiController.setStatusBarColor(
            color = statusBarColor,
            darkIcons = useDarkIcons
        )

        // Also set Activity window status bar color if Activity is available (no LocalContext cast)
        activity?.window?.statusBarColor = statusBarColor.toArgb()
    }

    // Wrap Material3 with Legacy Material so older components pick up PrimaryColor
    LegacyMaterialTheme(colors = if (darkTheme) LegacyDarkColors else LegacyLightColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}
