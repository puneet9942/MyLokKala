package com.example.museapp.domain.usecase

import com.example.museapp.data.util.NetworkResult
import com.example.museapp.domain.model.Ad
import com.example.museapp.domain.repository.HomeRepository
import javax.inject.Inject

class GetAdsUseCase @Inject constructor(
    private val repo: HomeRepository
) {
    suspend operator fun invoke(lat: Double? = null, lng: Double? = null, radiusKm: Int? = null): NetworkResult<List<Ad>> {
        return repo.getAds(lat, lng, radiusKm)
    }
}