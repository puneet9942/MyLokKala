package com.example.museapp.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object SharedPrefUtils {
    private const val PREFS_NAME = "muse_app_prefs"

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun putString(context: Context, key: String, value: String?) {
        getPrefs(context).edit { putString(key, value) }
    }

    fun getString(context: Context, key: String, defValue: String? = null): String? {
        return getPrefs(context).getString(key, defValue)
    }

    fun putInt(context: Context, key: String, value: Int) {
        getPrefs(context).edit { putInt(key, value) }
    }

    fun getInt(context: Context, key: String, defValue: Int = 0): Int {
        return getPrefs(context).getInt(key, defValue)
    }

    fun putBoolean(context: Context, key: String, value: Boolean) {
        getPrefs(context).edit { putBoolean(key, value) }
    }

    fun getBoolean(context: Context, key: String, defValue: Boolean = false): Boolean {
        return getPrefs(context).getBoolean(key, defValue)
    }

    fun remove(context: Context, key: String) {
        getPrefs(context).edit { remove(key) }
    }

    fun clearAll(context: Context) {
        getPrefs(context).edit { clear() }
    }
}
