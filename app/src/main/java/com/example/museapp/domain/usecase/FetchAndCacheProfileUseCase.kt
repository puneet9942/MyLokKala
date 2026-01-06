package com.example.museapp.domain.usecase

import com.example.museapp.domain.repository.UserRepository
import javax.inject.Inject

class FetchAndCacheProfileUseCase @Inject constructor(
    private val repository: UserRepository
) {
    suspend operator fun invoke() = repository.fetchProfileFromNetworkAndCache()
}
