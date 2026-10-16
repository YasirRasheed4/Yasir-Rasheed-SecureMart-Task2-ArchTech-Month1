package com.securemart.security

object SecurityConfig {
    /**
     * When true the app window is marked FLAG_SECURE: screenshots, screen recording and the
     * recent-apps thumbnail are blocked. Turn this on for a production release.
     * It is false here so you can take screenshots for your report.
     */
    const val BLOCK_SCREENSHOTS = false

    const val MAX_LOGIN_ATTEMPTS = 5
    const val LOCK_MINUTES = 5L
}
