package com.example.museapp.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

object LocationHelper {
    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestLocationPermission(launcher: ActivityResultLauncher<String>) {
        launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}
