package com.example.lokkala.domain.usecase

import com.example.lokkala.data.util.NetworkResult
import com.example.lokkala.domain.model.Ad
import com.example.lokkala.domain.repository.HomeRepository
import javax.inject.Inject

class GetAdsUseCase @Inject constructor(
    private val repo: HomeRepository
) {
    suspend operator fun invoke(lat: Double? = null, lng: Double? = null, radiusKm: Int? = null): NetworkResult<List<Ad>> {
        return repo.getAds(lat, lng, radiusKm)
    }
}