package com.example.museapp.domain.usecase

import com.example.museapp.data.util.NetworkResult
import com.example.museapp.domain.repository.HomeRepository
import javax.inject.Inject

class GetSkillsUseCase @Inject constructor(
    private val repo: HomeRepository
) {
    suspend operator fun invoke(): NetworkResult<List<String>> {
        return repo.getSkills()
    }
}