package com.example.museapp.util

import android.annotation.SuppressLint
import android.content.Context

/**
 * Tiny, thread-safe application context holder.
 * Initialize early from your DI (NetworkModule) so utilities can access SharedPreferences safely.
 */
@SuppressLint("StaticFieldLeak")
object AppContextProvider {
    @Volatile
    private var _context: Context? = null

    /**
     * Nullable accessor for safe use in utilities that must not crash.
     */
    fun getContext(): Context? = _context

    /**
     * Non-null accessor for places that must have been initialized (use sparingly).
     */
    fun requireContext(): Context = _context
        ?: throw IllegalStateException("AppContextProvider not initialized. Call AppContextProvider.init(appContext) early in Application/DI.")

    /**
     * Initialize with application context. Idempotent.
     * Call from NetworkModule.provideSharedPreferences or Application on startup.
     */
    fun init(appContext: Context) {
        _context = appContext.applicationContext
    }

    /**
     * Clear stored context (only for tests).
     */
    fun clear() {
        _context = null
    }
}
