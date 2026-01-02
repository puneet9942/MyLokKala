package com.example.museapp.data.repository

import com.example.museapp.data.auth.dto.OtpPayload
import com.example.museapp.data.auth.dto.SendOtpData
import com.example.museapp.data.auth.dto.VerifyOtpData
import com.example.museapp.data.auth.dto.VerifyOtpPayload
import com.example.museapp.data.remote.ApiService
import com.example.museapp.data.remote.dto.ApiResponse
import com.example.museapp.data.store.UserStore
import com.example.museapp.data.store.TokenStore
import com.example.museapp.data.util.NetworkResult
import com.example.museapp.data.util.safeApiCall
import com.example.museapp.domain.repository.AuthRepository
import com.example.museapp.domain.repository.InterestsRepository
import com.example.museapp.util.CommonRequestBuilder
import com.example.museapp.util.PhoneUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class AuthRepositoryImpl @Inject constructor(
    private val api: ApiService,
    private val interestsRepo: InterestsRepository,
    private val userStore: com.example.museapp.data.store.UserStore,
    private val tokenStore: com.example.museapp.data.store.TokenStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AuthRepository {

    override suspend fun requestOtp(countryCode: String, phone: String): NetworkResult<SendOtpData> =
        safeApiCall(ioDispatcher) {
            val e164 = PhoneUtils.formatE164(countryCode, phone)
            val payload = OtpPayload(phone = e164)
            val req = CommonRequestBuilder.buildWithLiveLocation(payload)
            val resp: ApiResponse<SendOtpData> = api.sendOtp(req)
            if (resp.isSuccessful()) {
                // Background fetch interests (fire-and-forget)
                try {
                    CoroutineScope(ioDispatcher).launch {
                        try {
                            interestsRepo.fetchAndSaveInterests(page = 1, limit = 10)
                        } catch (ignored: Exception) {
                        }
                    }
                } catch (ignored: Exception) {
                }

                resp.data ?: throw Exception("Empty send-otp data")
            } else {
                throw Exception(resp.message ?: "Failed to request OTP")
            }
        }

    override suspend fun resendOtp(countryCode: String, phone: String): NetworkResult<SendOtpData> =
        safeApiCall(ioDispatcher) {
            val e164 = PhoneUtils.formatE164(countryCode, phone)
            val payload = OtpPayload(phone = e164)
            val req = CommonRequestBuilder.buildWithLiveLocation(payload)
            val resp: ApiResponse<SendOtpData> = api.resendOtp(req)
            if (resp.isSuccessful()) {
                resp.data ?: throw Exception("Empty resend-otp data")
            } else {
                throw Exception(resp.message ?: "Failed to resend OTP")
            }
        }

    override suspend fun verifyOtp(countryCode: String, phone: String, otp: String): NetworkResult<VerifyOtpData> =
        safeApiCall(ioDispatcher) {
            val e164 = PhoneUtils.formatE164(countryCode, phone)
            val payload = VerifyOtpPayload(phone = e164, otp = otp)
            val req = CommonRequestBuilder.buildWithLiveLocation(payload)
            val resp: ApiResponse<VerifyOtpData> = api.verifyOtp(req)
            if (resp.isSuccessful()) {
                val data = resp.data

                // persist user to local store if available (non-blocking, but ensure safe)
                try {
                    data?.user?.let { user ->
                        // save on IO dispatcher
                        withContext(ioDispatcher) {
                            try {
                                userStore.saveUser(user)
                            } catch (ignored: Exception) { }
                        }
                    }
                } catch (ignored: Exception) { }

                // --- NEW: persist access token into TokenStore (safe, on IO) ---
                try {
                    val newToken = data?.access_token?: ""
                    if (!newToken.isNullOrBlank()) {
                        withContext(ioDispatcher) {
                            try {
                                tokenStore.setToken(newToken)
                            } catch (ignored: Exception) { }
                        }
                    }
                } catch (ignored: Exception) { }
                // -----------------------------------------------------------------

                data ?: VerifyOtpData(
                    user = null,
                    access_token = null,
                    refresh_token = null,
                    expires_in = null,
                    token_type = null
                )
            } else {
                throw Exception(resp.message ?: "Failed to verify OTP")
            }
        }
}
