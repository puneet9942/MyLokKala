package com.example.lokkala.data.repository

import com.example.lokkala.data.remote.ApiService
import com.example.lokkala.data.remote.dto.RequestOtpBody
import com.example.lokkala.data.remote.dto.VerifyOtpBody
import com.example.lokkala.domain.repository.AuthRepository
import com.example.lokkala.data.util.NetworkResult
import com.example.lokkala.data.util.safeApiCall
import kotlinx.coroutines.CoroutineDispatcher

class AuthRepositoryImpl(
    private val api: ApiService,
    private val io: CoroutineDispatcher
) : AuthRepository {

    override suspend fun requestOtp(countryCode: String, phone: String): NetworkResult<String> =
        safeApiCall(io) {
            val response = api.requestOtp(RequestOtpBody(countryCode, phone))
            if (response.success) {
                response.message // Return message for display (or just return Unit)
            } else {
                throw Exception(response.message)
            }
        }

    override suspend fun verifyOtp(countryCode: String, phone: String, otp: String): NetworkResult<String> =
        safeApiCall(io) {
            val response = api.verifyOtp(VerifyOtpBody(countryCode, phone, otp))
            if (response.success && response.data != null) {
                response.data.token // Return token string
            } else {
                throw Exception(response.message)
            }
        }
}