package me.kmsold.joinskyblock.compat

import me.kmsold.joinskyblock.JoinSkyBlock
import me.kmsold.joinskyblock.connect.Rejoin
import me.kmsold.joinskyblock.connect.SkyBlockRejoin
import net.hypixel.data.type.GameType
import net.hypixel.modapi.HypixelModAPI
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket
import net.minecraft.client.Minecraft
import kotlin.jvm.optionals.getOrNull

/**
 * Location updates straight from Hypixel. Only touched when the `hypixel-mod-api` mod is
 * installed - see the guard in [JoinSkyBlock], which keeps this class from being loaded otherwise.
 */
object HypixelLocationApi {

    fun register() {
        val api = HypixelModAPI.getInstance()
        api.subscribeToEventPacket(ClientboundLocationPacket::class.java)
        api.createHandler(ClientboundLocationPacket::class.java) { packet ->
            // The SkyBlock lobby reports the SkyBlock type too; only a lobby name tells it apart.
            val place = when {
                packet.serverName.contains("limbo", ignoreCase = true) -> SkyBlockRejoin.Place.LIMBO
                packet.lobbyName.isPresent -> SkyBlockRejoin.Place.LOBBY
                packet.serverType.getOrNull() == GameType.SKYBLOCK -> SkyBlockRejoin.Place.SKYBLOCK
                else -> SkyBlockRejoin.Place.OTHER
            }
            JoinSkyBlock.logger.debug(
                "Hypixel location: server={} type={} lobby={}",
                packet.serverName, packet.serverType.getOrNull(), packet.lobbyName.getOrNull(),
            )
            // Sending a command belongs on the client thread, whichever thread delivered the packet.
            Minecraft.getInstance().execute {
                JoinSkyBlock.autoPlay.onLocation(alreadyInSkyBlock = place == SkyBlockRejoin.Place.SKYBLOCK)
                Rejoin.onLocation(place)
            }
        }
        JoinSkyBlock.logger.info("Using hypixel-mod-api to detect the lobby")
    }
}
