package com.example.museapp.domain.repository

import com.example.museapp.data.util.NetworkResult
import com.example.museapp.domain.model.Ad
import com.example.museapp.domain.model.User

interface HomeRepository {
    suspend fun getAds(lat: Double? = null, lng: Double? = null, radiusKm: Int? = null): NetworkResult<List<Ad>>
    suspend fun getSkills(): NetworkResult<List<String>>

    // New: get users from backend (page, limit)
    suspend fun getUsers(page: Int = 1, limit: Int = 10): NetworkResult<List<User>>
}
