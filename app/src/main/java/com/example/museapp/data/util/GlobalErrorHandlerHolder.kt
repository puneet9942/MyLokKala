package com.example.museapp.data.util

/**
 * Simple holder to register a DI-provided GlobalErrorHandler at runtime.
 * Register once in Application.onCreate(): GlobalErrorHandlerHolder.handler = yourHandler
 */
object GlobalErrorHandlerHolder {
    var handler: GlobalErrorHandler? = null
}
