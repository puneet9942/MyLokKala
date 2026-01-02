package com.example.museapp.data.util

import kotlinx.coroutines.flow.SharedFlow

interface AppErrorBroadcaster {
    fun broadcast(error: AppError)
    fun errors(): SharedFlow<AppError>
}
