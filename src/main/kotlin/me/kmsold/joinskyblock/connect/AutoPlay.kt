package me.kmsold.joinskyblock.connect

/**
 * Decides when `/play skyblock` goes out. Kept free of Minecraft classes so it can be tested.
 *
 * The flow is: the button [request]s it, the next [onJoin] arms it, and it fires exactly once -
 * on the first Hypixel location packet, or after a delay when that packet never comes. Joining
 * Hypixel any other way never arms it, and a disconnect or a failed connection clears everything.
 */
class AutoPlay(
    val hasLocationApi: Boolean,
    private val send: () -> Unit,
) {

    private var requested = false
    private var armed = false
    private var ticksSinceJoin = 0

    val isPending: Boolean get() = requested || armed

    /** Called by the button right before it starts connecting. */
    fun request() {
        requested = true
        armed = false
    }

    fun onJoin() {
        if (!requested) return
        requested = false
        armed = true
        ticksSinceJoin = 0
    }

    /**
     * The first location packet is Hypixel saying the player has landed in a lobby, which is the
     * earliest point where commands are reliably accepted.
     */
    fun onLocation(alreadyInSkyBlock: Boolean) {
        if (!armed) return
        armed = false
        if (!alreadyInSkyBlock) send()
    }

    fun tick() {
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
    }

    companion object {
        /** Without hypixel-mod-api: two seconds after joining, the lobby has always loaded by then. */
        const val FALLBACK_DELAY_TICKS = 40

        /** With hypixel-mod-api the packet normally comes within a second; this only covers it going missing. */
        const val LOCATION_TIMEOUT_TICKS = 200
    }
}
