package dev.minimalphone.session

/**
 * Tracks foreground micro-sessions (5-minute countdowns) and 60-second cooldowns
 * to eliminate doom-scrolling and unconscious looping.
 */
object MicroSessionManager {

    const val SESSION_DURATION_MS = 5 * 60 * 1000L // 5 minutes
    const val COOLDOWN_DURATION_MS = 60 * 1000L     // 60 seconds

    data class SessionState(
        val packageName: String,
        val startedAtMillis: Long,
        val expiresAtMillis: Long,
        val cooldownEndsAtMillis: Long = 0L,
    )

    private val sessions = mutableMapOf<String, SessionState>()

    /**
     * Starts or resumes a 5-minute micro-session for the given package.
     * Returns the active session state, or null if currently locked in cooldown.
     */
    @Synchronized
    fun startOrGetSession(packageName: String, now: Long = System.currentTimeMillis()): SessionState? {
        val current = sessions[packageName]
        if (current != null && current.cooldownEndsAtMillis > now) {
            // Still in cooldown
            return null
        }
        if (current != null && now < current.expiresAtMillis) {
            // Continuing existing valid session
            return current
        }

        // Start new 5-minute session
        val state = SessionState(
            packageName = packageName,
            startedAtMillis = now,
            expiresAtMillis = now + SESSION_DURATION_MS,
            cooldownEndsAtMillis = 0L,
        )
        sessions[packageName] = state
        return state
    }

    @Synchronized
    fun getSession(packageName: String): SessionState? = sessions[packageName]

    /**
     * Checks if a package is currently subject to a 60-second cooldown.
     */
    @Synchronized
    fun isInCooldown(packageName: String, now: Long = System.currentTimeMillis()): Boolean {
        val current = sessions[packageName] ?: return false
        return current.cooldownEndsAtMillis > now
    }

    /**
     * Returns remaining cooldown in whole seconds (ceiling).
     */
    @Synchronized
    fun getRemainingCooldownSeconds(packageName: String, now: Long = System.currentTimeMillis()): Int {
        val current = sessions[packageName] ?: return 0
        val remainingMs = current.cooldownEndsAtMillis - now
        return if (remainingMs > 0) ((remainingMs + 999L) / 1000L).toInt() else 0
    }

    /**
     * Returns remaining session time in milliseconds.
     */
    @Synchronized
    fun getRemainingSessionMs(packageName: String, now: Long = System.currentTimeMillis()): Long {
        val current = sessions[packageName] ?: return 0L
        return (current.expiresAtMillis - now).coerceAtLeast(0L)
    }

    /**
     * Triggers expiration of the current session and begins the 60-second cooldown.
     */
    @Synchronized
    fun expireSession(packageName: String, now: Long = System.currentTimeMillis()): Long {
        val cooldownEnd = now + COOLDOWN_DURATION_MS
        val existing = sessions[packageName]
        sessions[packageName] = SessionState(
            packageName = packageName,
            startedAtMillis = existing?.startedAtMillis ?: (now - SESSION_DURATION_MS),
            expiresAtMillis = now,
            cooldownEndsAtMillis = cooldownEnd,
        )
        return cooldownEnd
    }

    @Synchronized
    fun clear(packageName: String) {
        sessions.remove(packageName)
    }

    @Synchronized
    fun clearAll() {
        sessions.clear()
    }
}
