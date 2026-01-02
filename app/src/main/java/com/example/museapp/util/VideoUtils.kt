package com.example.museapp.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object VideoUtils {

    suspend fun loadOrCreateVideoThumbnail(context: Context, uri: Uri): Bitmap? = withContext(
        Dispatchers.IO) {
        try {
            val cacheFile = File(context.cacheDir, "thumb_${uri.toString().hashCode()}.jpg")
            if (cacheFile.exists()) {
                return@withContext BitmapFactory.decodeFile(cacheFile.absolutePath)
            }

            val mmr = MediaMetadataRetriever()
            try {
                mmr.setDataSource(context, uri)
                // try frame at 1 second, then fallback
                var bmp: Bitmap? = null
                try { bmp = mmr.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST) } catch (_: Throwable) { }
                if (bmp == null) {
                    try { bmp = mmr.frameAtTime } catch (_: Throwable) { }
                }
                if (bmp != null) {
                    // save cached jpeg for next time
                    saveBitmapToCacheFile(bmp, cacheFile)
                    return@withContext bmp
                }
            } finally {
                try { mmr.release() } catch (_: Throwable) { }
            }
        } catch (_: Throwable) { /* ignore */ }
        return@withContext null
    }

    fun saveBitmapToCacheFile(bitmap: Bitmap, target: File) {
        try {
            FileOutputStream(target).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                out.flush()
            }
        } catch (_: Throwable) { /* ignore */ }
    }
}