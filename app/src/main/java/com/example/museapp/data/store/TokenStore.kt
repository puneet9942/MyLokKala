package com.example.museapp.data.store

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS = "museapp_auth_prefs"
        const val KEY_ACCESS_TOKEN = "access_token"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun setToken(token: String?) {
        prefs.edit().apply {
            if (token == null) remove(KEY_ACCESS_TOKEN) else putString(KEY_ACCESS_TOKEN, token)
        }.apply()
    }
}
