package com.example.museapp.domain.usecase

import com.example.museapp.data.util.NetworkResult
import com.example.museapp.domain.repository.AuthRepository
import com.example.museapp.util.ValidationUtils
import javax.inject.Inject

class RequestOtpUseCase @Inject constructor(
    private val repo: AuthRepository
) {
    suspend operator fun invoke(cc: String, phone: String): NetworkResult<String> {
        val countryIso = ValidationUtils.getCountryIsoFromCode(cc)
        val fullPhone = cc + phone

        if (countryIso == null || !ValidationUtils.isValidPhoneForCountry(fullPhone, countryIso)) {
            return NetworkResult.Error(message = "Invalid phone number for your country")
        }

        val repoResult = repo.requestOtp(cc, phone)

        return when (repoResult) {
            is NetworkResult.Success -> {
                val sidOrMsg = "OTP sent"
                NetworkResult.Success(sidOrMsg)
            }
            is NetworkResult.Error -> NetworkResult.Error(code = repoResult.code, message = repoResult.message)
        }
    }
}
