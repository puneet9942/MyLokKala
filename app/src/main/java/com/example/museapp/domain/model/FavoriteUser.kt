package com.example.museapp.domain.model

data class FavoriteUser(
    val id: String,
    val favoriteUser: User,
    val createdAt: String? = null
)
