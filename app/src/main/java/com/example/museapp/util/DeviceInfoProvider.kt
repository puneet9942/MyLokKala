package com.example.museapp.util

import android.content.Context
import android.os.Build
import com.example.museapp.BuildConfig
import com.example.museapp.data.remote.dto.DevicesInfo

/**
 * Provide DeviceInfo in a util package so repositories do NOT need Context DI changes.
 */
object DeviceInfoProvider {

    fun default(): DevicesInfo {
        val model = Build.MODEL ?: "unknown"
        val osVersion = "Android ${Build.VERSION.RELEASE ?: "unknown"}"
        val appVersion = try {
            BuildConfig.VERSION_NAME ?: "1.0.0"
        } catch (t: Throwable) {
            "1.0.0"
        }
        return DevicesInfo(deviceModel = model, osVersion = osVersion, appVersion = appVersion)
    }

    fun from(context: Context): DevicesInfo {
        // Extend later if you need telephony info; keep signature so you can pass Context when needed
        return default()
    }
}
