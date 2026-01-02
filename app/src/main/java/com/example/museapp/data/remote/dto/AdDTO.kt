package com.example.museapp.data.remote.dto

data class AdDto(
    val id: String,
    val user: UserDto,
    val primarySkill: String,
    val priceMin: Int,
    val priceMax: Int,
    val description: String? = null,
    val createdAt: String? = null
)