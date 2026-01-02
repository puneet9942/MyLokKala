package com.example.museapp.data.util

/**
 * Optional app-global handler for HTTP / network / unknown errors.
 */
interface GlobalErrorHandler {
    fun onHttpError(code: Int, parsed: ParsedApiError?, rawBody: String?, throwable: Throwable?)
    fun onNetworkError(throwable: Throwable)
    fun onUnknownError(throwable: Throwable)
}
