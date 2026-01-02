package com.example.museapp.domain.repository

import com.example.museapp.data.util.NetworkResult
import com.example.museapp.domain.model.FavoriteUser

interface UserFavoritesRepository {
    suspend fun getMyFavorites(): NetworkResult<List<FavoriteUser>>
    suspend fun addFavorite(userId: String): NetworkResult<FavoriteUser>
    suspend fun removeFavorite(favoriteId: String): NetworkResult<Unit>
}
