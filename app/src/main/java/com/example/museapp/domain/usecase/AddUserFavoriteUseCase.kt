package com.example.museapp.domain.usecase

import com.example.museapp.data.util.NetworkResult
import com.example.museapp.domain.model.FavoriteUser
import com.example.museapp.domain.repository.UserFavoritesRepository
import javax.inject.Inject

class AddUserFavoriteUseCase @Inject constructor(
    private val repo: UserFavoritesRepository
) {
    suspend operator fun invoke(userId: String): NetworkResult<FavoriteUser> = repo.addFavorite(userId)
}
