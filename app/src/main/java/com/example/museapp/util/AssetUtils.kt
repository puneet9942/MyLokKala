package com.example.museapp.util

import android.content.Context
import java.io.IOException

object AssetUtils {

    /**
     * Reads a JSON file from the assets directory and returns its contents as a String.
     * @param filename The name of the asset file (e.g., "otp_response.json")
     * @return The contents of the file as a String, or null if reading fails.
     */
    fun readJsonAsset(context: Context, filename: String): String? {
        return try {
            context.assets.open(filename).bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    // You can add more asset/file helpers here in future, e.g., reading images, copying files, etc.
}