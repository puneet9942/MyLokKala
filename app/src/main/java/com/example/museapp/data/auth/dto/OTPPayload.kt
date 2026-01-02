package com.example.museapp.data.auth.dto

data class OtpPayload( // "phone" or "email"
    val phone: String,
)

data class VerifyOtpPayload(
    val phone: String,
    val otp: String,
)