package me.kmsold.joinskyblock

import me.kmsold.joinskyblock.compat.SkyHanniCompat
import me.kmsold.joinskyblock.config.ConfigManager
import me.kmsold.joinskyblock.config.JsbConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConfigTest {

    @Test
    fun `missing keys keep their defaults`() {
        val config = ConfigManager.parse("""{ "autoPlaySkyblock": false }""")!!
        assertFalse(config.autoPlaySkyblock)
        assertTrue(config.showButton)
        assertEquals(JsbConfig.DEFAULT_SERVER_ADDRESS, config.serverAddress)
    }

    @Test
    fun `a blank address falls back to hypixel`() {
        val config = ConfigManager.parse("""{ "serverAddress": "  " }""")!!
        assertEquals(JsbConfig.DEFAULT_SERVER_ADDRESS, config.serverAddress)
    }

    @Test
    fun `a broken file is rejected instead of crashing`() {
        assertNull(ConfigManager.parse("""{ "showButton": """))
    }

    @Test
    fun `skyhanni auto join is read from misc`() {
        assertTrue(SkyHanniCompat.isAutoJoinEnabled("""{ "misc": { "autoJoinSkyblock": true } }"""))
        assertFalse(SkyHanniCompat.isAutoJoinEnabled("""{ "misc": { "autoJoinSkyblock": false } }"""))
        assertFalse(SkyHanniCompat.isAutoJoinEnabled("""{ "misc": {} }"""))
        assertFalse(SkyHanniCompat.isAutoJoinEnabled("""{}"""))
    }
}
