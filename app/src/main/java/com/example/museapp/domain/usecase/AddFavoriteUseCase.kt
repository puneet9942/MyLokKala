package com.example.museapp.domain.usecase

import com.example.museapp.data.util.NetworkResult
import com.example.museapp.domain.repository.FavoritesRepository
import javax.inject.Inject

class AddFavoriteUseCase @Inject constructor(
    private val repo: FavoritesRepository
) {
    suspend operator fun invoke(adId: String): NetworkResult<Unit> = repo.addFavorite(adId)
}