package com.example.museapp.presentation.feature.feedback.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.input.TextFieldValue

@Immutable
data class FeedbackUiState(
    val feedbackText: TextFieldValue = TextFieldValue(""),
    val isLoading: Boolean = false,
    val success: Boolean = false
)
