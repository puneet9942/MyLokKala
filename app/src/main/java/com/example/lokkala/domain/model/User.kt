package com.example.lokkala.domain.model

data class User(
    val id: String,
    val name: String,
    val subtitle: String? = null,
    val lat: Double,
    val lng: Double,
    val rating: Double = 0.0,
    val reviewsCount: Int = 0,
    val imageUrl: String? = null,
    val skills: List<String> = emptyList()
)