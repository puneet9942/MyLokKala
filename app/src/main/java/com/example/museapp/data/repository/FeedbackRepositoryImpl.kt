package com.example.museapp.data.repository

import android.util.Log
import com.example.museapp.data.remote.ApiService
import com.example.museapp.data.remote.dto.FeedbackDataDto
import com.example.museapp.data.remote.dto.FeedbackRequestDto
import com.example.museapp.data.remote.dto.FeedbackResponseDto
import com.example.museapp.data.remote.mapper.toDomain
import com.example.museapp.data.util.NetworkResult
import com.example.museapp.data.util.safeApiCall
import com.example.museapp.domain.model.Feedback
import com.example.museapp.domain.repository.FeedbackRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

private const val TAG = "FEEDBACK_REPO"

class FeedbackRepositoryImpl @Inject constructor(
    private val api: ApiService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : FeedbackRepository {

    override suspend fun sendFeedback(feedback: String): NetworkResult<Feedback> {
        Log.d(TAG, "sendFeedback called length=${feedback.length}")
        return safeApiCall(ioDispatcher) {
            val request = FeedbackRequestDto(feedback = feedback)
            Log.d(TAG, "sending request: $request")

            // Call API expecting ApiResponse<FeedbackDataDto>
            val response = api.sendFeedback(request)

            Log.d(TAG, "api responded success=${response.success} message=${response.message}")

            // Handle "data": { "feedback": {...} } shape first
            val dataObj = response.data
            if (dataObj is FeedbackDataDto) {
                val inner = dataObj.feedback
                if (inner != null) {
                    Log.d(TAG, "mapped data.feedback -> id=${inner.id} userId=${inner.userId}")
                    return@safeApiCall inner.toDomain()
                }
            }

            // If we didn't get the wrapper shape, try fallback (some APIs returned the feedback DTO directly)
            try {
                // try to cast response.data to FeedbackResponseDto if possible
                val fallback = response.data as? FeedbackResponseDto
                if (fallback != null) {
                    Log.d(TAG, "mapped direct data -> id=${fallback.id} userId=${fallback.userId}")
                    return@safeApiCall fallback.toDomain()
                }
            } catch (t: Throwable) {
                // ignore and fall through to error
            }

            throw Exception(response.message ?: "Failed to parse feedback response")
        }
    }
}
