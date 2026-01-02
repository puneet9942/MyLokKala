package com.example.museapp.presentation.ui.components

import androidx.compose.material.MaterialTheme
import androidx.compose.material.TextFieldColors
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.museapp.ui.theme.PrimaryColor

@Composable
fun appButtonColors() = androidx.compose.material3.ButtonDefaults.buttonColors(
    containerColor = PrimaryColor,
    contentColor = Color.White
)

@Composable
fun appTextFieldColors(): TextFieldColors =
    TextFieldDefaults.outlinedTextFieldColors(
        focusedBorderColor = PrimaryColor,
        unfocusedBorderColor = PrimaryColor,
        cursorColor = PrimaryColor,
        backgroundColor = Color.Transparent,
        focusedLabelColor = PrimaryColor,
        unfocusedLabelColor = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
        placeholderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
    )