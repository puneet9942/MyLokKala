package com.example.lokkala.data.repository

import com.example.lokkala.data.remote.ApiService
import com.example.lokkala.data.remote.mapper.toDomain
import com.example.lokkala.domain.repository.HomeRepository
import com.example.lokkala.data.util.NetworkResult
import com.example.lokkala.data.util.safeApiCall
import com.example.lokkala.domain.model.Ad
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class HomeRepositoryImpl @Inject constructor(
    private val api: ApiService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : HomeRepository {

    override suspend fun getAds(
        lat: Double?,
        lng: Double?,
        radiusKm: Int?
    ): NetworkResult<List<Ad>> {
        // use your safeApiCall which returns NetworkResult<T>
        return safeApiCall(ioDispatcher) {
            val response = api.getAds(lat, lng, radiusKm)
            if (response.success) {
                val dtoList = response.data ?: emptyList()
                // map AdDto -> domain Ad
                dtoList.map { it.toDomain() }
            } else {
                throw Exception(response.message ?: "Failed to fetch ads")
            }
        }
    }

    override suspend fun getSkills(): NetworkResult<List<String>> {
        return safeApiCall(ioDispatcher) {
            val response = api.getSkills()
            if (response.success) {
                response.data ?: emptyList()
            } else {
                throw Exception(response.message ?: "Failed to fetch skills")
            }
        }
    }
}