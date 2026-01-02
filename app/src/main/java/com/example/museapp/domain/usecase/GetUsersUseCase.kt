package com.example.museapp.domain.usecase

import com.example.museapp.data.util.NetworkResult
import com.example.museapp.domain.model.User
import com.example.museapp.domain.repository.HomeRepository
import javax.inject.Inject

class GetUsersUseCase @Inject constructor(
    private val repo: HomeRepository
) {
    suspend operator fun invoke(page: Int = 1, limit: Int = 10): NetworkResult<List<User>> {
        return repo.getUsers(page, limit)
    }
}
