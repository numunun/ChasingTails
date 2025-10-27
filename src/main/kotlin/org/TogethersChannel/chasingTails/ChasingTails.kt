package org.TogethersChannel.chasingTails

import org.TogethersChannel.chasingTails.commands.ConfigCommands
import org.TogethersChannel.chasingTails.commands.GameCommands
import org.TogethersChannel.chasingTails.game.ConfigManager
import org.TogethersChannel.chasingTails.game.GameManager
import org.TogethersChannel.chasingTails.game.GameState
import org.TogethersChannel.chasingTails.listeners.*
import org.bukkit.Bukkit
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import org.bukkit.entity.ArmorStand
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import java.util.*

class ChasingTails : JavaPlugin() {

    companion object {
        val playerBossBars = mutableMapOf<UUID, BossBar>()
        val playersInDanger = mutableSetOf<UUID>()
        val deathDummies = mutableMapOf<UUID, ArmorStand>()
        var bossBarTask: BukkitTask? = null
        var proximityCheckTask: BukkitTask? = null
        var proximityWarningTask: BukkitTask? = null
        var gracePeriodTask: BukkitTask? = null
        var sidebarUpdateTask: BukkitTask? = null
        var countdownTask: BukkitTask? = null
    }

    override fun onEnable() {
        logger.info("===============================================================================================")
        logger.info("")
        logger.info("ChasingTails 플러그인이 활성화되었습니다.")
        logger.info("이 플러그인의 저작권은 Paradise Dev Team에게 있습니다. 제작자는 이 플러그인에 대한 저작권을 행사하지 않습니다.")
        logger.info("")
        logger.info("===============================================================================================")
        saveDefaultConfig()
        ConfigManager.loadConfig()
        getCommand("tail")?.setExecutor(GameCommands())
        getCommand("tailconfig")?.setExecutor(ConfigCommands())
        server.pluginManager.registerEvents(DamageListener(), this)
        server.pluginManager.registerEvents(DeathListener(), this)
        server.pluginManager.registerEvents(TrackingListener(), this)
        // server.pluginManager.registerEvents(ConnectionListener(), this) // 사용하지 않으므로 비활성화
        server.pluginManager.registerEvents(WorldListener(), this)
    }

    override fun onDisable() {
        deathDummies.values.forEach { it.remove() }
        deathDummies.clear()
        playerBossBars.values.forEach { it.removeAll() }
        playerBossBars.clear()
        bossBarTask?.cancel()
        proximityCheckTask?.cancel()
        proximityWarningTask?.cancel()
        gracePeriodTask?.cancel()
        sidebarUpdateTask?.cancel()
        countdownTask?.cancel()
        playersInDanger.clear()
        logger.info("ChasingTails 플러그인이 비활성화되었습니다.")
    }

    fun startBossBarTask() {
        bossBarTask = server.scheduler.runTaskTimer(this, Runnable {
            Bukkit.getOnlinePlayers().forEach { player ->
                if (GameManager.getTeamLeader(player.uniqueId) == null) return@forEach
                val bar = playerBossBars.computeIfAbsent(player.uniqueId) {
                    Bukkit.createBossBar("", BarColor.BLUE, BarStyle.SOLID)
                }
                bar.addPlayer(player)
                val teamName = GameManager.getTeamName(player)
                val targetName = GameManager.getTargetDisplayName(player)
                val hunterName = GameManager.getHunterDisplayName(player)
                val stateInfo = if (GameManager.state == GameState.GRACE_PERIOD) "§a(평화 시간)" else ""
                bar.setTitle("§b팀: $teamName §f| §c목표: $targetName §f| §9포식자: $hunterName $stateInfo")
            }
        }, 0L, 20L)
    }

    fun stopBossBarTask() {
        bossBarTask?.cancel()
        playerBossBars.values.forEach { it.removeAll() }
        playerBossBars.clear()
    }

    fun startProximityTasks() {
        proximityCheckTask = server.scheduler.runTaskTimer(this, Runnable {
            GameManager.updatePlayersInDanger()
        }, 0L, 5L)
        proximityWarningTask = server.scheduler.runTaskTimer(this, Runnable {
            GameManager.showWarningToPlayersInDanger()
        }, 0L, 20L)
    }

    fun stopProximityTasks() {
        proximityCheckTask?.cancel()
        proximityWarningTask?.cancel()
        playersInDanger.clear()
    }

    fun startSidebarUpdateTask() {
        sidebarUpdateTask = server.scheduler.runTaskTimer(this, Runnable {
            GameManager.updateSidebar()
        }, 0L, 20L)
    }

    fun stopSidebarUpdateTask() {
        sidebarUpdateTask?.cancel()
    }
}