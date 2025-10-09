package com.example.lokkala.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * Thin, testable wrapper around FusedLocationProviderClient.
 *
 * - Returns null if location permission is not granted or location is unavailable.
 * - Designed to be injected via Hilt.
 */
class LocationProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fused: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    /**
     * Convenience wrapper around your existing LocationHelper permission check.
     */
    fun hasLocationPermission(): Boolean = LocationHelper.hasLocationPermission(context)

    /**
     * Returns the last known device location or null.
     *
     * Note: this method is safe to call from a coroutine. It will return null when:
     *  - the app doesn't have ACCESS_FINE_LOCATION permission, or
     *  - the fused provider has no last known location (device/emulator state).
     */
    @SuppressLint("MissingPermission")
    suspend fun getLastKnownLocation(): Location? {
        if (!hasLocationPermission()) return null

        return suspendCancellableCoroutine { cont ->
            fused.lastLocation
                .addOnSuccessListener { loc -> cont.resume(loc) }
                .addOnFailureListener { _ -> cont.resume(null) }
        }
    }
}