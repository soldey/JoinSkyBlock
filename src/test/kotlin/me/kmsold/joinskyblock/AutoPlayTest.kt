package me.kmsold.joinskyblock

import me.kmsold.joinskyblock.connect.AutoPlay
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class AutoPlayTest {

    private var sent = 0
    private fun autoPlay(hasLocationApi: Boolean) = AutoPlay(hasLocationApi) { sent++ }

    private fun AutoPlay.tick(times: Int) = repeat(times) { tick() }

    @Test
    fun `joining without pressing the button sends nothing`() {
        val autoPlay = autoPlay(hasLocationApi = true)
        autoPlay.onJoin()
        autoPlay.onLocation(alreadyInSkyBlock = false)
        autoPlay.tick(1000)
        assertEquals(0, sent)
    }

    @Test
    fun `sends once on the first location packet`() {
        val autoPlay = autoPlay(hasLocationApi = true)
        autoPlay.request()
        autoPlay.onJoin()
        autoPlay.onLocation(alreadyInSkyBlock = false)
        autoPlay.onLocation(alreadyInSkyBlock = false)
        autoPlay.tick(1000)
        assertEquals(1, sent)
        assertFalse(autoPlay.isPending)
    }

    @Test
    fun `does not send when hypixel already put the player in skyblock`() {
        val autoPlay = autoPlay(hasLocationApi = true)
        autoPlay.request()
        autoPlay.onJoin()
        autoPlay.onLocation(alreadyInSkyBlock = true)
        autoPlay.tick(1000)
        assertEquals(0, sent)
    }

    @Test
    fun `falls back to a timeout when the location packet never comes`() {
        val autoPlay = autoPlay(hasLocationApi = true)
        autoPlay.request()
        autoPlay.onJoin()
        autoPlay.tick(AutoPlay.LOCATION_TIMEOUT_TICKS - 1)
        assertEquals(0, sent)
        autoPlay.tick(1000)
        assertEquals(1, sent)
    }

    @Test
    fun `without the mod api it waits a short delay`() {
        val autoPlay = autoPlay(hasLocationApi = false)
        autoPlay.request()
        autoPlay.onJoin()
        autoPlay.tick(AutoPlay.FALLBACK_DELAY_TICKS - 1)
        assertEquals(0, sent)
        autoPlay.tick(1)
        assertEquals(1, sent)
    }

    @Test
    fun `a disconnect before the lobby cancels it`() {
        val autoPlay = autoPlay(hasLocationApi = false)
        autoPlay.request()
        autoPlay.onJoin()
        autoPlay.reset()
        autoPlay.tick(1000)
        autoPlay.onJoin()
        autoPlay.tick(1000)
        assertEquals(0, sent)
    }

    @Test
    fun `only the join right after the button press counts`() {
        val autoPlay = autoPlay(hasLocationApi = false)
        autoPlay.request()
        autoPlay.onJoin()
        autoPlay.tick(1000)
        autoPlay.reset()
        autoPlay.onJoin()
        autoPlay.tick(1000)
        assertEquals(1, sent)
    }
}
