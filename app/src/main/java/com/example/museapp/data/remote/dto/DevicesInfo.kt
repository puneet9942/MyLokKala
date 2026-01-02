package com.example.museapp.data.remote.dto

import com.squareup.moshi.Json

data class DevicesInfo(
    @Json(name = "device_model") val deviceModel: String,
    @Json(name = "os_version") val osVersion: String,
    @Json(name = "app_version") val appVersion: String
)
