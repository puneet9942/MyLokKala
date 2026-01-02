package com.example.museapp.data.remote.dto

import com.squareup.moshi.Json

data class UsersApiResponse(
    @Json(name = "status") val status: String? = null,
    @Json(name = "status_code") val status_code: Int? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "data") val data: UsersDataDto? = null,
    @Json(name = "error") val error: Any? = null
)
