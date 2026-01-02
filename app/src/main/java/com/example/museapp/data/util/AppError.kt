package com.example.museapp.data.util

sealed class AppError {
    data class TooManyRequests(val message: String) : AppError()
    data class ServerError(val message: String) : AppError()
    data class Network(val message: String) : AppError()
    data class AuthFailure(val message: String? = null) : AppError()
    data class Unknown(val message: String) : AppError()
    data class InlineFieldError(val field: String, val message: String) : AppError()
}
