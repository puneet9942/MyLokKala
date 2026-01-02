package com.example.museapp.data.repository

import com.example.museapp.data.remote.ApiService
import com.example.museapp.data.remote.mapper.toDomain
import com.example.museapp.domain.repository.FavoritesRepository
import com.example.museapp.data.util.NetworkResult
import com.example.museapp.data.util.safeApiCall
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class FavoritesRepositoryImpl @Inject constructor(
    private val api: ApiService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : FavoritesRepository {

    override suspend fun getFavorites(): NetworkResult<List<com.example.museapp.domain.model.Ad>> {
        return safeApiCall(ioDispatcher) {
            val response = api.getFavorites()
            if (response.success == true) {
                (response.data ?: emptyList()).map { it.toDomain() }
            } else {
                throw Exception(response.message ?: "Failed to fetch favorites")
            }
        }
    }

    override suspend fun addFavorite(adId: String): NetworkResult<Unit> {
        return safeApiCall(ioDispatcher) {
            val response = api.addFavorite(adId)
            if (response.success == true) Unit else throw Exception(response.message ?: "Failed to add favorite")
        }
    }

    override suspend fun removeFavorite(adId: String): NetworkResult<Unit> {
        return safeApiCall(ioDispatcher) {
            val response = api.removeFavorite(adId)
            if (response.success == true) Unit else throw Exception(response.message ?: "Failed to remove favorite")
        }
    }
}
