package com.example.museapp.data.auth.dto

/**
 * Response data for OTP send request.
 */
data class SendOtpData(
    val otp_sent: Boolean,
    val channel: String?,
    val ttl_seconds: Int?,
)