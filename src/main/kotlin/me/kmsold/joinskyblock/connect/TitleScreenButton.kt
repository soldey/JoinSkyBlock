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
 * rather than by position, so it is found wherever another mod (Essential, for one) moved it -
 * and it is hidden instead of removed, so mods that kept a reference to it do not break.
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
            val singleplayer = widgets.firstOrNull { it.isSingleplayer() }
            if (singleplayer == null) {
                JoinSkyBlock.logger.warn("No Singleplayer button on the title screen, not adding ours")
                return@register
            }

            singleplayer.visible = false
            singleplayer.active = false
            widgets.add(
                Button.builder(label()) { HypixelConnector.connect(screen) }
                    .bounds(singleplayer.x, singleplayer.y, singleplayer.width, singleplayer.height)
                    .build(),
            )
        }
    }

    private fun label(): Component = Component.translatable(
        if (ConfigManager.config.autoPlaySkyblock) "joinskyblock.button.skyblock" else "joinskyblock.button.hypixel",
    )

    private fun AbstractWidget.isSingleplayer(): Boolean =
        (message.contents as? TranslatableContents)?.key == SINGLEPLAYER_KEY
}
