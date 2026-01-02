package com.example.museapp.domain.model

data class Feedback(
    val id: String,
    val userId: String?,
    val feedback: String,
    val createdAt: String?,
    val updatedAt: String?
)
