package me.kmsold.joinskyblock.connect

/**
 * Decides when `/play skyblock` goes out. Kept free of Minecraft classes so it can be tested.
 *
 * The flow is: the button [request]s it, the next [onJoin] arms it, the first Hypixel location
 * packet starts a short countdown, and it fires exactly once when that runs out - or after a
 * delay when the packet never comes. Joining Hypixel any other way never arms it, and a
 * disconnect or a failed connection clears everything.
 */
class AutoPlay(
    val hasLocationApi: Boolean,
    private val send: () -> Unit,
) {

    private var requested = false
    private var armed = false
    private var ticksSinceJoin = 0

    /** Ticks left before the command once the lobby is reached; null when nothing is counting down. */
    private var ticksToSend: Int? = null

    val isPending: Boolean get() = requested || armed || ticksToSend != null

    /** Called by the button right before it starts connecting. */
    fun request() {
        requested = true
        armed = false
        ticksToSend = null
    }

    fun onJoin() {
        if (!requested) return
        requested = false
        armed = true
        ticksSinceJoin = 0
    }

    /**
     * The first location packet is Hypixel saying the player has landed in a lobby. The command
     * still waits [LOBBY_DELAY_TICKS]: moving to SkyBlock within the first couple of seconds in
     * the lobby sometimes drops the connection with a packet decoding error.
     */
    fun onLocation(alreadyInSkyBlock: Boolean) {
        if (alreadyInSkyBlock) {
            // Hypixel (or the player) got there first, nothing left to do.
            armed = false
            ticksToSend = null
            return
        }
        if (!armed) return
        armed = false
        ticksToSend = LOBBY_DELAY_TICKS
    }

    fun tick() {
        ticksToSend?.let { left ->
            if (left > 1) {
                ticksToSend = left - 1
            } else {
                ticksToSend = null
                send()
            }
            return
        }
        if (!armed) return
        ticksSinceJoin++
        val limit = if (hasLocationApi) LOCATION_TIMEOUT_TICKS else FALLBACK_DELAY_TICKS
        if (ticksSinceJoin >= limit) {
            armed = false
            send()
        }
    }

    fun reset() {
        requested = false
        armed = false
        ticksToSend = null
    }

    companion object {
        /**
         * Five seconds in the lobby before the command. In the author's logs, moves sent within
         * 0-2 s of the lobby dropped the connection 17 times out of 110, those sent 3 s or later
         * never (0 of 152). Counted in client ticks, so it stretches while the game is frozen.
         */
        const val LOBBY_DELAY_TICKS = 100

        /** Without hypixel-mod-api: the lobby loads within a second of joining, plus the same five seconds. */
        const val FALLBACK_DELAY_TICKS = 120

        /** With hypixel-mod-api the packet normally comes within a second; this only covers it going missing. */
        const val LOCATION_TIMEOUT_TICKS = 200
    }
}
