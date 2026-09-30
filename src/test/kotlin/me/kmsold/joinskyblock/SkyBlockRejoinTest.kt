package me.kmsold.joinskyblock

import me.kmsold.joinskyblock.connect.SkyBlockRejoin
import me.kmsold.joinskyblock.connect.SkyBlockRejoin.Event
import me.kmsold.joinskyblock.connect.SkyBlockRejoin.Place
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SkyBlockRejoinTest {

    private val sent = mutableListOf<String>()
    private val rejoin = SkyBlockRejoin { sent += it }
    private val delay = 70_000L

    private fun at(place: Place, now: Long = 0, enabled: Boolean = true) =
        rejoin.onLocation(place, enabled, delay, now)

    private fun kickMessage(limbo: Boolean = false, now: Long = 0, enabled: Boolean = true) =
        rejoin.onKickMessage(limbo, enabled, delay, now)

    @Test
    fun `kicked from skyblock into a lobby goes back after the delay`() {
        at(Place.SKYBLOCK)
        assertEquals(Event.Scheduled(delay), at(Place.LOBBY, now = 1_000))
        assertNull(rejoin.tick(enabled = true, now = 1_000 + delay - 1))
        assertEquals(Event.Sent("play skyblock"), rejoin.tick(enabled = true, now = 1_000 + delay))
        // One attempt per kick.
        rejoin.tick(enabled = true, now = 10 * delay)
        assertEquals(listOf("play skyblock"), sent)
    }

    @Test
    fun `the chat line alone is enough, and limbo is left through the lobby`() {
        at(Place.SKYBLOCK)
        // Limbo sends no location packet, only the chat line.
        assertEquals(Event.Scheduled(delay), kickMessage(limbo = true))
        assertEquals(Event.Sent("lobby"), rejoin.tick(enabled = true, now = delay))
        assertEquals(Event.Sent("play skyblock"), at(Place.LOBBY, now = delay + 2_000))
        assertEquals(listOf("lobby", "play skyblock"), sent)
    }

    @Test
    fun `from limbo it tries anyway when the lobby never reports`() {
        at(Place.SKYBLOCK)
        kickMessage(limbo = true)
        rejoin.tick(enabled = true, now = delay)
        assertNull(rejoin.tick(enabled = true, now = delay + SkyBlockRejoin.LIMBO_EXIT_TIMEOUT - 1))
        assertEquals(Event.Sent("play skyblock"), rejoin.tick(enabled = true, now = delay + SkyBlockRejoin.LIMBO_EXIT_TIMEOUT))
    }

    @Test
    fun `the chat line and the packet together schedule it once`() {
        at(Place.SKYBLOCK)
        assertEquals(Event.Scheduled(delay), at(Place.LOBBY, now = 1_000))
        assertNull(kickMessage(now = 1_500))
        rejoin.tick(enabled = true, now = 10 * delay)
        assertEquals(listOf("play skyblock"), sent)
    }

    @Test
    fun `the chat line overrules a leave command typed just before`() {
        at(Place.SKYBLOCK)
        rejoin.onPlayerCommand("lobby")
        assertNull(at(Place.LOBBY, now = 1_000))
        assertEquals(Event.Scheduled(delay), kickMessage(now = 2_000))
    }

    @Test
    fun `leaving with a command is not a kick`() {
        for (command in listOf("lobby", "l", "play bedwars", "/lobby", "LIMBO")) {
            val rejoin = SkyBlockRejoin { sent += it }
            rejoin.onLocation(Place.SKYBLOCK, true, delay, 0)
            rejoin.onPlayerCommand(command)
            assertNull(rejoin.onLocation(Place.LOBBY, true, delay, 0), command)
        }
    }

    @Test
    fun `other commands do not count as leaving`() {
        at(Place.SKYBLOCK)
        rejoin.onPlayerCommand("warp garden")
        rejoin.onPlayerCommand("pv")
        assertEquals(Event.Scheduled(delay), at(Place.LOBBY))
    }

    @Test
    fun `nothing happens outside skyblock`() {
        assertNull(at(Place.LOBBY))
        assertNull(kickMessage())
        // A kick line long after leaving SkyBlock is about something else.
        at(Place.SKYBLOCK)
        rejoin.onPlayerCommand("play bedwars")
        at(Place.OTHER, now = 1_000)
        assertNull(kickMessage(now = 1_000 + SkyBlockRejoin.KICK_MESSAGE_WINDOW + 1))
        assertEquals(emptyList<String>(), sent)
    }

    @Test
    fun `nothing happens when the option is off`() {
        at(Place.SKYBLOCK)
        assertNull(at(Place.LOBBY, enabled = false))
        assertNull(kickMessage(enabled = false))
    }

    @Test
    fun `switching the option off during the countdown cancels it`() {
        at(Place.SKYBLOCK)
        at(Place.LOBBY)
        assertEquals(Event.Cancelled, rejoin.tick(enabled = false, now = 1_000))
        rejoin.tick(enabled = true, now = 10 * delay)
        assertEquals(emptyList<String>(), sent)
    }

    @Test
    fun `getting back to skyblock or into a game cancels it`() {
        at(Place.SKYBLOCK)
        at(Place.LOBBY)
        assertEquals(Event.Cancelled, at(Place.SKYBLOCK, now = 5_000))
        at(Place.LOBBY, now = 6_000)
        assertEquals(Event.Cancelled, at(Place.OTHER, now = 7_000))
        rejoin.tick(enabled = true, now = 10 * delay)
        assertEquals(emptyList<String>(), sent)
    }

    @Test
    fun `cancel and disconnect stop it`() {
        at(Place.SKYBLOCK)
        at(Place.LOBBY)
        assertEquals(Event.Cancelled, rejoin.cancel())
        assertNull(rejoin.cancel())
        at(Place.SKYBLOCK)
        at(Place.LOBBY)
        rejoin.reset()
        assertFalse(rejoin.isPending)
        rejoin.tick(enabled = true, now = 10 * delay)
        assertEquals(emptyList<String>(), sent)
    }

    @Test
    fun `kick lines are recognised by how they start`() {
        assertEquals(false, SkyBlockRejoin.parseKickMessage("An exception occurred in your connection, so you were put in the SkyBlock Lobby!"))
        assertEquals(true, SkyBlockRejoin.parseKickMessage("A kick occurred in your connection, so you have been routed to limbo!"))
        assertEquals(false, SkyBlockRejoin.parseKickMessage("You were kicked while joining that server!"))
        // Someone quoting it in chat comes with a name in front.
        assertNull(SkyBlockRejoin.parseKickMessage("[MVP+] Someone: An exception occurred in your connection"))
        assertNull(SkyBlockRejoin.parseKickMessage("Evacuating to Hub..."))
    }
}
