package com.example.museapp.data.remote.dto

import com.squareup.moshi.Json

data class InterestsDataDto(
    // direct lists
    @Json(name = "items") val items: List<InterestDto>? = null,
    @Json(name = "interests") val interests: List<InterestDto>? = null,
    @Json(name = "dataList") val dataList: List<InterestDto>? = null,

    // nested container: data: { items: [...] }
    @Json(name = "data") val nestedData: NestedInterestsDto? = null,

    // pagination object (nullable — prevents unresolved reference)
    @Json(name = "pagination") val pagination: PaginationDto? = null,

    // also tolerate top-level page/limit/total fields sometimes present
    @Json(name = "page") val page: Int? = null,
    @Json(name = "limit") val limit: Int? = null,
    @Json(name = "total") val total: Int? = null,
    @Json(name = "totalPages") val totalPages: Int? = null
)

data class NestedInterestsDto(
    @Json(name = "items") val items: List<InterestDto>? = null,
    @Json(name = "interests") val interests: List<InterestDto>? = null,
    @Json(name = "pagination") val pagination: PaginationDto? = null
)
