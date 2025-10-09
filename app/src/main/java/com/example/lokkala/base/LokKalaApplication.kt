package com.example.lokkala.base

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * The main Application class for the LokKala app.
 *
 * This class is the entry point for the application and is responsible for
 * initializing the Hilt dependency injection framework. The `@HiltAndroidApp`
 * annotation triggers Hilt's code generation, which sets up the
 * application-level dependency container.
 */
@HiltAndroidApp
class LokKalaApplication: Application() {
}