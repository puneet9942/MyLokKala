package com.example.museapp.domain.repository

import com.example.museapp.data.util.NetworkResult
import com.example.museapp.domain.model.Feedback

interface FeedbackRepository {
    suspend fun sendFeedback(feedback: String): NetworkResult<Feedback>
}