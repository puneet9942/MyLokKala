package com.example.lokkala.data.mocks

import android.content.Context
import com.example.lokkala.data.remote.dto.ApiResponse
import com.example.lokkala.data.remote.dto.EmptyData
import com.example.lokkala.data.remote.dto.VerifyOtpData
import com.example.lokkala.data.util.NetworkResult
import com.example.lokkala.domain.repository.AuthRepository
import com.example.lokkala.util.AssetUtils
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.delay

/**
 * A fake implementation of the [AuthRepository] for development and testing purposes.
 *
 * This repository simulates network requests for OTP and token verification by reading
 * predefined responses from JSON files stored in the app's assets folder. This allows for
 * UI development and testing without needing a live backend server. It is typically used
 * when the `USE_FAKE_REPO` build configuration flag is enabled.
 *
 * @param context The application context, used to access the asset manager.
 */
class FakeAuthRepository( private val context: Context) : AuthRepository {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    /**
     * Simulates a request to send an OTP to the user's phone.
     *
     * It introduces a delay to mimic network latency, then reads `otp_response.json` from
     * the assets. It parses the JSON and returns a [NetworkResult.Success] with the success
     * message if the mock response is valid, otherwise returns a [NetworkResult.Error].
     *
     * @param countryCode The country dialing code.
     * @param phone The user's phone number.
     * @return A [NetworkResult] containing a success message or an error.
     */
    override suspend fun requestOtp(countryCode: String, phone: String): NetworkResult<String> {
        delay(500) // simulate network delay
        val json = AssetUtils.readJsonAsset(context, "otp_response.json")
        val type = Types.newParameterizedType(ApiResponse::class.java, EmptyData::class.java)
        val adapter = moshi.adapter<ApiResponse<EmptyData>>(type)
        val response = json?.let { adapter.fromJson(it) }
        return if (response != null && response.success) {
            NetworkResult.Success(response.message)
        } else {
            NetworkResult.Error(message = response?.message ?: "Mock response error")
        }
    }

    /**
     * Simulates the verification of an OTP code provided by the user.
     *
     * It first checks if the OTP matches the hardcoded value "123456". If not, it returns
     * an "Invalid OTP" error. If correct, it reads `verify_otp_response.json`, parses it,
     * and returns a [NetworkResult.Success] containing the fake authentication token.
     *
     * @param countryCode The country dialing code.
     * @param phone The user's phone number.
     * @param otp The one-time password entered by the user.
     * @return A [NetworkResult] containing the authentication token on success, or an error.
     */
    override suspend fun verifyOtp(countryCode: String, phone: String, otp: String): NetworkResult<String> {
        delay(500)
        return if (otp == "123456") {
            val json = AssetUtils.readJsonAsset(context, "verify_otp_response.json")
            val type = Types.newParameterizedType(ApiResponse::class.java, VerifyOtpData::class.java)
            val adapter = moshi.adapter<ApiResponse<VerifyOtpData>>(type)
            val response = json?.let { adapter.fromJson(it) }
            if (response != null && response.success && response.data != null) {
                NetworkResult.Success(response.data.token)
            } else {
                NetworkResult.Error(message = response?.message ?: "Mock response error")
            }
        } else {
            // Return error if OTP is incorrect (simulate backend validation)
            NetworkResult.Error(message = "Invalid OTP")
        }
    }
}
