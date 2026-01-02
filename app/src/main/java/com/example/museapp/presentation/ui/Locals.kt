package com.example.museapp.ui

import android.app.Activity
import androidx.compose.runtime.staticCompositionLocalOf

// Provides the host Activity to composables without casting LocalContext
val LocalActivity = staticCompositionLocalOf<Activity?> { null }
