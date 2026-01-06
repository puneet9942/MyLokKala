package com.example.museapp.data.remote.dto

// in com.example.museapp.data.remote.dto (or wherever your DTOs live)
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass


data class PaginationDto(
    val page: Int? = null,
    val limit: Int? = null,
    val total: Int? = null,
    val totalPages: Int? = null
)