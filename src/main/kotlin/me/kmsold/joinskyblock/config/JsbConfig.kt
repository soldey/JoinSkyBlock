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

    /** Reconnect by itself after the Hypixel connection is lost. */
    @Expose
    var autoReconnect: Boolean = false

    /** How long the "connection lost" screen counts down before reconnecting. */
    @Expose
    var reconnectDelaySeconds: Int = 70

    companion object {
        const val DEFAULT_SERVER_ADDRESS = "mc.hypixel.net"
    }
}
