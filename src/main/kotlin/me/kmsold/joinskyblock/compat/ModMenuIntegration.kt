package me.kmsold.joinskyblock.compat

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import me.kmsold.joinskyblock.config.JsbConfigScreen

/** Only loaded by Fabric when ModMenu is actually installed. */
class ModMenuIntegration : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> =
        ConfigScreenFactory { parent -> JsbConfigScreen(parent) }
}
