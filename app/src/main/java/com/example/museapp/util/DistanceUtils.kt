package com.example.museapp.util

import kotlin.math.*

object DistanceUtils {
    /**
     * Haversine formula - returns distance in kilometers.
     */
    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0 // km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)
        val c = 2 * asin(sqrt(a))
        return earthRadius * c
    }

    fun formatDistance(km: Double): String {
        return if (km < 1.0) {
            val meters = (km * 1000).toInt()
            "$meters m"
        } else {
            String.format("%.1f km", km)
        }
    }
}