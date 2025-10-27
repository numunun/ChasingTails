package org.TogethersChannel.chasingTails.game

import org.TogethersChannel.chasingTails.ChasingTails
import org.bukkit.Material
import org.bukkit.plugin.java.JavaPlugin

object ConfigManager {

    private val plugin = JavaPlugin.getPlugin(ChasingTails::class.java)

    var proximityDistance: Double = 15.0
    var trackingItem: Material = Material.DIAMOND
    var keepInventoryOnNormalDeath: Boolean = false
    var gracePeriodDuration: Long = 300
    var randomSpawnRange: Int = 500
    var worldBorderSize: Double = 1000.0
    var useAnonymousNames: Boolean = true
    var deathPenaltyDuration: Long = 60 // ▼▼▼ [추가된 부분] ▼▼▼

    fun loadConfig() {
        plugin.reloadConfig()
        val config = plugin.config

        proximityDistance = config.getDouble("proximity-warning-distance", 15.0)

        val materialName = config.getString("tracking-item", "DIAMOND")
        trackingItem = Material.getMaterial(materialName?.uppercase() ?: "DIAMOND") ?: Material.DIAMOND

        keepInventoryOnNormalDeath = config.getBoolean("keep-inventory-on-normal-death", false)
        gracePeriodDuration = config.getLong("grace-period-duration", 300)

        randomSpawnRange = config.getInt("random-spawn-range", 500)
        worldBorderSize = config.getDouble("world-border-size", 1000.0)
        useAnonymousNames = config.getBoolean("use-anonymous-names", true)

        deathPenaltyDuration = config.getLong("death-penalty-duration", 60) // ▼▼▼ [추가된 부분] ▼▼▼
    }
}