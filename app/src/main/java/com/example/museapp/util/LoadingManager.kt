package com.example.museapp.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicInteger

/**
 * App-wide loading manager. Tracks concurrent operations and exposes a StateFlow<Boolean>.
 *
 * Usage:
 *   LoadingManager.show()   // increment
 *   LoadingManager.hide()   // decrement
 * The flow LoadingManager.isLoading emits true when count > 0 and false when count == 0.
 *
 * NOTE: This was moved from ui.loading to util so it can be used by any layer
 * without creating UI-to-data package dependencies.
 */
object LoadingManager {
    private val counter = AtomicInteger(0)
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun show() {
        val current = counter.incrementAndGet()
        if (current > 0) _isLoading.value = true
    }

    fun hide() {
        val current = counter.decrementAndGet().coerceAtLeast(0)
        if (current == 0) _isLoading.value = false
    }

    fun reset() {
        counter.set(0)
        _isLoading.value = false
    }
}
