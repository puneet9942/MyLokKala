package com.example.lokkala.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.coroutines.resume

object AddressHelper {

    @SuppressLint("MissingPermission")
    suspend fun getCurrentAddress(context: Context): String {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        val isPermissionGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PermissionChecker.PERMISSION_GRANTED

        if (!isPermissionGranted) {
            return "Location permission not granted"
        }

        val location: Location = fusedLocationClient.awaitLastLocation()
            ?: return "Location not available"

        val geocoder = Geocoder(context, Locale.getDefault())
        val addressList = withContext(Dispatchers.IO) {
            geocoder.getFromLocation(location.latitude, location.longitude, 1)
        }

        return addressList?.firstOrNull()?.locality ?: "Unknown location"
    }

    private suspend fun FusedLocationProviderClient.awaitLastLocation(): Location? =
        suspendCancellableCoroutine { cont ->
            lastLocation
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resume(null) }
        }
}
