package com.example.museapp.data.remote.dto

import com.squareup.moshi.Json

data class FavoriteUserAddRequestDto(
    @Json(name = "userId") val userId: String
)

data class FavoriteUserAddResponseDto(
    @Json(name = "id") val id: String?,
    @Json(name = "userId") val userId: String?,
    @Json(name = "favoriteUserId") val favoriteUserId: String?,
    @Json(name = "createdAt") val createdAt: String?,
    @Json(name = "user") val user: UserDto?,
    @Json(name = "favoriteUser") val favoriteUser: UserDto?
)

data class FavoriteUserDto(
    @Json(name = "id") val id: String,
    @Json(name = "favoriteUser") val favoriteUser: UserDto,
    @Json(name = "createdAt") val createdAt: String?
)
