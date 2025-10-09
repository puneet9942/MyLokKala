package com.example.lokkala.data.remote.mapper

import com.example.lokkala.data.remote.dto.AdDto
import com.example.lokkala.data.remote.dto.UserDto
import com.example.lokkala.domain.model.Ad
import com.example.lokkala.domain.model.User


fun UserDto.toDomain(): User = User(
    id = id,
    name = name,
    subtitle = subtitle,
    lat = lat,
    lng = lng,
    rating = rating,
    reviewsCount = reviewsCount,
    imageUrl = imageUrl,
    skills = skills
)

fun AdDto.toDomain(): Ad = Ad(
    id = id,
    user = user.toDomain(),
    primarySkill = primarySkill,
    priceMin = priceMin,
    priceMax = priceMax,
    description = description,
    createdAt = createdAt
)