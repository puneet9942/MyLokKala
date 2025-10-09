package com.example.lokkala.data.remote.dto

data class ApiResponse<T>(
    val success: Boolean,
    val code: Int,
    val message: String,
    val data: T? = null,
    val timestamp: String? = null
)