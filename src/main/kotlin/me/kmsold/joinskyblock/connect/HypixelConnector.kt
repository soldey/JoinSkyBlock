package me.kmsold.joinskyblock.connect

import me.kmsold.joinskyblock.JoinSkyBlock
import me.kmsold.joinskyblock.compat.SkyHanniCompat
import me.kmsold.joinskyblock.config.ConfigManager
import me.kmsold.joinskyblock.config.JsbConfig
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.ConnectScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.multiplayer.ServerData
import net.minecraft.client.multiplayer.ServerList
import net.minecraft.client.multiplayer.resolver.ServerAddress

object HypixelConnector {

    private const val DEFAULT_NAME = "Hypixel"

    fun connect(parent: Screen) {
        val config = ConfigManager.config
        val address = config.serverAddress.trim().ifEmpty { JsbConfig.DEFAULT_SERVER_ADDRESS }
        val minecraft = Minecraft.getInstance()

        if (config.autoPlaySkyblock && !SkyHanniCompat.autoJoinsSkyBlock()) {
            JoinSkyBlock.autoPlay.request()
        } else {
            JoinSkyBlock.autoPlay.reset()
        }

        JoinSkyBlock.logger.info("Connecting to {}", address)
        // The last argument is the TransferState of a server-initiated transfer, which this is not.
        ConnectScreen.startConnecting(
            parent,
            minecraft,
            ServerAddress.parseString(address),
            serverData(minecraft, address),
            false,
            null,
        )
    }

    /**
     * The entry from the player's real server list. A throwaway [ServerData] would work for the
     * connection, but the answer to "this server wants a resource pack" is stored on that entry -
     * without a saved one the question would come back on every join.
     */
    private fun serverData(minecraft: Minecraft, address: String): ServerData {
        val list = ServerList(minecraft)
        list.load()
        list.get(address)?.let { return it }

        val data = ServerData(DEFAULT_NAME, address, ServerData.Type.OTHER)
        list.add(data, false)
        list.save()
        JoinSkyBlock.logger.info("Added {} to the server list", address)
        return data
    }
}

