package com.example.museapp.data.mocks

import android.content.Context
import android.util.Log
import com.example.museapp.data.remote.dto.FeedbackResponseDto
import com.example.museapp.data.remote.mapper.toDomain
import com.example.museapp.data.util.NetworkResult
import com.example.museapp.domain.model.Feedback
import com.example.museapp.domain.repository.FeedbackRepository
import com.example.museapp.util.AssetUtils
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val TAG = "FAKE_FEEDBACK"

class FakeFeedbackRepository(private val context: Context) : FeedbackRepository {
    override suspend fun sendFeedback(feedback: String): NetworkResult<Feedback> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Fake sendFeedback called length=${feedback.length}")
        delay(250)
        try {
            val json = AssetUtils.readJsonAsset(context, "feedback_send_response.json")
                ?: return@withContext NetworkResult.Error(message = "Missing feedback_send_response.json")
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val adapter = moshi.adapter(FeedbackResponseDto::class.java)
            val dto = adapter.fromJson(json) ?: return@withContext NetworkResult.Error(message = "Invalid mock JSON")
            Log.d(TAG, "parsed mock dto = $dto")
            NetworkResult.Success(dto.toDomain())
        } catch (t: Throwable) {
            Log.e(TAG, "fake repo failed", t)
            NetworkResult.Error(message = t.message ?: "Mock error", throwable = t)
        }
    }
}
