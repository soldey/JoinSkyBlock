package me.kmsold.joinskyblock.connect

import me.kmsold.joinskyblock.JoinSkyBlock
import me.kmsold.joinskyblock.config.ConfigManager
import me.kmsold.joinskyblock.config.JsbConfig
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.Screens
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.DisconnectedScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.network.chat.Component

/**
 * Puts a countdown on the "connection lost" screen after a Hypixel session and reconnects when
 * it runs out. The countdown lives only as long as that screen: leaving it, or pressing the
 * countdown button, cancels the reconnect.
 */
object AutoReconnect {

    val policy = ReconnectPolicy()

    /** The screen the countdown belongs to; resizing re-runs init on the same instance. */
    private var countdownScreen: Screen? = null
    private var deadline = 0L
    private var cancelled = false

    fun register() {
        ScreenEvents.AFTER_INIT.register { _, screen, _, _ ->
            if (screen !is DisconnectedScreen) return@register
            if (screen !== countdownScreen) {
                val config = ConfigManager.config
                val reason = screen.narrationMessage.string
                if (!policy.shouldReconnect(config.autoReconnect, reason)) return@register
                countdownScreen = screen
                cancelled = false
                deadline = System.currentTimeMillis() + delaySeconds(config) * 1000L
                JoinSkyBlock.logger.info("Lost the Hypixel connection, reconnecting in {} s", delaySeconds(config))
                ScreenEvents.remove(screen).register { forget(it) }
                ScreenEvents.afterTick(screen).register { tick(it) }
            }
            addButton(screen)
        }
    }

    private fun addButton(screen: Screen) {
        val widgets = Screens.getWidgets(screen)
        val bottom = widgets.maxOfOrNull { it.y + it.height } ?: (screen.height / 2)
        val button = Button.builder(label()) { cancel() }
            .bounds(screen.width / 2 - 100, minOf(bottom + 4, screen.height - 24), 200, 20)
            .build()
        button.active = !cancelled
        widgets.add(button)
        countdownButton = button
    }

    private var countdownButton: Button? = null

    private fun label(): Component {
        if (cancelled) return Component.translatable("joinskyblock.reconnect.cancelled")
        val left = ((deadline - System.currentTimeMillis()).coerceAtLeast(0) + 999) / 1000
        return Component.translatable("joinskyblock.reconnect.countdown", "%d:%02d".format(left / 60, left % 60))
    }

    private fun tick(screen: Screen) {
        if (screen !== countdownScreen || cancelled) return
        countdownButton?.message = label()
        if (System.currentTimeMillis() < deadline) return
        forget(screen)
        policy.onReconnectStarted()
        JoinSkyBlock.logger.info("Reconnecting to Hypixel")
        HypixelConnector.connect(TitleScreen())
    }

    private fun cancel() {
        if (cancelled) return
        cancelled = true
        JoinSkyBlock.logger.info("Reconnect cancelled")
        countdownButton?.let {
            it.message = label()
            it.active = false
        }
    }

    private fun forget(screen: Screen) {
        if (screen !== countdownScreen) return
        countdownScreen = null
        countdownButton = null
    }

    private fun delaySeconds(config: JsbConfig): Int = config.reconnectDelaySeconds.coerceIn(5, 600)

    /** Whether the session that just started is on Hypixel, judged by the server address. */
    fun isHypixel(minecraft: Minecraft): Boolean {
        val ip = minecraft.currentServer?.ip ?: return false
        val host = ip.substringBefore(':').trim().lowercase()
        val configured = ConfigManager.config.serverAddress.substringBefore(':').trim().lowercase()
        return host == configured || host == "hypixel.net" || host.endsWith(".hypixel.net")
    }
}
