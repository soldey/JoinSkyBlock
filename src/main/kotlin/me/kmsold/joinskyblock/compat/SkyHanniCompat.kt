package me.kmsold.joinskyblock.compat

import com.google.gson.JsonParser
import me.kmsold.joinskyblock.JoinSkyBlock
import net.fabricmc.loader.api.FabricLoader
import kotlin.io.path.exists
import kotlin.io.path.readText

/**
 * SkyHanni has its own "Auto Join Skyblock" option. With both active the command would go out
 * twice, so when SkyHanni's is on we leave the job to it and only connect.
 */
object SkyHanniCompat {

    fun autoJoinsSkyBlock(): Boolean {
        val loader = FabricLoader.getInstance()
        if (!loader.isModLoaded("skyhanni")) return false
        val file = loader.configDir.resolve("skyhanni/config.json")
        if (!file.exists()) return false
        val enabled = runCatching { isAutoJoinEnabled(file.readText()) }
            .onFailure { JoinSkyBlock.logger.warn("Could not read SkyHanni's config", it) }
            .getOrDefault(false)
        if (enabled) JoinSkyBlock.logger.info("SkyHanni's Auto Join Skyblock is on, leaving /play skyblock to it")
        return enabled
    }

    /** `misc.autoJoinSkyblock` in SkyHanni's config.json. */
    fun isAutoJoinEnabled(json: String): Boolean {
        val misc = JsonParser.parseString(json).asJsonObject.getAsJsonObject("misc") ?: return false
        val value = misc.get("autoJoinSkyblock") ?: return false
        return value.isJsonPrimitive && value.asBoolean
    }
}
