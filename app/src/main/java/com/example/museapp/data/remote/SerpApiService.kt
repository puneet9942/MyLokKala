package com.example.museapp.data.remote

import com.example.museapp.data.remote.dto.ExploreEventResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface SerpApiService {

    @GET("search.json")
    suspend fun getExploreEvents(
        @Query("engine") engine: String = "google_events",
        @Query("q") query: String,
        @Query("hl") hl: String = "en",
        @Query("gl") gl: String = "in",
        @Query("api_key") apiKey: String
    ): ExploreEventResponseDto
}