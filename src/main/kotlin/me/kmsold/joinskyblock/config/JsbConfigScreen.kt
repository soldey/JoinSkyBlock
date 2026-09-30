package me.kmsold.joinskyblock.config

import me.kmsold.joinskyblock.compat.McCompat
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.CycleButton
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.StringWidget
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component

/** The three options on plain vanilla widgets, opened from Mod Menu. */
class JsbConfigScreen(private val parent: Screen?) : Screen(Component.translatable("joinskyblock.config.title")) {

    private lateinit var addressBox: EditBox

    override fun init() {
        val config = ConfigManager.config
        val left = width / 2 - WIDTH / 2
        var y = height / 4

        addRenderableWidget(StringWidget(left, y - 30, WIDTH, 9, title, font))

        addRenderableWidget(StringWidget(left, y, WIDTH, 9, Component.translatable("joinskyblock.config.serverAddress"), font))
        y += 12
        addressBox = EditBox(font, left, y, WIDTH, 20, Component.translatable("joinskyblock.config.serverAddress"))
        addressBox.setMaxLength(255)
        addressBox.setValue(config.serverAddress)
        addRenderableWidget(addressBox)
        y += 28

        addRenderableWidget(
            CycleButton.onOffBuilder(config.autoPlaySkyblock)
                .create(left, y, WIDTH, 20, Component.translatable("joinskyblock.config.autoPlaySkyblock")) { _, value ->
                    config.autoPlaySkyblock = value
                },
        )
        y += 24

        addRenderableWidget(
            CycleButton.onOffBuilder(config.showButton)
                .create(left, y, WIDTH, 20, Component.translatable("joinskyblock.config.showButton")) { _, value ->
                    config.showButton = value
                },
        )
        y += 24

        addRenderableWidget(
            CycleButton.onOffBuilder(config.rejoinSkyblock)
                .create(left, y, WIDTH, 20, Component.translatable("joinskyblock.config.rejoinSkyblock")) { _, value ->
                    config.rejoinSkyblock = value
                },
        )
        y += 32

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE) { onClose() }.bounds(left, y, WIDTH, 20).build())
    }

    override fun onClose() {
        ConfigManager.config.serverAddress = addressBox.value.trim().ifEmpty { JsbConfig.DEFAULT_SERVER_ADDRESS }
        ConfigManager.save()
        McCompat.setScreen(parent)
    }

    private companion object {
        const val WIDTH = 200
    }
}
