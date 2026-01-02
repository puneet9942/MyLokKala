package com.example.museapp.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ProfileDataDto(
    val user: UserDto? = null
)
