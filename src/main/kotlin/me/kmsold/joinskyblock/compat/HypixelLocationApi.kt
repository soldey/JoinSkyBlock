package me.kmsold.joinskyblock.compat

import me.kmsold.joinskyblock.JoinSkyBlock
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
            val inSkyBlock = packet.serverType.getOrNull() == GameType.SKYBLOCK
            JoinSkyBlock.logger.debug("Hypixel location: server={} type={}", packet.serverName, packet.serverType.getOrNull())
            // Sending a command belongs on the client thread, whichever thread delivered the packet.
            Minecraft.getInstance().execute { JoinSkyBlock.autoPlay.onLocation(inSkyBlock) }
        }
        JoinSkyBlock.logger.info("Using hypixel-mod-api to detect the lobby")
    }
}
