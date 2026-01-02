package com.example.museapp.domain.usecase

import com.example.museapp.domain.repository.FavoritesRepository
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val repo: FavoritesRepository
) {
    suspend operator fun invoke(adId: String, currentlyFavorite: Boolean) {
        if (currentlyFavorite) repo.removeFavorite(adId)
        else repo.addFavorite(adId)
    }
}