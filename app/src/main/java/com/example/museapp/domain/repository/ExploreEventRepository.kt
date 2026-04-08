package com.example.museapp.domain.repository

import com.example.museapp.domain.model.ExploreEvent

interface ExploreEventRepository {
    suspend fun getExploreEvents(query: String): Result<List<ExploreEvent>>
}