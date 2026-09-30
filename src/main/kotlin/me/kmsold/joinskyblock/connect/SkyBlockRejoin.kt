package me.kmsold.joinskyblock.connect

/**
 * Notices being thrown out of SkyBlock and goes back with one `/play skyblock` after a delay.
 * Kept free of Minecraft classes so it can be tested; time comes in as millis.
 *
 * Two signals, either is enough: the location packet moving from SkyBlock to a lobby, or
 * Hypixel's own chat line about the kick. The chat line matters for limbo, which sends no
 * location packet, and it overrules a leave command the player happened to type. From limbo the
 * way back starts with `/lobby`. Landing anywhere else - a game, or back in SkyBlock - cancels
 * it, and each kick gets exactly one attempt.
 */
class SkyBlockRejoin(private val send: (String) -> Unit) {

    enum class Place { SKYBLOCK, LOBBY, LIMBO, OTHER }

    sealed interface Event {
        data class Scheduled(val delayMillis: Long) : Event
        data object Cancelled : Event
        data class Sent(val command: String) : Event
    }

    private var inSkyBlock = false
    private var place = Place.OTHER
    private var leftSkyBlockAt: Long? = null
    private var leavingOnPurpose = false
    private var deadline: Long? = null

    /** Set while `/lobby` has been sent from limbo and the lobby has not been reached yet. */
    private var leavingLimboUntil: Long? = null

    val isPending: Boolean get() = deadline != null || leavingLimboUntil != null

    fun onLocation(place: Place, enabled: Boolean, delayMillis: Long, now: Long): Event? {
        this.place = place
        if (place == Place.SKYBLOCK) {
            inSkyBlock = true
            leavingOnPurpose = false
            return cancel()
        }
        val left = inSkyBlock
        val onPurpose = leavingOnPurpose
        inSkyBlock = false
        leavingOnPurpose = false
        if (left) leftSkyBlockAt = now

        if (leavingLimboUntil != null && place == Place.LOBBY) {
            leavingLimboUntil = null
            return sent(PLAY)
        }
        if (place == Place.OTHER) return cancel()
        if (deadline != null) return null
        if (!left || onPurpose || !enabled) return null
        return schedule(delayMillis, now)
    }

    /** Hypixel's chat line about the kick; [limbo] when it says the player went to limbo. */
    fun onKickMessage(limbo: Boolean, enabled: Boolean, delayMillis: Long, now: Long): Event? {
        val fromSkyBlock = inSkyBlock || leftSkyBlockAt?.let { now - it <= KICK_MESSAGE_WINDOW } == true
        if (!enabled || !fromSkyBlock || isPending) return null
        if (limbo) place = Place.LIMBO
        inSkyBlock = false
        leavingOnPurpose = false
        return schedule(delayMillis, now)
    }

    /** A command the player typed themselves; ours never come through here. */
    fun onPlayerCommand(command: String) {
        val word = command.trimStart('/').substringBefore(' ').lowercase()
        if (inSkyBlock && word in LEAVE_COMMANDS) leavingOnPurpose = true
    }

    fun tick(enabled: Boolean, now: Long): Event? {
        leavingLimboUntil?.let { until ->
            // No lobby packet came, try from wherever we are.
            if (now < until) return null
            leavingLimboUntil = null
            return sent(PLAY)
        }
        val due = deadline ?: return null
        if (!enabled) return cancel()
        if (now < due) return null
        deadline = null
        if (place == Place.LIMBO) {
            leavingLimboUntil = now + LIMBO_EXIT_TIMEOUT
            return sent(LOBBY)
        }
        return sent(PLAY)
    }

    fun cancel(): Event? {
        if (!isPending) return null
        deadline = null
        leavingLimboUntil = null
        return Event.Cancelled
    }

    fun reset() {
        inSkyBlock = false
        place = Place.OTHER
        leftSkyBlockAt = null
        leavingOnPurpose = false
        deadline = null
        leavingLimboUntil = null
    }

    private fun schedule(delayMillis: Long, now: Long): Event {
        deadline = now + delayMillis
        return Event.Scheduled(delayMillis)
    }

    private fun sent(command: String): Event {
        send(command)
        return Event.Sent(command)
    }

    companion object {
        const val PLAY = "play skyblock"
        const val LOBBY = "lobby"

        /** How long after leaving SkyBlock a kick message still counts as being about SkyBlock. */
        const val KICK_MESSAGE_WINDOW = 15_000L

        /** How long to wait for the lobby after `/lobby` from limbo before trying anyway. */
        const val LIMBO_EXIT_TIMEOUT = 15_000L

        /** Hypixel commands that take the player out of SkyBlock on purpose. */
        val LEAVE_COMMANDS = setOf("lobby", "l", "leave", "limbo", "play", "server", "main", "rejoin")

        /**
         * How Hypixel's kick lines begin. Matched on the start of a system message, so a player
         * quoting one in chat - which comes with a rank and a name in front - does not count.
         */
        val KICK_MESSAGE_STARTS = listOf(
            "an exception occurred in your connection",
            "a kick occurred in your connection",
            "a disconnect occurred in your connection",
            "you were kicked while joining that server",
            "the server you were previously on went down",
        )

        /** @return null when [message] is not a kick line, otherwise whether it mentions limbo. */
        fun parseKickMessage(message: String): Boolean? {
            val text = message.trim().lowercase()
            if (KICK_MESSAGE_STARTS.none { text.startsWith(it) }) return null
            return "limbo" in text
        }
    }
}
