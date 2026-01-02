package com.example.museapp.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Save a Bitmap to the app cache directory and return a Uri to the file.
 *
 * This is intentionally simple and synchronous since we call it from
 * small UI actions (TakePicturePreview). If you want, we can run this
 * on a background thread or use a more robust FileProvider approach.
 */
fun saveBitmapToCache(context: Context, bitmap: Bitmap, prefix: String = "profile_img"): Uri {
    val cacheDir = context.cacheDir
    val fileName = "${prefix}_${System.currentTimeMillis()}.jpg"
    val outFile = File(cacheDir, fileName)

    var fos: FileOutputStream? = null
    try {
        fos = FileOutputStream(outFile)
        // compress to JPEG; quality 85 to balance size/quality
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos)
        fos.flush()
    } catch (t: Throwable) {
        // if something goes wrong, attempt to delete partial file
        try { outFile.delete() } catch (_: Throwable) {}
        throw IOException("Failed to save bitmap to cache", t)
    } finally {
        try { fos?.close() } catch (_: Throwable) {}
    }

    return Uri.fromFile(outFile)
}
