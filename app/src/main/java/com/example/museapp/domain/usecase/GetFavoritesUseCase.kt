package com.example.museapp.domain.usecase

import com.example.museapp.data.util.NetworkResult
import com.example.museapp.domain.model.Ad
import com.example.museapp.domain.repository.FavoritesRepository
import javax.inject.Inject

class GetFavoritesUseCase @Inject constructor(
    private val repo: FavoritesRepository
) {
    suspend operator fun invoke(): NetworkResult<List<Ad>> = repo.getFavorites()
}