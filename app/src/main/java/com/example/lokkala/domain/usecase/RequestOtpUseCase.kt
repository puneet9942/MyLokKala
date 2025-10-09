package com.example.lokkala.domain.usecase

import com.example.lokkala.data.util.NetworkResult
import com.example.lokkala.domain.repository.AuthRepository
import com.example.lokkala.util.ValidationUtils
import javax.inject.Inject

class RequestOtpUseCase @Inject constructor(
    private val repo: AuthRepository
) {
    suspend operator fun invoke(cc: String, phone: String): NetworkResult<String> {
        val countryIso = ValidationUtils.getCountryIsoFromCode(cc)
        val fullPhone = cc + phone

        // Strict validation before repo call (now works for any country)
        if (countryIso == null || !ValidationUtils.isValidPhoneForCountry(fullPhone, countryIso)) {
            return NetworkResult.Error(message = "Invalid phone number for your country")
        }
        return repo.requestOtp(cc, phone)
    }
}
