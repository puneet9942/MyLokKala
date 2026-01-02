package com.example.museapp.data.util

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultGlobalErrorHandler @Inject constructor(
    private val broadcaster: AppErrorBroadcaster
) : GlobalErrorHandler {

    override fun onHttpError(code: Int, parsed: ParsedApiError?, rawBody: String?, throwable: Throwable?) {
        try {
            val display = parsed?.message ?: rawBody ?: throwable?.message ?: "An error occurred"
            when {
                code == 401 -> broadcaster.broadcast(AppError.AuthFailure(display))
                code == 429 -> broadcaster.broadcast(AppError.TooManyRequests(display))
                code in 500..599 -> broadcaster.broadcast(AppError.ServerError(display))
                else -> broadcaster.broadcast(AppError.Unknown(display))
            }
        } catch (t: Throwable) {
            Log.w("DefaultGlobalErrorHandler", "handler failed", t)
        }
    }

    override fun onNetworkError(throwable: Throwable) {
        broadcaster.broadcast(AppError.Network(throwable.message ?: "Network error"))
    }

    override fun onUnknownError(throwable: Throwable) {
        broadcaster.broadcast(AppError.Unknown(throwable.message ?: "Unknown error"))
    }
}
