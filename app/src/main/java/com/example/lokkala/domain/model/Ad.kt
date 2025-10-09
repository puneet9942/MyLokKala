package com.example.lokkala.domain.model

data class Ad(
    val id: String,
    val user: User,
    val primarySkill: String,
    val priceMin: Int,
    val priceMax: Int,
    val description: String? = null,
    val createdAt: String? = null
)