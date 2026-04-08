package com.example.museapp.domain.usecase

import com.example.museapp.domain.repository.ProfileCacheRepository
import javax.inject.Inject

class FetchAndCacheProfileUseCase @Inject constructor(
    private val repository: ProfileCacheRepository
) {
    suspend operator fun invoke() = repository.fetchAndCacheProfile()
}
