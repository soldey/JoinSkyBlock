package me.kmsold.joinskyblock.config

import com.google.gson.annotations.Expose

/** Stored as `config/joinskyblock.json`. Every field has a default, so Gson can fill in missing keys. */
class JsbConfig {

    /** Where the button connects to. */
    @Expose
    var serverAddress: String = DEFAULT_SERVER_ADDRESS

    /** Send `/play skyblock` once after joining; off means the button only connects. */
    @Expose
    var autoPlaySkyblock: Boolean = true

    /** Replace Singleplayer with the button. Off gives the vanilla menu back. */
    @Expose
    var showButton: Boolean = true

    /** Send `/play skyblock` again after SkyBlock throws the player into a Hypixel lobby. */
    @Expose
    var rejoinSkyblock: Boolean = false

    /** How long to wait in the lobby before going back. */
    @Expose
    var rejoinDelaySeconds: Int = 70

    companion object {
        const val DEFAULT_SERVER_ADDRESS = "mc.hypixel.net"
    }
}
