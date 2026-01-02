package com.example.museapp.presentation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.example.museapp.ui.theme.AppTypography

private val Light = lightColorScheme()

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Light, typography = AppTypography, content = content)
}