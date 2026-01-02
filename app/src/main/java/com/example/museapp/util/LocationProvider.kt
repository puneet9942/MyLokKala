package com.example.museapp.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import kotlin.coroutines.resume
import java.util.Locale

/**
 * Thin, testable wrapper around FusedLocationProviderClient.
 *
 * - Returns null if location permission is not granted or location is unavailable.
 * - Tries multiple strategies (getCurrentLocation -> lastLocation -> LocationManager providers).
 * - Provides helpers to reverse-geocode a Location into a human locality/area string.
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
     * Strategy:
     * 1) If permission missing -> return null immediately.
     * 2) Try fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY) with a short timeout (default 5s).
     * 3) If that returns null, try fused.lastLocation.
     * 4) If still null (or timeout), scan LocationManager providers and pick the best Location by accuracy.
     *
     * Notes:
     * - This is safe to call from a coroutine; it will not block the main thread.
     * - We intentionally keep the timeout short so the UI/VM doesn't hang waiting for a fresh fix.
     */
    @SuppressLint("MissingPermission")
    suspend fun getLastKnownLocation(timeoutMs: Long = 5000L): Location? {
        if (!hasLocationPermission()) return null

        // 1) Try getCurrentLocation (best attempt for a fresh, high-accuracy fix)
        try {
            val fromFused = withTimeoutOrNull(timeoutMs) {
                suspendCancellableCoroutine<Location?> { cont ->
                    val cts = CancellationTokenSource()
                    try {
                        fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                            .addOnSuccessListener { loc ->
                                if (!cont.isCompleted) cont.resume(loc)
                            }
                            .addOnFailureListener { _ ->
                                if (!cont.isCompleted) cont.resume(null)
                            }
                    } catch (se: SecurityException) {
                        // permission revoked between check and call
                        if (!cont.isCompleted) cont.resume(null)
                    } catch (t: Throwable) {
                        if (!cont.isCompleted) cont.resume(null)
                    }

                    cont.invokeOnCancellation {
                        try { cts.cancel() } catch (_: Throwable) { }
                    }
                }
            }

            if (fromFused != null) {
                Log.d("LocationProvider", "getCurrentLocation succeeded (accuracy=${fromFused.accuracy})")
                return normalizeLocation(fromFused)
            }

            // 2) Fallback: fused.lastLocation (fast, may be stale)
            val last = suspendCancellableCoroutine<Location?> { cont ->
                try {
                    fused.lastLocation
                        .addOnSuccessListener { loc ->
                            if (!cont.isCompleted) cont.resume(loc)
                        }
                        .addOnFailureListener { _ ->
                            if (!cont.isCompleted) cont.resume(null)
                        }
                } catch (se: SecurityException) {
                    if (!cont.isCompleted) cont.resume(null)
                } catch (t: Throwable) {
                    if (!cont.isCompleted) cont.resume(null)
                }
            }

            if (last != null) {
                Log.d("LocationProvider", "lastLocation succeeded (accuracy=${last.accuracy})")
                return normalizeLocation(last)
            }
        } catch (se: SecurityException) {
            // permission issue, bail out gracefully
            Log.w("LocationProvider", "SecurityException while using fused provider: ${se.message}")
            return null
        } catch (t: Throwable) {
            // continue to provider scan fallback
            Log.w("LocationProvider", "Fused provider error: ${t.message}")
        }

        // 3) Final fallback: LocationManager provider scan (GPS, network, passive)
        return try {
            getBestFromLocationManager()
        } catch (t: Throwable) {
            Log.w("LocationProvider", "LocationManager fallback failed: ${t.message}")
            null
        }
    }

    /**
     * Chooses the best (most accurate, non-null) location from enabled providers.
     * Returns null if permission missing or no provider has last known location.
     */
    private fun getBestFromLocationManager(): Location? {
        if (!LocationHelper.hasLocationPermission(context)) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null

        var best: Location? = null
        try {
            val providers = lm.getProviders(true)
            for (p in providers) {
                try {
                    val l = lm.getLastKnownLocation(p)
                    if (l != null) {
                        // ignore zeroed coords (sometimes returned on some devices/emulators)
                        if (l.latitude == 0.0 && l.longitude == 0.0) continue
                        if (best == null) best = l
                        else {
                            // choose the location with smaller accuracy value (meters) if available
                            val a1 = if (l.hasAccuracy()) l.accuracy else Float.MAX_VALUE
                            val a2 = if (best.hasAccuracy()) best.accuracy else Float.MAX_VALUE
                            if (a1 < a2) best = l
                        }
                    }
                } catch (se: SecurityException) {
                    // permission unexpectedly revoked; abort
                    Log.w("LocationProvider", "Permission lost while querying provider $p")
                    return null
                } catch (t: Throwable) {
                    // ignore provider-specific failures
                    Log.d("LocationProvider", "Provider $p threw: ${t.message}")
                }
            }
        } catch (t: Throwable) {
            Log.w("LocationProvider", "Error enumerating providers: ${t.message}")
        }

        best?.let { Log.d("LocationProvider", "LocationManager best: provider=${it.provider}, acc=${it.accuracy}") }
        return best?.let { normalizeLocation(it) }
    }

    /**
     * Defensive normalization: treat zeroed coordinates or NaNs as null.
     * Also returns a copy to avoid accidental mutation of original object.
     */
    private fun normalizeLocation(loc: Location?): Location? {
        if (loc == null) return null
        if (loc.latitude.isNaN() || loc.longitude.isNaN()) return null
        if (loc.latitude == 0.0 && loc.longitude == 0.0) return null
        // return clone to be safe (Location is mutable)
        val copy = Location(loc)
        return copy
    }

    /**
     * Reverse-geocodes the provided location into a short locality/area string.
     *
     * This method is safe to call from a coroutine and will run the Geocoder on Dispatchers.IO.
     * It returns the most meaningful short label it can find, choosing in this order:
     *  - locality (city)
     *  - subLocality
     *  - subAdminArea
     *  - featureName
     *  - adminArea (state)
     *  - first address line (fallback)
     *
     * Returns null if geocoding fails or no meaningful label found.
     */
    suspend fun getAreaFromLocation(location: Location?): String? {
        if (location == null) return null
        // Geocoder itself does not require location permission, but we already have coordinates here.
        return try {
            withContext(Dispatchers.IO) {
                try {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    val results = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (results.isNullOrEmpty()) return@withContext null
                    val addr = results[0]
                    val candidates = listOf(
                        addr.locality,
                        addr.subLocality,
                        addr.subAdminArea,
                        addr.featureName,
                        addr.adminArea
                    )
                    val chosen = candidates.firstOrNull { !it.isNullOrBlank() }?.trim()
                    if (!chosen.isNullOrBlank()) return@withContext chosen
                    val line = addr.getAddressLine(0)
                    return@withContext line?.trim()?.takeIf { it.isNotBlank() }
                } catch (t: Throwable) {
                    Log.w("LocationProvider", "Geocoder reverse-geocode failed: ${t.message}")
                    null
                }
            }
        } catch (t: Throwable) {
            Log.w("LocationProvider", "getAreaFromLocation failed: ${t.message}")
            null
        }
    }

    /**
     * Convenience: get last-known location (using the robust strategy) and attempt to reverse-geocode to an area.
     * Returns the area string or null.
     */
    suspend fun getAreaFromLastKnownLocation(timeoutMs: Long = 5000L): String? {
        val loc = try {
            getLastKnownLocation(timeoutMs)
        } catch (t: Throwable) {
            null
        }
        return getAreaFromLocation(loc)
    }
}
