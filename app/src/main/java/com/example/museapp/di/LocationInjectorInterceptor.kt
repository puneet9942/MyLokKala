// File: main/java/com/example/museapp/di/LocationInjectorInterceptor.kt
package com.example.museapp.network

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.museapp.util.AppConstants
import com.example.museapp.util.SharedPrefUtils
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.Buffer
import org.json.JSONObject
import java.nio.charset.StandardCharsets

/**
 * Interceptor that patches outgoing JSON request bodies with a "location" object:
 *   "location": { "lat": "<string>", "long": "<string>" }
 *
 * Strategy:
 * 1. Try device last known location via LocationManager (if app has ACCESS_FINE_LOCATION).
 * 2. If unavailable or no permission, try cached values in SharedPrefUtils (keys: last_lat, last_lng).
 * 3. Fallback to AppConstants.DEFAULT_LAT / DEFAULT_LNG (Faridabad).
 *
 * Notes:
 * - Does nothing for multipart or non-JSON requests.
 * - Caches any successfully obtained device coordinates.
 */
class LocationInjectorInterceptor(private val appContext: Context) : Interceptor {

    companion object {
        private const val KEY_LAST_LAT = "last_lat"
        private const val KEY_LAST_LNG = "last_lng"
        private const val JSON_MEDIA_TYPE = "application/json"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()

        // If no body or not JSON, just continue
        val contentType = req.body?.contentType()?.toString() ?: ""
        if (contentType.isBlank() || !contentType.contains(JSON_MEDIA_TYPE, ignoreCase = true)) {
            return chain.proceed(req)
        }

        // Read body as string
        val buffer = Buffer()
        try {
            req.body?.writeTo(buffer)
        } catch (t: Throwable) {
            // If we cannot read body for some reason, proceed as-is
            return chain.proceed(req)
        }
        val charset = req.body?.contentType()?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8
        val bodyStr = buffer.readString(charset)

        // Try to parse JSON
        val jsonObj = try {
            JSONObject(bodyStr)
        } catch (t: Throwable) {
            // Not valid JSON -> proceed
            return chain.proceed(req)
        }

        // Compute lat,long (strings)
        val (latStr, lngStr, usedDevice) = obtainCoordinates()

        // Insert or replace location field
        try {
            val loc = JSONObject()
            loc.put("lat", latStr)
            loc.put("long", lngStr)
            jsonObj.put("location", loc)
        } catch (t: Throwable) {
            // In case of any JSON errors, proceed with original request
            return chain.proceed(req)
        }

        // Build new body and request
        val newBody = jsonObj.toString().toRequestBody(JSON_MEDIA_TYPE.toMediaTypeOrNull())
        val newReq = req.newBuilder()
            .method(req.method, newBody)
            .header("Content-Type", JSON_MEDIA_TYPE)
            .build()

        // If we used device values, cache them
        if (usedDevice) {
            try {
                SharedPrefUtils.putString(appContext, KEY_LAST_LAT, latStr)
                SharedPrefUtils.putString(appContext, KEY_LAST_LNG, lngStr)
            } catch (_: Throwable) {
                // ignore caching failures
            }
        }

        return chain.proceed(newReq)
    }

    /**
     * Returns Triple(latString, lngString, usedDeviceBoolean)
     */
    private fun obtainCoordinates(): Triple<String, String, Boolean> {
        // 1) Try device last known location (if permission)
        try {
            val isLocationGranted = ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            Log.d("location enabled", isLocationGranted.toString())
            if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                val lm = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                lm?.let { locationManager ->
                    val providers = locationManager.getProviders(true)
                    var best: Location? = null
                    for (p in providers) {
                        try {
                            val l = locationManager.getLastKnownLocation(p)
                            if (l != null) {
                                if (best == null || l.accuracy < best.accuracy) best = l
                            }
                        } catch (_: SecurityException) {
                            // permission issue -> abort device attempt
                            break
                        } catch (_: Throwable) {
                            // ignore provider-specific errors
                        }
                    }
                    best?.let {
                        return Triple(it.latitude.toString(), it.longitude.toString(), true)
                    }
                }
            }
        } catch (_: Throwable) {
            // ignore device lookup errors
        }

        // 2) Try cached
        try {
            val cachedLat = SharedPrefUtils.getString(appContext, KEY_LAST_LAT)
            val cachedLng = SharedPrefUtils.getString(appContext, KEY_LAST_LNG)
            if (!cachedLat.isNullOrBlank() && !cachedLng.isNullOrBlank()) {
                return Triple(cachedLat, cachedLng, false)
            }
        } catch (_: Throwable) {
            // ignore
        }

        // 3) fallback to default Faridabad
        return Triple(AppConstants.DEFAULT_LAT.toString(), AppConstants.DEFAULT_LNG.toString(), false)
    }
}
