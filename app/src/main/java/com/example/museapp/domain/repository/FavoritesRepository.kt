package com.example.museapp.domain.repository

import com.example.museapp.data.util.NetworkResult
import com.example.museapp.domain.model.Ad

interface FavoritesRepository {
    suspend fun getFavorites(): NetworkResult<List<Ad>>
    suspend fun addFavorite(adId: String): NetworkResult<Unit>
    suspend fun removeFavorite(adId: String): NetworkResult<Unit>
}