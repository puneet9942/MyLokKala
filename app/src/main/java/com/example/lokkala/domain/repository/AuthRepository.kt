package com.example.lokkala.domain.repository

import com.example.lokkala.data.util.NetworkResult

interface AuthRepository {
    suspend fun requestOtp(countryCode: String, phone: String): NetworkResult<String>
    suspend fun verifyOtp(countryCode: String, phone: String, otp: String): NetworkResult<String> // token
}