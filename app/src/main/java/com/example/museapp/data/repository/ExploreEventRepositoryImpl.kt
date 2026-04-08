package com.example.museapp.data.repository

import com.example.museapp.BuildConfig
import com.example.museapp.data.remote.SerpApiService
import com.example.museapp.domain.model.ExploreEvent
import com.example.museapp.domain.repository.ExploreEventRepository
import javax.inject.Inject

class ExploreEventRepositoryImpl @Inject constructor(
    private val serpApi: SerpApiService
) : ExploreEventRepository {

    override suspend fun getExploreEvents(query: String): Result<List<ExploreEvent>> {
        return try {
            val response = serpApi.getExploreEvents(
                query = query,
                apiKey = BuildConfig.SERP_API_KEY
            )

            val events = response.events_results?.map { eventDto ->
                ExploreEvent(
                    title = eventDto.title ?: "",
                    date = eventDto.date?.startDate ?: "",
                    location = eventDto.address?.joinToString(", ") ?: "",
                    imageUrl = eventDto.thumbnail ?: "",   // ✅ FIX
                    ticketLink = eventDto.link ?: "",
                    link = TODO()       // ✅ FIX
                )
            } ?: emptyList()

            Result.success(events)

        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}