package com.example.museapp.util

import android.annotation.SuppressLint
import com.example.museapp.BuildConfig
import com.example.museapp.data.remote.dto.CommonRequest
import com.example.museapp.data.remote.dto.DevicesInfo
import com.example.museapp.data.remote.dto.LocationDto
import com.google.android.gms.location.LocationServices
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlinx.coroutines.suspendCancellableCoroutine
import android.location.Location
import kotlin.coroutines.resume

object CommonRequestBuilder {

    private const val KEY_LAST_LAT = "last_lat"
    private const val KEY_LAST_LNG = "last_lng"

    /**
     * Non-suspending build that uses cached coords if available, otherwise AppConstants defaults.
     */
    fun <TPayload> build(
        payload: TPayload,
        locale: String = "en-IN",
        platform: String = "android",
        authToken: String? = null,
        deviceInfo: DevicesInfo = DeviceInfoProvider.default()
    ): CommonRequest<TPayload> {
        val ctx = AppContextProvider.getContext()
        val latStr: String = ctx?.let { SharedPrefUtils.getString(it, KEY_LAST_LAT) } ?: AppConstants.DEFAULT_LAT.toString()
        val lngStr: String = ctx?.let { SharedPrefUtils.getString(it, KEY_LAST_LNG) } ?: AppConstants.DEFAULT_LNG.toString()

        val locationDto = LocationDto(lat = latStr, long = lngStr)

        return CommonRequest(
            request_id = UUID.randomUUID().toString(),
            timestamp = utcIsoNow(),
            app_version = BuildConfig.VERSION_NAME,
            platform = platform,
            auth_token = authToken,
            locale = locale,
            device_info = deviceInfo,
            payload = payload,
            location = locationDto
        )
    }

    /**
     * Suspends while attempting to fetch the last known device location using FusedLocationProvider.
     * If permission denied, unavailable, or any error occurs, falls back to cached values or AppConstants defaults.
     *
     * Note: We explicitly check permission via LocationHelper.hasLocationPermission(ctx) and catch SecurityException
     * in case the permission gets revoked between the check and the actual call.
     */
    @SuppressLint("MissingPermission")
    suspend fun <TPayload> buildWithLiveLocation(
        payload: TPayload,
        locale: String = "en-IN",
        platform: String = "android",
        authToken: String? = null,
        deviceInfo: DevicesInfo = DeviceInfoProvider.default()
    ): CommonRequest<TPayload> {
        var lat = AppConstants.DEFAULT_LAT
        var lng = AppConstants.DEFAULT_LNG
        val ctx = AppContextProvider.getContext()

        if (ctx != null) {
            try {
                // Only attempt fused location if we have permission
                if (LocationHelper.hasLocationPermission(ctx)) {
                    try {
                        val fused = LocationServices.getFusedLocationProviderClient(ctx)
                        val last: Location? = suspendCancellableCoroutine { cont ->
                            fused.lastLocation
                                .addOnSuccessListener { loc -> cont.resume(loc) }
                                .addOnFailureListener { _ -> cont.resume(null) }
                        }

                        if (last != null) {
                            lat = last.latitude
                            lng = last.longitude
                            // cache
                            try {
                                SharedPrefUtils.putString(ctx, KEY_LAST_LAT, lat.toString())
                                SharedPrefUtils.putString(ctx, KEY_LAST_LNG, lng.toString())
                            } catch (_: Throwable) { /* ignore caching errors */ }
                        } else {
                            // fallback to cached if available
                            try {
                                val cachedLat = SharedPrefUtils.getString(ctx, KEY_LAST_LAT)
                                val cachedLng = SharedPrefUtils.getString(ctx, KEY_LAST_LNG)
                                if (!cachedLat.isNullOrBlank() && !cachedLng.isNullOrBlank()) {
                                    lat = cachedLat.toDoubleOrNull() ?: lat
                                    lng = cachedLng.toDoubleOrNull() ?: lng
                                }
                            } catch (_: Throwable) {
                                // ignore
                            }
                        }
                    } catch (se: SecurityException) {
                        // permission revoked between check and call - fallback to cache/default
                        try {
                            val cachedLat = SharedPrefUtils.getString(ctx, KEY_LAST_LAT)
                            val cachedLng = SharedPrefUtils.getString(ctx, KEY_LAST_LNG)
                            if (!cachedLat.isNullOrBlank() && !cachedLng.isNullOrBlank()) {
                                lat = cachedLat.toDoubleOrNull() ?: lat
                                lng = cachedLng.toDoubleOrNull() ?: lng
                            }
                        } catch (_: Throwable) { }
                    } catch (_: Throwable) {
                        // fused client error - fall back to cached/default
                        try {
                            val cachedLat = SharedPrefUtils.getString(ctx, KEY_LAST_LAT)
                            val cachedLng = SharedPrefUtils.getString(ctx, KEY_LAST_LNG)
                            if (!cachedLat.isNullOrBlank() && !cachedLng.isNullOrBlank()) {
                                lat = cachedLat.toDoubleOrNull() ?: lat
                                lng = cachedLng.toDoubleOrNull() ?: lng
                            }
                        } catch (_: Throwable) { }
                    }
                } else {
                    // no permission: use cached values if present
                    try {
                        val cachedLat = SharedPrefUtils.getString(ctx, KEY_LAST_LAT)
                        val cachedLng = SharedPrefUtils.getString(ctx, KEY_LAST_LNG)
                        if (!cachedLat.isNullOrBlank() && !cachedLng.isNullOrBlank()) {
                            lat = cachedLat.toDoubleOrNull() ?: lat
                            lng = cachedLng.toDoubleOrNull() ?: lng
                        }
                    } catch (_: Throwable) { /* ignore */ }
                }
            } catch (_: Throwable) {
                // top-level fallback to cache/default if anything unexpected happens
                try {
                    val cachedLat = SharedPrefUtils.getString(ctx, KEY_LAST_LAT)
                    val cachedLng = SharedPrefUtils.getString(ctx, KEY_LAST_LNG)
                    if (!cachedLat.isNullOrBlank() && !cachedLng.isNullOrBlank()) {
                        lat = cachedLat.toDoubleOrNull() ?: lat
                        lng = cachedLng.toDoubleOrNull() ?: lng
                    }
                } catch (_: Throwable) { /* ignore */ }
            }
        }

        val locationDto = LocationDto(lat = lat.toString(), long = lng.toString())

        return CommonRequest(
            request_id = UUID.randomUUID().toString(),
            timestamp = utcIsoNow(),
            app_version = BuildConfig.VERSION_NAME,
            platform = platform,
            auth_token = authToken,
            locale = locale,
            device_info = deviceInfo,
            payload = payload,
            location = locationDto
        )
    }

    private fun utcIsoNow(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        return fmt.format(Date())
    }
}
