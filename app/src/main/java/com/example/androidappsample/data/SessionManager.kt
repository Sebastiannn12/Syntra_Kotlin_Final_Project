package com.example.androidappsample.data

import android.content.Context
import androidx.core.content.edit

class SessionManager(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val token: String? get() = transientToken ?: preferences.getString(KEY_TOKEN, null)
    val userId: Int get() = preferences.getInt(KEY_USER_ID, -1)
    val displayName: String get() = preferences.getString(KEY_DISPLAY_NAME, "Explorer").orEmpty()
    val username: String get() = preferences.getString(KEY_USERNAME, "").orEmpty()
    val isSignedIn: Boolean get() = !token.isNullOrBlank()

    fun save(token: String, user: User, remember: Boolean = true) {
        transientToken = if (remember) null else token
        preferences.edit {
            if (remember) putString(KEY_TOKEN, token) else remove(KEY_TOKEN)
            putInt(KEY_USER_ID, user.id)
            putString(KEY_DISPLAY_NAME, user.displayName)
            putString(KEY_USERNAME, user.username)
        }
    }

    fun updateUser(user: User) {
        preferences.edit {
            putInt(KEY_USER_ID, user.id)
            putString(KEY_DISPLAY_NAME, user.displayName)
            putString(KEY_USERNAME, user.username)
        }
    }

    fun clear() {
        transientToken = null
        preferences.edit { clear() }
    }

    companion object {
        @Volatile private var transientToken: String? = null
        private const val PREFS_NAME = "secure_session"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_USERNAME = "username"
    }
}
