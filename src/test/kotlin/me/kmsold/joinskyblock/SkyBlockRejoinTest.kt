package me.kmsold.joinskyblock

import me.kmsold.joinskyblock.connect.SkyBlockRejoin
import me.kmsold.joinskyblock.connect.SkyBlockRejoin.Event
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SkyBlockRejoinTest {

    private var sent = 0
    private val rejoin = SkyBlockRejoin { sent++ }
    private val delay = 70_000L

    private fun skyBlock(now: Long = 0) = rejoin.onLocation(skyBlock = true, lobby = false, enabled = true, delayMillis = delay, now = now)
    private fun lobby(now: Long = 0, enabled: Boolean = true) =
        rejoin.onLocation(skyBlock = false, lobby = true, enabled = enabled, delayMillis = delay, now = now)

    @Test
    fun `kicked from skyblock into a lobby goes back after the delay`() {
        skyBlock()
        assertEquals(Event.Scheduled(delay), lobby(now = 1_000))
        assertNull(rejoin.tick(enabled = true, now = 1_000 + delay - 1))
        assertEquals(0, sent)
        assertEquals(Event.Sent, rejoin.tick(enabled = true, now = 1_000 + delay))
        assertEquals(1, sent)
        // One attempt per kick.
        rejoin.tick(enabled = true, now = 10 * delay)
        assertEquals(1, sent)
    }

    @Test
    fun `leaving with a command is not a kick`() {
        for (command in listOf("lobby", "l", "play bedwars", "/lobby", "LIMBO")) {
            val rejoin = SkyBlockRejoin { sent++ }
            rejoin.onLocation(skyBlock = true, lobby = false, enabled = true, delayMillis = delay, now = 0)
            rejoin.onPlayerCommand(command)
            assertNull(rejoin.onLocation(skyBlock = false, lobby = true, enabled = true, delayMillis = delay, now = 0), command)
        }
    }

    @Test
    fun `other commands do not count as leaving`() {
        skyBlock()
        rejoin.onPlayerCommand("warp garden")
        rejoin.onPlayerCommand("pv")
        assertEquals(Event.Scheduled(delay), lobby())
    }

    @Test
    fun `a lobby without skyblock before it is ignored`() {
        assertNull(lobby())
        rejoin.tick(enabled = true, now = 10 * delay)
        assertEquals(0, sent)
    }

    @Test
    fun `nothing happens when the option is off`() {
        skyBlock()
        assertNull(lobby(enabled = false))
    }

    @Test
    fun `switching the option off during the countdown cancels it`() {
        skyBlock()
        lobby()
        assertEquals(Event.Cancelled, rejoin.tick(enabled = false, now = 1_000))
        rejoin.tick(enabled = true, now = 10 * delay)
        assertEquals(0, sent)
    }

    @Test
    fun `getting back to skyblock by hand cancels it`() {
        skyBlock()
        lobby()
        assertEquals(Event.Cancelled, skyBlock(now = 5_000))
        rejoin.tick(enabled = true, now = 10 * delay)
        assertEquals(0, sent)
    }

    @Test
    fun `a party warp into a game cancels it`() {
        skyBlock()
        lobby()
        assertEquals(Event.Cancelled, rejoin.onLocation(skyBlock = false, lobby = false, enabled = true, delayMillis = delay, now = 5_000))
        rejoin.tick(enabled = true, now = 10 * delay)
        assertEquals(0, sent)
    }

    @Test
    fun `cancel and disconnect stop it`() {
        skyBlock()
        lobby()
        assertEquals(Event.Cancelled, rejoin.cancel())
        assertNull(rejoin.cancel())
        skyBlock()
        lobby()
        rejoin.reset()
        assertFalse(rejoin.isPending)
        rejoin.tick(enabled = true, now = 10 * delay)
        assertEquals(0, sent)
    }
}
