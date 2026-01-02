package com.example.museapp.di

import android.util.Log
import com.example.museapp.data.store.TokenStore
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

class AuthInterceptor(
    private val tokenStore: TokenStore
) : Interceptor {

    private val TAG = "AuthInterceptor"

    private val excludedPaths = setOf(
        "/api/auth/send-otp",
        "/api/auth/resend-otp",
        "/api/auth/verify-otp"
    )

    override fun intercept(chain: Interceptor.Chain): Response {
        val original: Request = chain.request()
        val path = original.url.encodedPath.trimEnd('/')

        if (isExcludedPath(path)) {
            Log.d(TAG, "Skipping auth header for excluded path: $path")
            return chain.proceed(original)
        }

        // Do not overwrite existing Authorization header if present
        if (original.header("Authorization") != null) {
            Log.d(TAG, "Request already has Authorization header; not overwriting")
            return chain.proceed(original)
        }

        val token = tokenStore.getToken()

        if (token.isNullOrBlank()) {
            Log.d(TAG, "No token present in TokenStore; proceeding without Authorization header")
            return chain.proceed(original)
        }

        // Log masked token info (safe): show first/last 4 chars or length
        val masked = if (token.length > 8) "${token.take(4)}...${token.takeLast(4)}" else "len=${token.length}"
        Log.d(TAG, "Attaching Authorization header, token (masked)=$masked")

        val authorized = original.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        Log.d(TAG, "Request body:$original");

        return chain.proceed(authorized)
    }

    private fun isExcludedPath(path: String): Boolean {
        return excludedPaths.any { it.equals(path, ignoreCase = true) }
    }
}
