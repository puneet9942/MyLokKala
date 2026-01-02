package com.example.museapp.domain.usecase

import com.example.museapp.domain.repository.AuthRepository
import javax.inject.Inject

class VerifyOtpUseCase @Inject constructor(   
    private val repo: AuthRepository
) {
    suspend operator fun invoke(cc: String, phone: String, otp: String) =
        repo.verifyOtp(cc, phone, otp)
}