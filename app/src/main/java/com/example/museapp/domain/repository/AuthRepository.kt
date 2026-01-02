package com.example.museapp.domain.repository

import com.example.museapp.data.auth.dto.SendOtpData
import com.example.museapp.data.auth.dto.VerifyOtpData
import com.example.museapp.data.util.NetworkResult

interface AuthRepository {
    suspend fun requestOtp(countryCode: String, phone: String): NetworkResult<SendOtpData>
    suspend fun resendOtp(countryCode: String, phone: String): NetworkResult<SendOtpData>
    suspend fun verifyOtp(countryCode: String, phone: String, otp: String): NetworkResult<VerifyOtpData>
}