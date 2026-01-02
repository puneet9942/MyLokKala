package com.example.museapp.domain.usecase

import com.example.museapp.data.util.NetworkResult
import com.example.museapp.domain.repository.UserFavoritesRepository
import javax.inject.Inject

class RemoveUserFavoriteUseCase @Inject constructor(
    private val repo: UserFavoritesRepository
) {
    suspend operator fun invoke(favoriteId: String): NetworkResult<Unit> = repo.removeFavorite(favoriteId)
}
