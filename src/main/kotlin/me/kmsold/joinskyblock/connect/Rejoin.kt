package me.kmsold.joinskyblock.connect

import me.kmsold.joinskyblock.JoinSkyBlock
import me.kmsold.joinskyblock.config.ConfigManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

/** Wires [SkyBlockRejoin] to the game: location packets, typed commands, ticks and the chat. */
object Rejoin {

    val state = SkyBlockRejoin(send = { JoinSkyBlock.sendCommand(it) })

    private val enabled: Boolean get() = ConfigManager.config.rejoinSkyblock

    private val delayMillis: Long get() = ConfigManager.config.rejoinDelaySeconds.coerceIn(5, 600) * 1000L

    fun register() {
        ClientSendMessageEvents.COMMAND.register { command ->
            if (!JoinSkyBlock.sendingOwnCommand) state.onPlayerCommand(command)
        }
        ClientReceiveMessageEvents.GAME.register { message, overlay ->
            if (overlay) return@register
            val limbo = SkyBlockRejoin.parseKickMessage(message.string) ?: return@register
            JoinSkyBlock.logger.info("Hypixel kick message: {}", message.string)
            report(state.onKickMessage(limbo, enabled, delayMillis, System.currentTimeMillis()))
        }
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                ClientCommands.literal("jsb").then(
                    ClientCommands.literal("cancel").executes {
                        if (state.cancel() == null) tell(Component.translatable("joinskyblock.rejoin.nothing"))
                        else report(SkyBlockRejoin.Event.Cancelled)
                        1
                    },
                ),
            )
        }
    }

    /** Called on the client thread for every Hypixel location packet. */
    fun onLocation(place: SkyBlockRejoin.Place) {
        report(state.onLocation(place, enabled, delayMillis, System.currentTimeMillis()))
    }

    fun tick() {
        report(state.tick(enabled, System.currentTimeMillis()))
    }

    private fun report(event: SkyBlockRejoin.Event?) {
        when (event) {
            is SkyBlockRejoin.Event.Scheduled -> {
                val seconds = event.delayMillis / 1000
                JoinSkyBlock.logger.info("Thrown out of SkyBlock, /play skyblock in {} s", seconds)
                tell(Component.translatable("joinskyblock.rejoin.scheduled", "%d:%02d".format(seconds / 60, seconds % 60)))
            }
            SkyBlockRejoin.Event.Cancelled -> {
                JoinSkyBlock.logger.info("SkyBlock rejoin cancelled")
                tell(Component.translatable("joinskyblock.rejoin.cancelled"))
            }
            is SkyBlockRejoin.Event.Sent -> JoinSkyBlock.logger.info("SkyBlock rejoin: sent /{}", event.command)
            null -> {}
        }
    }

    /** A line only this player sees, nothing goes to the server. */
    private fun tell(message: Component) {
        val prefix = Component.literal("[Join SkyBlock] ").withStyle(ChatFormatting.GOLD)
        Minecraft.getInstance().player?.sendSystemMessage(prefix.append(message.copy().withStyle(ChatFormatting.YELLOW)))
    }
}
