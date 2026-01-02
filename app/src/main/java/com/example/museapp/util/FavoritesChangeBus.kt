package com.example.museapp.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Lightweight bus that emits id-only favorite change events.
 * Using id-only avoids needing to construct domain User objects.
 */
data class FavoriteChange(val favoriteId: String, val isAdded: Boolean)

object FavoritesChangeBus {
    // replay = 1 so a late subscriber can get the most recent change in edge cases
    private val _changes = MutableSharedFlow<FavoriteChange>(replay = 1)
    val changes = _changes.asSharedFlow()

    fun tryEmit(change: FavoriteChange): Boolean = _changes.tryEmit(change)
}
