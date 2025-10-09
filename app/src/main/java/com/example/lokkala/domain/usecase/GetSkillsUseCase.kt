package com.example.lokkala.domain.usecase

import com.example.lokkala.data.util.NetworkResult
import com.example.lokkala.domain.repository.HomeRepository
import javax.inject.Inject

class GetSkillsUseCase @Inject constructor(
    private val repo: HomeRepository
) {
    suspend operator fun invoke(): NetworkResult<List<String>> {
        return repo.getSkills()
    }
}