package com.example.museapp.data.remote.dto

// in com.example.museapp.data.remote.dto (or wherever your DTOs live)
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

//@JsonClass(generateAdapter = true)
//data class InterestDto(
//    @Json(name = "id") val id: String? = null,
//    @Json(name = "remote_id") val remoteId: String? = null,
//    @Json(name = "name") val name: String? = null,
//)


data class PaginationDto(
    val page: Int? = null,
    val limit: Int? = null,
    val total: Int? = null,
    val totalPages: Int? = null
)