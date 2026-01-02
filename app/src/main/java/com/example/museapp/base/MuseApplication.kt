package com.example.museapp.base

import android.app.Application
import com.example.museapp.data.util.GlobalErrorHandler
import com.example.museapp.data.util.GlobalErrorHandlerHolder
import dagger.hilt.android.HiltAndroidApp
import jakarta.inject.Inject

/**
 * The main Application class for the LokKala app.
 *
 * This class is the entry point for the application and is responsible for
 * initializing the Hilt dependency injection framework. The `@HiltAndroidApp`
 * annotation triggers Hilt's code generation, which sets up the
 * application-level dependency container.
 */
@HiltAndroidApp
class MuseApplication: Application() {
    @Inject lateinit var globalErrorHandler: GlobalErrorHandler

    override fun onCreate() {
        super.onCreate()
        GlobalErrorHandlerHolder.handler = globalErrorHandler
    }
}