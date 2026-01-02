package com.example.museapp.presentation.feature.favorites

sealed class FavoritesEvent {
    object LoadFavorites : FavoritesEvent()
    data class AddFavorite(val userId: String) : FavoritesEvent()
    data class RemoveFavorite(val favoriteId: String) : FavoritesEvent()
}
