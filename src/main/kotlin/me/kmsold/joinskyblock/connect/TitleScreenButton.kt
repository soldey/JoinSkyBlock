package me.kmsold.joinskyblock.connect

import me.kmsold.joinskyblock.JoinSkyBlock
import me.kmsold.joinskyblock.config.ConfigManager
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.Screens
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.contents.TranslatableContents

/**
 * Puts our button where Singleplayer was. The vanilla button is found by its translation key
 * rather than by position, so it is found wherever another mod moved it.
 *
 * Our button takes Singleplayer's slot in the widget list, not just its coordinates. Mod Menu
 * lays the menu out by list index - buttons before its Mods button move up, the rest move down -
 * so a button appended at the end was pushed down onto Multiplayer and hid it.
 */
object TitleScreenButton {

    private const val SINGLEPLAYER_KEY = "menu.singleplayer"

    fun register() {
        ScreenEvents.AFTER_INIT.register { _, screen, _, _ ->
            if (screen !is TitleScreen) return@register
            // Back on the title screen means the connection we started is over, so is our job.
            JoinSkyBlock.autoPlay.reset()
            if (!ConfigManager.config.showButton) return@register

            val widgets = Screens.getWidgets(screen)
            val index = widgets.indexOfFirst { it.isSingleplayer() }
            if (index < 0) {
                JoinSkyBlock.logger.warn("No Singleplayer button on the title screen, not adding ours")
                return@register
            }

            val singleplayer = widgets[index]
            widgets[index] = Button.builder(label()) { HypixelConnector.connect(screen) }
                .bounds(singleplayer.x, singleplayer.y, singleplayer.width, singleplayer.height)
                .build()
        }
    }

    private fun label(): Component = Component.translatable(
        if (ConfigManager.config.autoPlaySkyblock) "joinskyblock.button.skyblock" else "joinskyblock.button.hypixel",
    )

    private fun AbstractWidget.isSingleplayer(): Boolean =
        (message.contents as? TranslatableContents)?.key == SINGLEPLAYER_KEY
}
