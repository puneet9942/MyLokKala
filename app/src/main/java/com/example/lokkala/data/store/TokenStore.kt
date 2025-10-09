package com.example.lokkala.data.store

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("app_prefs")

class TokenStore(private val context: Context) {
    private val KEY = stringPreferencesKey("auth_token")

    val tokenFlow = context.dataStore.data.map { it[KEY] }

    suspend fun save(token: String) {
        context.dataStore.edit { it[KEY] = token }
    }

    suspend fun clear() {
        context.dataStore.edit { it.remove(KEY) }
    }
}