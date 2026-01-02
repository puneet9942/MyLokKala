package com.example.museapp.data.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultAppErrorBroadcaster @Inject constructor() : AppErrorBroadcaster {
    private val _flow = MutableSharedFlow<AppError>(replay = 0)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun broadcast(error: AppError) {
        val emitted = _flow.tryEmit(error)
        if (!emitted) {
            scope.launch { _flow.emit(error) }
        }
    }

    override fun errors() = _flow.asSharedFlow()
}
