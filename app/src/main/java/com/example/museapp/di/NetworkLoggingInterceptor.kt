package com.example.museapp.di

import android.util.Log
import com.example.museapp.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

/**
 * Logs HTTP requests & responses in a single structured message.
 * - Redacts configured headers
 * - Peeks response body safely (doesn't consume stream)
 * - Truncates very large bodies
 */
class NetworkLoggingInterceptor(
    private val tag: String = "API",
    private val redactHeaders: Set<String> = setOf("Authorization", "Cookie", "Set-Cookie"),
    private val enabled: Boolean = BuildConfig.DEBUG,
    private val maxBodyLogSize: Int = 2000 // chars
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        if (!enabled) return chain.proceed(chain.request())

        val request = chain.request()
        val requestLine = "${request.method} ${request.url}"
        val t0 = System.nanoTime()

        // read request body safely (best-effort)
        val reqBodyStr = try {
            request.body?.let { body ->
                val buffer = Buffer()
                body.writeTo(buffer)
                val charset = body.contentType()?.charset(Charset.forName("UTF-8")) ?: Charset.forName("UTF-8")
                buffer.readString(charset)
            }
        } catch (t: Throwable) {
            "<<<unreadable-request-body>>>"
        }

        val reqHeaders = redactHeadersInHeaders(request.headers.toMultimap())

        Log.d(tag, "→ REQUEST: $requestLine\nheaders: $reqHeaders\nbody: ${truncate(reqBodyStr)}")

        val response: Response = try {
            chain.proceed(request)
        } catch (t: Throwable) {
            val durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0)
            Log.e(tag, "✖ NETWORK ERROR: $requestLine (${durationMs}ms) -> ${t.javaClass.simpleName}: ${t.message}")
            throw t
        }

        val durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0)

        // Peek body safely (up to some size) — does not consume response body
        val peek = try {
            response.peekBody(1024L * 1024L) // peek up to 1MB; adjust if needed
        } catch (t: Throwable) {
            null
        }
        val respBodyStr = try { peek?.string() } catch (t: Throwable) { "<<<unreadable-response-body>>>" }

        val respHeaders = redactHeadersInHeaders(response.headers.toMultimap())

        Log.d(
            tag,
            "← RESPONSE: ${response.code} ${response.message} for $requestLine (${durationMs}ms)\nheaders: $respHeaders\nbody: ${truncate(respBodyStr)}"
        )

        return response
    }

    private fun redactHeadersInHeaders(map: Map<String, List<String>>): String {
        return map.entries.joinToString(separator = ", ") { (k, v) ->
            val value = if (redactHeaders.any { it.equals(k, ignoreCase = true) }) "██" else v.joinToString(";")
            "$k: $value"
        }
    }

    private fun truncate(s: String?, max: Int = maxBodyLogSize): String {
        if (s == null) return "null"
        return if (s.length > max) s.take(max) + "...(truncated ${s.length - max} chars)" else s
    }
}
