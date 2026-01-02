package com.example.museapp.util

import android.os.Build
import androidx.annotation.RequiresApi
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object TimeUtils {
    /**
     * Returns ISO-8601 UTC timestamp string. Accepts optional provider for deterministic tests.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun isoUtcNow(nowProvider: (() -> String)? = null): String {
        nowProvider?.let { return it() }
        return try {
            Instant.now().toString()
        } catch (t: Throwable) {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            sdf.format(Date())
        }
    }
}
