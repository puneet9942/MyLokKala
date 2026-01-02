package com.example.museapp.util

import android.location.Geocoder
import android.location.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * ReverseGeocodeHelper
 *
 * Use this from a ViewModel or other coroutine scope (not from UI composables directly).
 * It does the blocking Geocoder work on Dispatchers.IO and caches the result.
 *
 * Usage example (from a ViewModel):
 *   viewModelScope.launch {
 *       ReverseGeocodeHelper.reverseGeocodeAndCacheIfPossible(context, lat, lng)
 *       // then update UI/state to read the newly cached label
 *   }
 */
object ReverseGeocodeHelper {

    private const val PREF_KEY_LOCATION_LABEL = "last_location_label"

    /**
     * Attempts to reverse-geocode the provided coordinates, returns the best locality string if found.
     * If successful, it will persist the label in SharedPrefUtils under PREF_KEY_LOCATION_LABEL.
     *
     * NOTE: Caller must ensure location permission has already been granted before calling this.
     */
    suspend fun reverseGeocodeAndCacheIfPossible(context: android.content.Context?, lat: Double, lng: Double): String? {
        if (context == null) return null

        // Do the geocoding off the main thread
        return try {
            withContext(Dispatchers.IO) {
                try {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    val results = geocoder.getFromLocation(lat, lng, 1) // may block / network
                    if (!results.isNullOrEmpty()) {
                        val addr = results[0]
                        // pick the most meaningful short label available
                        val locality = listOf(
                            addr.locality,
                            addr.subAdminArea,
                            addr.subLocality,
                            addr.featureName,
                            addr.adminArea
                        ).firstOrNull { !it.isNullOrBlank() }

                        val label = locality ?: addr.getAddressLine(0)?.takeIf { it.isNotBlank() }
                        if (!label.isNullOrBlank()) {
                            try {
                                SharedPrefUtils.putString(context, PREF_KEY_LOCATION_LABEL, label)
                            } catch (_: Throwable) {
                                // ignore caching failure
                            }
                            return@withContext label
                        }
                    }
                    null
                } catch (t: Throwable) {
                    // geocoder unavailable, network fail, or other.
                    null
                }
            }
        } catch (t: Throwable) {
            null
        }
    }
}
