package com.example.lokkala.domain.repository

import com.example.lokkala.data.util.NetworkResult
import com.example.lokkala.domain.model.Ad

interface HomeRepository {
    suspend fun getAds(lat: Double? = null, lng: Double? = null, radiusKm: Int? = null): NetworkResult<List<Ad>>
    suspend fun getSkills(): NetworkResult<List<String>>
}