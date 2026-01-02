package com.example.museapp.domain.usecase


import com.example.museapp.data.util.NetworkResult
import com.example.museapp.domain.model.Feedback
import com.example.museapp.domain.repository.FeedbackRepository
import javax.inject.Inject

class SendFeedbackUseCase @Inject constructor(
    private val repository: FeedbackRepository
) {
    suspend operator fun invoke(feedback: String): NetworkResult<Feedback> {
        return repository.sendFeedback(feedback)
    }
}
