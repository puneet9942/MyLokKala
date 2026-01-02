// MultipartUtils.kt
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.buffer
import okio.source
import java.io.IOException
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import okio.Okio

// Query display name (filename) from content resolver, fallback to lastPathSegment
private fun queryFileName(context: Context, uri: Uri): String {
    var name: String? = null
    val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (idx != -1 && it.moveToFirst()) {
            name = it.getString(idx)
        }
    }
    if (name.isNullOrBlank()) {
        name = uri.lastPathSegment ?: "file_${System.currentTimeMillis()}"
    }
    return name!!
}

/**
 * Create a streaming RequestBody backed by the content resolver InputStream.
 * This avoids loading whole file bytes into memory.
 */
private fun streamRequestBodyFromUri(context: Context, uri: Uri, mime: String?): RequestBody {
    return object : RequestBody() {
        override fun contentType() = (mime ?: "application/octet-stream").toMediaTypeOrNull()
        override fun writeTo(sink: BufferedSink) {
            // open input stream and copy to sink
            val input = context.contentResolver.openInputStream(uri)
                ?: throw IOException("Unable to open stream for URI: $uri")
            input.source().use { source ->
                sink.writeAll(source)
            }
            input.close()
        }
    }
}

/**
 * Convert a Uri to MultipartBody.Part
 *
 * @param context Android context
 * @param partName form field name expected by server, e.g. "photos" or "photos[]"
 * @param uri content Uri (content://..., file://...)
 */
fun uriToMultipartPart(context: Context, partName: String, uri: Uri): MultipartBody.Part {
    val filename = queryFileName(context, uri)
    val mime = try { context.contentResolver.getType(uri) } catch (_: Exception) { null } ?: "application/octet-stream"
    val requestBody = streamRequestBodyFromUri(context, uri, mime)
    return MultipartBody.Part.createFormData(partName, filename, requestBody)
}

/**
 * Build a list of MultipartBody.Part for many URIs, filtering nulls.
 */
fun uriListToMultipartParts(context: Context, partName: String, uris: List<Uri>?): List<MultipartBody.Part> {
    if (uris == null) return emptyList()
    return uris.map { uriToMultipartPart(context, partName, it) }
}
