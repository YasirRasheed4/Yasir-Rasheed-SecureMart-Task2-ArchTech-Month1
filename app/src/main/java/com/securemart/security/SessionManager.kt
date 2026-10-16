package com.securemart.security

import android.content.Context

/**
 * Keeps the signed-in user id in app-private storage and signs the user out
 * automatically after 30 minutes away from the app.
 */
class SessionManager(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("session", Context.MODE_PRIVATE)

    fun start(userId: Long) {
        prefs.edit()
            .putLong(KEY_USER, userId)
            .putLong(KEY_ACTIVE, System.currentTimeMillis())
            .apply()
    }

    /** Remember the moment the user left the app. */
    fun touch() {
        if (prefs.contains(KEY_USER)) {
            prefs.edit().putLong(KEY_ACTIVE, System.currentTimeMillis()).apply()
        }
    }

    /** Returns the signed-in user id, or null if nobody is signed in or the session timed out. */
    fun userId(): Long? {
        val id = prefs.getLong(KEY_USER, -1L)
        if (id < 0) return null
        val last = prefs.getLong(KEY_ACTIVE, 0L)
        if (System.currentTimeMillis() - last > TIMEOUT_MS) {
            clear()
            return null
        }
        return id
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        const val TIMEOUT_MS = 30 * 60 * 1000L
        private const val KEY_USER = "user_id"
        private const val KEY_ACTIVE = "last_active"
    }
}
