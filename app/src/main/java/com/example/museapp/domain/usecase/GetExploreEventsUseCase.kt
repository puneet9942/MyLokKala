package com.example.museapp.domain.usecase

import com.example.museapp.domain.model.ExploreEvent
import com.example.museapp.domain.repository.ExploreEventRepository
import javax.inject.Inject

class GetExploreEventsUseCase @Inject constructor(
    private val repository: ExploreEventRepository
) {
    suspend operator fun invoke(query: String): Result<List<ExploreEvent>> {
        return repository.getExploreEvents(query)
    }
}