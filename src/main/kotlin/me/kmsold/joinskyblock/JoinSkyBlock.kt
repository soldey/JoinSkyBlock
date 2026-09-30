package me.kmsold.joinskyblock

import me.kmsold.joinskyblock.compat.HypixelLocationApi
import me.kmsold.joinskyblock.config.ConfigManager
import me.kmsold.joinskyblock.connect.AutoPlay
import me.kmsold.joinskyblock.connect.Rejoin
import me.kmsold.joinskyblock.connect.TitleScreenButton
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object JoinSkyBlock : ClientModInitializer {

    const val MOD_ID = "joinskyblock"

    val logger: Logger = LoggerFactory.getLogger("Join SkyBlock")

    /** The command itself, without the slash. Sent once per button press, never repeated. */
    const val PLAY_COMMAND = "play skyblock"

    val autoPlay = AutoPlay(
        hasLocationApi = FabricLoader.getInstance().isModLoaded("hypixel-mod-api"),
        send = { sendCommand(PLAY_COMMAND) },
    )

    override fun onInitializeClient() {
        ConfigManager.load()
        TitleScreenButton.register()
        Rejoin.register()
        registerConnectionHooks()
        ClientTickEvents.END_CLIENT_TICK.register {
            autoPlay.tick()
            Rejoin.tick()
        }
        logger.info("Join SkyBlock ready")
    }

    private fun registerConnectionHooks() {
        if (autoPlay.hasLocationApi) {
            runCatching { HypixelLocationApi.register() }
                .onFailure { logger.error("Could not hook into hypixel-mod-api", it) }
        } else {
            logger.info("hypixel-mod-api is not installed, /play skyblock will be sent after a short delay")
            logger.info("Without hypixel-mod-api the SkyBlock rejoin cannot tell where the player is and stays off")
        }

        ClientPlayConnectionEvents.JOIN.register { _, _, _ -> autoPlay.onJoin() }
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            autoPlay.reset()
            Rejoin.state.reset()
        }
    }

    /** True while the mod itself sends a command, so the typed-command listener can skip it. */
    var sendingOwnCommand = false
        private set

    /** Sends [command], without the slash, as if typed - but marked as the mod's own. */
    fun sendCommand(command: String) {
        val connection = Minecraft.getInstance().connection
        if (connection == null) {
            logger.warn("Not connected any more, /{} was not sent", command)
            return
        }
        logger.info("Sending /{}", command)
        sendingOwnCommand = true
        try {
            connection.sendCommand(command)
        } finally {
            sendingOwnCommand = false
        }
    }
}
