package com.example.museapp.data.repository

import com.example.museapp.data.remote.ApiService
import com.example.museapp.data.remote.mapper.toDomain
import com.example.museapp.domain.model.User
import com.example.museapp.domain.repository.HomeRepository
import com.example.museapp.data.util.NetworkResult
import com.example.museapp.data.util.safeApiCall
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
    ): NetworkResult<List<com.example.museapp.domain.model.Ad>> {
        // existing implementation kept intact (no change)
        return safeApiCall(ioDispatcher) {
            val resp = api.getAds(lat = lat, lng = lng, radiusKm = radiusKm)
            // assume existing mapping exists similar to ad mapper in repo
            resp.data?.let { dt ->
                dt.map { it.toDomain() }
            } ?: emptyList()
        }
    }

    override suspend fun getSkills(): NetworkResult<List<String>> {
        return safeApiCall(ioDispatcher) {
            val resp = api.getSkills()
            resp.data ?: emptyList()
        }
    }

    // New implementation to fetch users from backend and map to domain model
    override suspend fun getUsers(page: Int, limit: Int): NetworkResult<List<User>> {
        return safeApiCall(ioDispatcher) {
            val resp = api.getAllUsers(page = page, limit = limit)
            // ApiResponse<T> wraps UsersDataDto which contains 'users'
            val usersDto = (resp.data as? com.example.museapp.data.remote.dto.UsersDataDto)
                ?: resp.data as? com.example.museapp.data.remote.dto.UsersDataDto
            // Some ApiResponse generic uses a typed T; however, to be safe, rely on resp.data?.users if typed
            // If your ApiResponse<T> already returns UsersDataDto in resp.data, then:
            val maybeUsers = resp.data
            // Try to get users explicitly (if API model is UsersDataDto)
            val usersList = when (val d = resp.data) {
                is com.example.museapp.data.remote.dto.UsersDataDto -> d.users
                is com.example.museapp.data.remote.dto.UsersApiResponse -> d.data?.users ?: emptyList()
                else -> emptyList()
            }
            usersList.map { it.toDomain() }
        }
    }
}
