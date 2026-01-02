package com.example.museapp.domain.repository

import com.example.museapp.data.local.entity.InterestEntity
import kotlinx.coroutines.flow.Flow

interface InterestsRepository {
    suspend fun fetchAndSaveInterests(page: Int, limit: Int)
    fun getAllInterests(): Flow<List<InterestEntity>>
}
