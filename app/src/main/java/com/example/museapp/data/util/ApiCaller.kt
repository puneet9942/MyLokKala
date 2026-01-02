package com.example.museapp.data.util

import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Small injectable wrapper around safeApiCall to avoid touching many repositories
 * when you want to pass dispatcher (and later add other cross-cutting concerns).
 *
 * NOTE: the call() method is intentionally non-inline to avoid Kotlin's
 * "Public-API inline function cannot access non-public-API property" compiler error.
 */
@Singleton
class ApiCaller @Inject constructor(
    private val ioDispatcher: CoroutineDispatcher
) {
    // No inline needed — it's a simple suspend wrapper
    suspend fun <T> call(block: suspend () -> T): NetworkResult<T> =
        safeApiCall(ioDispatcher, block)
}
