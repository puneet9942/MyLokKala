package com.example.museapp.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ExploreEventResponseDto(
    val events_results: List<EventDto>?   // ✅ NOT "events"
)

data class EventDto(
    val title: String?,
    val date: DateDto?,
    val address: List<String>?,
    val link: String?,
    val thumbnail: String?   // ✅ image
)

data class DateDto(
    @SerializedName("start_date")
    val startDate: String?
)