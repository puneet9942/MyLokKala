package com.example.museapp.data.util

import com.example.museapp.util.LoadingManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

/**
 * Canonical safeApiCall: repositories must pass a block that returns T or throws.
 * This converts results to NetworkResult<T>, parses API error bodies, and notifies a registered GlobalErrorHandler.
 * Also notifies LoadingManager so UI can show/hide a global loader.
 */
suspend inline fun <T> safeApiCall(
    dispatcher: CoroutineDispatcher,
    crossinline block: suspend () -> T
): NetworkResult<T> = withContext(dispatcher) {
    LoadingManager.show()
    try {
        val result = block()
        return@withContext NetworkResult.Success(result)
    } catch (e: HttpException) {
        val code = e.code()
        val body = try { e.response()?.errorBody()?.string() } catch (_: Throwable) { null }
        val parsed = try { parseApiErrorBody(body) } catch (_: Throwable) { null }

        try { GlobalErrorHandlerHolder.handler?.onHttpError(code = code, parsed = parsed, rawBody = body, throwable = e) } catch (_: Throwable) {}

        val message = parsed?.message ?: parsed?.error?.code ?: body ?: e.message()
        @Suppress("UNCHECKED_CAST")
        return@withContext NetworkResult.Error(code = code, message = message, throwable = e) as NetworkResult<T>
    } catch (e: IOException) {
        try { GlobalErrorHandlerHolder.handler?.onNetworkError(e) } catch (_: Throwable) {}
        @Suppress("UNCHECKED_CAST")
        return@withContext NetworkResult.Error(message = "Network error", throwable = e) as NetworkResult<T>
    } catch (e: Exception) {
        try { GlobalErrorHandlerHolder.handler?.onUnknownError(e) } catch (_: Throwable) {}
        @Suppress("UNCHECKED_CAST")
        return@withContext NetworkResult.Error(message = e.message, throwable = e) as NetworkResult<T>
    } finally {
        LoadingManager.hide()
    }
}
