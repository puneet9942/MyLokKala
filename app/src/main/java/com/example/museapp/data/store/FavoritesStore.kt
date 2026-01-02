package com.example.museapp.data.store

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged

private val Context.dataStore by preferencesDataStore("app_prefs")

class FavoritesStore(private val context: Context) {
    companion object {
        private val KEY_FAVORITES = stringSetPreferencesKey("favorites_set")
    }

    val favoritesFlow = context.dataStore.data
        .map { prefs -> prefs[KEY_FAVORITES] ?: emptySet() }
        .distinctUntilChanged()

    suspend fun addFavorite(id: String) {
        context.dataStore.edit { prefs ->
            val set = prefs[KEY_FAVORITES]?.toMutableSet() ?: mutableSetOf()
            set.add(id)
            prefs[KEY_FAVORITES] = set
        }
    }

    suspend fun removeFavorite(id: String) {
        context.dataStore.edit { prefs ->
            val set = prefs[KEY_FAVORITES]?.toMutableSet() ?: mutableSetOf()
            set.remove(id)
            prefs[KEY_FAVORITES] = set
        }
    }

    suspend fun clearFavorites() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_FAVORITES)
        }
    }
}