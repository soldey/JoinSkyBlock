package me.kmsold.joinskyblock.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import me.kmsold.joinskyblock.JoinSkyBlock
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

object ConfigManager {

    val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .excludeFieldsWithoutExposeAnnotation()
        .create()

    private val configFile: Path by lazy {
        FabricLoader.getInstance().configDir.resolve("${JoinSkyBlock.MOD_ID}.json")
    }

    var config: JsbConfig = JsbConfig()
        private set

    fun load() {
        config = read() ?: JsbConfig()
        // Written back straight away so a fresh install shows every option in the file.
        save()
    }

    /** Parses [json], or returns null when it is not a usable config. Separate for the tests. */
    fun parse(json: String): JsbConfig? = runCatching { gson.fromJson(json, JsbConfig::class.java) }
        .onFailure { JoinSkyBlock.logger.error("Could not parse ${JoinSkyBlock.MOD_ID}.json, using defaults", it) }
        .getOrNull()
        ?.also { if (it.serverAddress.isBlank()) it.serverAddress = JsbConfig.DEFAULT_SERVER_ADDRESS }

    private fun read(): JsbConfig? {
        if (!configFile.exists()) return null
        val text = runCatching { configFile.readText() }
            .onFailure { JoinSkyBlock.logger.error("Could not read ${JoinSkyBlock.MOD_ID}.json, using defaults", it) }
            .getOrNull() ?: return null
        return parse(text)
    }

    fun save() {
        runCatching {
            configFile.parent.createDirectories()
            configFile.writeText(gson.toJson(config))
        }.onFailure { JoinSkyBlock.logger.error("Could not write ${JoinSkyBlock.MOD_ID}.json", it) }
    }
}
