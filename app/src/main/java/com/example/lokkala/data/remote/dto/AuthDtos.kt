package com.example.lokkala.data.remote.dto

data class RequestOtpBody(val countryCode: String, val phone: String)
data class VerifyOtpBody(val countryCode: String, val phone: String, val otp: String)
data class VerifyOtpResponse(val token: String)