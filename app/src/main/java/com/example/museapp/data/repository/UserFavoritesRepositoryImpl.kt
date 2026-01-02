package com.example.museapp.data.repository

import com.example.museapp.data.remote.ApiService
import com.example.museapp.data.remote.dto.FavoriteUserAddRequestDto
import com.example.museapp.data.remote.mapper.toDomain
import com.example.museapp.domain.model.FavoriteUser
import com.example.museapp.domain.repository.UserFavoritesRepository
import com.example.museapp.data.util.NetworkResult
import com.example.museapp.data.util.safeApiCall
import com.example.museapp.util.CommonRequestBuilder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class UserFavoritesRepositoryImpl @Inject constructor(
    private val api: ApiService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : UserFavoritesRepository {

    override suspend fun getMyFavorites(): NetworkResult<List<FavoriteUser>> {
        return safeApiCall(ioDispatcher) {
            val resp = api.getMyUserFavorites()
            if (resp.isSuccessful()) {
                val dtoList = resp.data ?: emptyList()
                dtoList.map { dto ->
                    FavoriteUser(
                        id = dto.id,
                        favoriteUser = dto.favoriteUser.toDomain(),
                        createdAt = dto.createdAt
                    )
                }
            } else {
                throw Exception(resp.message ?: "Failed to fetch favorites")
            }
        }
    }

    override suspend fun addFavorite(userId: String): NetworkResult<FavoriteUser> {
        return safeApiCall(ioDispatcher) {
            // Build the common request using your project's CommonRequestBuilder util (NOT hardcoded).
            val payload = FavoriteUserAddRequestDto(userId = userId)
            val req = CommonRequestBuilder.buildWithLiveLocation(payload)
            val resp = api.addUserFavorite(req)
            if (resp.isSuccessful()) {
                val data = resp.data ?: throw Exception(resp.message ?: "Empty add favorite response")
                val userDto = data.favoriteUser ?: data.user
                ?: throw Exception("Missing favorite user in response")
                FavoriteUser(
                    id = data.id ?: throw Exception("Missing favorite id"),
                    favoriteUser = userDto.toDomain(),
                    createdAt = data.createdAt
                )
            } else {
                throw Exception(resp.message ?: "Failed to add favorite")
            }
        }
    }

    override suspend fun removeFavorite(favoriteId: String): NetworkResult<Unit> {
        return safeApiCall(ioDispatcher) {
            val resp = api.removeUserFavorite(favoriteId)
            if (resp.isSuccessful()) {
                Unit
            } else {
                throw Exception(resp.message ?: "Failed to remove favorite")
            }
        }
    }
}
