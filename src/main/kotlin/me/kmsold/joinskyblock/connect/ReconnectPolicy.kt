package me.kmsold.joinskyblock.connect

/**
 * Decides whether a lost connection gets an automatic reconnect. Kept free of Minecraft classes
 * so it can be tested.
 *
 * Only Hypixel sessions count, and only when they end on the "connection lost" screen - leaving
 * through the pause menu never shows it. Bans and a login from another client are left alone,
 * and a server that keeps refusing us gets [MAX_ATTEMPTS] tries in a row, not an endless loop.
 */
class ReconnectPolicy {

    private var onHypixel = false
    private var ownAttempt = false
    private var attempts = 0

    /** A play session started; a successful join to Hypixel also clears the failed attempts. */
    fun onJoin(isHypixel: Boolean) {
        onHypixel = isHypixel
        ownAttempt = false
        if (isHypixel) attempts = 0
    }

    /** Our reconnect started connecting; if that fails, the failure screen still counts as ours. */
    fun onReconnectStarted() {
        attempts++
        ownAttempt = true
    }

    /**
     * Called once per "connection lost" screen. Consumes the session either way, so the screen
     * of some later, unrelated server never inherits it.
     */
    fun shouldReconnect(enabled: Boolean, reason: String): Boolean {
        val eligible = onHypixel || ownAttempt
        onHypixel = false
        ownAttempt = false
        if (!enabled || !eligible) return false
        if (attempts >= MAX_ATTEMPTS) return false
        val lower = reason.lowercase()
        return BLOCKING_REASONS.none { it in lower }
    }

    companion object {
        const val MAX_ATTEMPTS = 3

        /** Kicks that a reconnect must not fight: a ban, or the account being used elsewhere. */
        val BLOCKING_REASONS = listOf("banned", "logged in from another location")
    }
}
