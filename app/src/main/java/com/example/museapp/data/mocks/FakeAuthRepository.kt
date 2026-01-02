package com.example.museapp.data.mocks

import android.content.Context
import android.util.Log
import com.example.museapp.data.auth.dto.SendOtpData
import com.example.museapp.data.auth.dto.VerifyOtpData
import com.example.museapp.data.remote.dto.ApiResponse
import com.example.museapp.data.util.NetworkResult
import com.example.museapp.domain.repository.AuthRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.io.InputStreamReader
import java.lang.reflect.Type
import javax.inject.Inject

/**
 * A simple fake auth repository that reads JSON assets (useful for local dev).
 * I kept the behaviour similar to your previous mock implementations:
 * - networkDelayMs controls simulated latency
 * - shouldFail forces an error for testing
 *
 * This fake now expects the verify_otp JSON to contain the new access_token/refresh_token fields.
 */
class FakeAuthRepository @Inject constructor(
    private val context: Context,
    private val networkDelayMs: Long = 500L,
    private val shouldFail: Boolean = false
) : AuthRepository {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    private inline fun <reified T> readAssetJson(assetPath: String): T? {
        return try {
            context.assets.open(assetPath).use { stream ->
                val reader = InputStreamReader(stream, "UTF-8")
                val type: Type = Types.newParameterizedType(ApiResponse::class.java, T::class.java)
                val adapter = moshi.adapter<ApiResponse<T>>(type)
                val apiResp = adapter.fromJson(reader.readText())
                apiResp?.data as T?
            }
        } catch (fnf: FileNotFoundException) {
            Log.w(TAG, "Asset not found: $assetPath")
            null
        } catch (ex: Exception) {
            Log.e(TAG, "Failed to parse asset $assetPath: ${ex.message}", ex)
            null
        }
    }

    override suspend fun requestOtp(countryCode: String, phone: String): NetworkResult<SendOtpData> =
        withContext(Dispatchers.IO) {
            delay(networkDelayMs)
            if (shouldFail) {
                return@withContext NetworkResult.Error(code = 500, message = "Forced failure (fake)")
            }

            // Try to load mock send otp response
            val sendData = readAssetJson<SendOtpData>("send_otp_response.json")
                ?: SendOtpData(true, "sms", 120) // fallback fake shape (adjust to your SendOtpData)
            NetworkResult.Success(sendData)
        }

    override suspend fun resendOtp(countryCode: String, phone: String): NetworkResult<SendOtpData> =
        withContext(Dispatchers.IO) {
            delay(networkDelayMs)
            if (shouldFail) {
                return@withContext NetworkResult.Error(code = 500, message = "Forced failure (fake)")
            }
            // Reuse same mock
            val sendData = readAssetJson<SendOtpData>("send_otp_response.json")
                ?: SendOtpData(true,"sms", 120 )
            NetworkResult.Success(sendData)
        }

    override suspend fun verifyOtp(countryCode: String, phone: String, otp: String): NetworkResult<VerifyOtpData> =
        withContext(Dispatchers.IO) {
            delay(networkDelayMs)
            if (shouldFail) {
                return@withContext NetworkResult.Error(code = 500, message = "Forced failure (fake)")
            }

            // Read the verify_otp response asset (the new JSON)
            val verifyData = readAssetJson<VerifyOtpData>("verify_otp_response.json")
            if (verifyData != null) {
                NetworkResult.Success(verifyData)
            } else {
                // fallback to construct a minimal VerifyOtpData so callers don't fail
                val fallback = VerifyOtpData(
                    user = null,
                    access_token = "fake_access_token",
                    refresh_token = null,
                    expires_in = null,
                    token_type = null,
                )
                NetworkResult.Success(fallback)
            }
        }

    companion object {
        private const val TAG = "FakeAuthRepository"
    }
}
