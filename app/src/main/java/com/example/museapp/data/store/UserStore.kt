package com.example.museapp.data.store

import android.content.Context
import com.example.museapp.data.auth.dto.UserDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simple small store to persist current user (from verify-otp response).
 * Uses SharedPreferences with Moshi JSON for persistence (keeps it lightweight).
 *
 * If you prefer DataStore, we can change to DataStore later (but this is minimal and safe).
 */
@Singleton
class UserStore @Inject constructor(
    private val context: Context
) {

    private val prefsName = "museapp_user_prefs"
    private val keyUserJson = "key_user_json"

    private val prefs by lazy { context.getSharedPreferences(prefsName, Context.MODE_PRIVATE) }

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val userAdapter = moshi.adapter(UserDto::class.java)

    suspend fun saveUser(user: UserDto) {
        try {
            val json = userAdapter.toJson(user)
            prefs.edit().putString(keyUserJson, json).apply()
        } catch (ex: Exception) {
            // swallow - persistence shouldn't crash app
        }
    }

    fun getUserFlow(): Flow<UserDto?> = flow {
        try {
            val json = prefs.getString(keyUserJson, null)
            if (json.isNullOrEmpty()) {
                emit(null)
            } else {
                val user = try { userAdapter.fromJson(json) } catch (e: Exception) { null }
                emit(user)
            }
        } catch (ex: Exception) {
            emit(null)
        }
    }.flowOn(Dispatchers.IO)

    suspend fun clearUser() {
        prefs.edit().remove(keyUserJson).apply()
    }
}
