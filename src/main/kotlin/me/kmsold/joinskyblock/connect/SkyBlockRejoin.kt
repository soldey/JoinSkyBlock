package me.kmsold.joinskyblock.connect

/**
 * Notices being thrown out of SkyBlock into a Hypixel lobby and schedules one `/play skyblock`
 * after a delay. Kept free of Minecraft classes so it can be tested; time comes in as millis.
 *
 * It only fires when the player was in SkyBlock, lands in a lobby or limbo, and did not ask to
 * leave with a command like `/lobby` or `/play`. Landing anywhere else - a game, or back in
 * SkyBlock - cancels it, and each kick gets exactly one attempt.
 */
class SkyBlockRejoin(private val send: () -> Unit) {

    sealed interface Event {
        data class Scheduled(val delayMillis: Long) : Event
        data object Cancelled : Event
        data object Sent : Event
    }

    private var inSkyBlock = false
    private var leavingOnPurpose = false
    private var deadline: Long? = null

    val isPending: Boolean get() = deadline != null

    fun onLocation(skyBlock: Boolean, lobby: Boolean, enabled: Boolean, delayMillis: Long, now: Long): Event? {
        if (skyBlock) {
            inSkyBlock = true
            leavingOnPurpose = false
            return cancel()
        }
        val wasInSkyBlock = inSkyBlock
        val onPurpose = leavingOnPurpose
        inSkyBlock = false
        leavingOnPurpose = false
        if (!lobby) return cancel()
        if (!wasInSkyBlock || onPurpose || !enabled) return null
        deadline = now + delayMillis
        return Event.Scheduled(delayMillis)
    }

    /** A command the player typed themselves; ours never come through here. */
    fun onPlayerCommand(command: String) {
        val word = command.trimStart('/').substringBefore(' ').lowercase()
        if (inSkyBlock && word in LEAVE_COMMANDS) leavingOnPurpose = true
    }

    fun tick(enabled: Boolean, now: Long): Event? {
        val due = deadline ?: return null
        if (!enabled) return cancel()
        if (now < due) return null
        deadline = null
        send()
        return Event.Sent
    }

    fun cancel(): Event? {
        if (deadline == null) return null
        deadline = null
        return Event.Cancelled
    }

    fun reset() {
        inSkyBlock = false
        leavingOnPurpose = false
        deadline = null
    }

    companion object {
        /** Hypixel commands that take the player out of SkyBlock on purpose. */
        val LEAVE_COMMANDS = setOf("lobby", "l", "leave", "limbo", "play", "server", "main", "rejoin")
    }
}
