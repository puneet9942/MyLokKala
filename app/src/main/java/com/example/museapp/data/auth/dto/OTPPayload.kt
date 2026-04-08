package com.example.museapp.data.auth.dto

/**
 * Payload for requesting an OTP.
 */
data class OtpPayload(
    val phone: String,
)

/**
 * Payload for verifying OTP.
 */
data class VerifyOtpPayload(
    val phone: String,
    val otp: String,
)