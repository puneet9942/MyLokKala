package com.example.lokkala.data.util
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

suspend inline fun <T> safeApiCall(
    dispatcher: CoroutineDispatcher,
    crossinline block: suspend () -> T
): NetworkResult<T> = withContext(dispatcher) {
    try {
        NetworkResult.Success(block())
    } catch (e: HttpException) {
        val body = e.response()?.errorBody()?.string()
        NetworkResult.Error(code = e.code(), message = body ?: e.message(), throwable = e)
    } catch (e: IOException) {
        NetworkResult.Error(message = "Network error", throwable = e)
    } catch (e: Exception) {
        NetworkResult.Error(message = e.message, throwable = e)
    }
}