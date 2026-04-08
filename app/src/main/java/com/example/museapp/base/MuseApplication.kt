package com.example.museapp.base

import android.app.Application
import com.example.museapp.data.util.GlobalErrorHandler
import com.example.museapp.data.util.GlobalErrorHandlerHolder
import dagger.hilt.android.HiltAndroidApp
import jakarta.inject.Inject

/**
 * Main Application class for the app.
 *
 * Acts as the entry point and initializes Hilt for dependency injection.
 * Also sets up the global error handler used across the application.
 */
@HiltAndroidApp
class MuseApplication : Application() {

    /**
     * Injected global error handler instance.
     * Used to handle uncaught or centralized errors across the app.
     */
    @Inject
    lateinit var globalErrorHandler: GlobalErrorHandler

    override fun onCreate() {
        super.onCreate()

        // Assign the injected handler to a globally accessible holder
        GlobalErrorHandlerHolder.handler = globalErrorHandler
    }
}