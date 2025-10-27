package org.TogethersChannel.chasingTails.game

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.TogethersChannel.chasingTails.ChasingTails
import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Scoreboard
import java.util.*
import kotlin.random.Random

object GameManager {

    var state: GameState = GameState.WAITING
    private val teams = mutableMapOf<UUID, MutableList<UUID>>()
    private val targets = mutableMapOf<UUID, UUID>()
    private val plugin: ChasingTails = JavaPlugin.getPlugin(ChasingTails::class.java)
    private val teamDisplayNames = mutableMapOf<UUID, String>()
    private val availableTeamNames = listOf(
        "§c빨강 팀", "§6주황 팀", "§e노랑 팀", "§a연두 팀", "§2초록 팀", "§b청록 팀",
        "§3진청록 팀", "§9파랑 팀", "§1남색 팀", "§d분홍 팀", "§5보라 팀", "§f하양 팀",
        "§7회색 팀", "§8쥐색 팀"
    )
    private var gameScoreboard: Scoreboard? = null
    private var sidebarObjective: Objective? = null
    private var gracePeriodEndTime: Long = 0
    private val scoreboardTeamNames = mutableMapOf<UUID, String>()
    private val teamColors = mapOf(
        'c' to ChatColor.RED, '6' to ChatColor.GOLD, 'e' to ChatColor.YELLOW, 'a' to ChatColor.GREEN,
        '2' to ChatColor.DARK_GREEN, 'b' to ChatColor.AQUA, '3' to ChatColor.DARK_AQUA, '9' to ChatColor.BLUE,
        '1' to ChatColor.DARK_BLUE, 'd' to ChatColor.LIGHT_PURPLE, '5' to ChatColor.DARK_PURPLE,
        'f' to ChatColor.WHITE, '7' to ChatColor.GRAY, '8' to ChatColor.DARK_GRAY
    )
    private var originalKeepInventoryRule: Boolean? = null

    fun startGame(players: List<Player>) {
        if (players.size < 2) return
        teams.clear(); targets.clear(); teamDisplayNames.clear(); scoreboardTeamNames.clear()
        gameScoreboard = Bukkit.getScoreboardManager().newScoreboard
        sidebarObjective = gameScoreboard!!.registerNewObjective("ChasingTails", Criteria.DUMMY, "§e§l꼬리잡기")
        sidebarObjective!!.displaySlot = DisplaySlot.SIDEBAR
        val world = players.first().world
        world.worldBorder.setCenter(0.0, 0.0)
        world.worldBorder.size = ConfigManager.worldBorderSize
        originalKeepInventoryRule = world.getGameRuleValue(GameRule.KEEP_INVENTORY)
        world.setGameRule(GameRule.KEEP_INVENTORY, ConfigManager.keepInventoryOnNormalDeath)
        Bukkit.broadcastMessage("§e[꼬리잡기] §f인벤세이브 규칙을 플러그인 설정(§b${ConfigManager.keepInventoryOnNormalDeath}§f)으로 적용합니다.")

        val slowness = PotionEffect(PotionEffectType.SLOWNESS, 120, 255, false, false) // 6초간 속박
        val blindness = PotionEffect(PotionEffectType.BLINDNESS, 120, 1, false, false) // 6초간 암흑
        val jump = PotionEffect(PotionEffectType.JUMP_BOOST, 120, 128, false, false) // 6초간 점프 방지

        players.forEach { player ->
            val randomX = Random.nextInt(-ConfigManager.randomSpawnRange, ConfigManager.randomSpawnRange + 1)
            val randomZ = Random.nextInt(-ConfigManager.randomSpawnRange, ConfigManager.randomSpawnRange + 1)
            val safeLocation = Location(world, randomX.toDouble(), world.getHighestBlockYAt(randomX, randomZ) + 1.0, randomZ.toDouble())
            player.teleport(safeLocation)
            player.setBedSpawnLocation(safeLocation, true)

            player.gameMode = GameMode.ADVENTURE
            player.addPotionEffect(slowness)
            player.addPotionEffect(blindness)
            player.addPotionEffect(jump)
        }
        val assignedTeamNames = availableTeamNames.take(players.size)
        for ((index, player) in players.withIndex()) {
            val currentPlayerId = player.uniqueId
            val targetPlayerId = if (index == players.size - 1) players.first().uniqueId else players[index + 1].uniqueId
            teams[currentPlayerId] = mutableListOf(currentPlayerId)
            targets[currentPlayerId] = targetPlayerId
            val assignedDisplayName = assignedTeamNames[index]
            teamDisplayNames[currentPlayerId] = assignedDisplayName
            val scoreboardTeamName = "ct_team_$index"
            scoreboardTeamNames[currentPlayerId] = scoreboardTeamName
            val scoreboardTeam = gameScoreboard!!.registerNewTeam(scoreboardTeamName)
            scoreboardTeam.color = teamColors[assignedDisplayName[1]] ?: ChatColor.WHITE
            scoreboardTeam.addEntry(player.name)
        }
        players.forEach { it.scoreboard = gameScoreboard!! }

        state = GameState.PREPARING

        startCountdown(players)
    }

    private fun startCountdown(players: List<Player>) {
        var countdown = 5
        ChasingTails.countdownTask = object : BukkitRunnable() {
            override fun run() {
                if (countdown > 0) {
                    val title = "§e${countdown}초"
                    val subtitle = "§f게임 시작 까지"
                    players.forEach { it.sendTitle(title, subtitle, 0, 25, 5) }
                    countdown--
                } else {
                    this.cancel()
                    players.forEach {
                        it.removePotionEffect(PotionEffectType.SLOWNESS)
                        it.removePotionEffect(PotionEffectType.BLINDNESS)
                        it.removePotionEffect(PotionEffectType.JUMP_BOOST)
                        it.gameMode = GameMode.SURVIVAL
                        it.sendTitle("§aSTART!", "", 0, 40, 10)
                    }
                    startGracePeriod()
                }
            }
        }.runTaskTimer(plugin, 0L, 20L)
    }

    private fun startGracePeriod() {
        state = GameState.GRACE_PERIOD
        gracePeriodEndTime = System.currentTimeMillis() + (ConfigManager.gracePeriodDuration * 1000)
        Bukkit.broadcastMessage("§a[꼬리잡기] §f평화 시간이 시작되었습니다! §e${ConfigManager.gracePeriodDuration / 60}분§f 동안 PvP가 비활성화됩니다.")

        plugin.startBossBarTask()
        plugin.startProximityTasks()
        plugin.startSidebarUpdateTask()

        ChasingTails.gracePeriodTask = plugin.server.scheduler.runTaskLater(plugin, Runnable {
            if (state == GameState.GRACE_PERIOD) {
                state = GameState.RUNNING
                Bukkit.broadcastMessage("§c[꼬리잡기] §f평화 시간이 종료되었습니다. PvP가 활성화됩니다!")
            }
        }, ConfigManager.gracePeriodDuration * 20L)
    }

    fun stopGame() {
        if (state == GameState.WAITING) return

        ChasingTails.countdownTask?.cancel()
        if (state == GameState.PREPARING) {
            Bukkit.getOnlinePlayers().forEach {
                it.removePotionEffect(PotionEffectType.SLOWNESS)
                it.removePotionEffect(PotionEffectType.BLINDNESS)
                it.removePotionEffect(PotionEffectType.JUMP_BOOST)
                if (it.gameMode == GameMode.ADVENTURE) it.gameMode = GameMode.SURVIVAL
            }
        }

        ChasingTails.gracePeriodTask?.cancel(); plugin.stopSidebarUpdateTask()
        ChasingTails.deathDummies.values.forEach { it.remove() }; ChasingTails.deathDummies.clear()
        val mainScoreboard = Bukkit.getScoreboardManager().mainScoreboard
        Bukkit.getOnlinePlayers().forEach { if(it.scoreboard == gameScoreboard) it.scoreboard = mainScoreboard }
        gameScoreboard = null; sidebarObjective = null
        val world = Bukkit.getWorlds().firstOrNull()
        if (world != null) {
            world.worldBorder.reset()
            if (originalKeepInventoryRule != null) {
                world.setGameRule(GameRule.KEEP_INVENTORY, originalKeepInventoryRule!!)
                Bukkit.broadcastMessage("§e[꼬리잡기] §f게임 종료. 인벤세이브 규칙을 서버 원래 설정으로 복원합니다.")
                originalKeepInventoryRule = null
            }
        }
        teams.clear(); targets.clear(); teamDisplayNames.clear(); scoreboardTeamNames.clear(); ChasingTails.playersInDanger.clear()
        state = GameState.WAITING; plugin.stopBossBarTask(); plugin.stopProximityTasks()
    }

    fun updateSidebar() {
        val objective = sidebarObjective ?: return
        gameScoreboard?.entries?.forEach { gameScoreboard?.resetScores(it) }
        var score = 16

        if (state == GameState.PREPARING) {
            objective.getScore("§a잠시 후 시작...").score = score--
        }

        if (state == GameState.GRACE_PERIOD) {
            val remainingSeconds = (gracePeriodEndTime - System.currentTimeMillis()) / 1000
            if (remainingSeconds >= 0) {
                objective.getScore("§a평화 시간: §f${String.format("%02d:%02d", remainingSeconds / 60, remainingSeconds % 60)}").score = score--
            }
        }

        objective.getScore(" ").score = score--
        val sortedLeaders = teams.keys.sortedBy { availableTeamNames.indexOf(teamDisplayNames[it]) }
        for (leaderId in sortedLeaders) {
            val hunterName = teamDisplayNames[leaderId] ?: continue
            val targetName = teamDisplayNames[targets[leaderId]] ?: "§7(소멸)"
            var entry = "$hunterName §f-> $targetName"
            if (entry.length > 40) entry = entry.substring(0, 40)
            objective.getScore(entry).score = score--
            if (score <= 0) break
        }
    }

    fun getTeamLeader(playerId: UUID): UUID? = teams.entries.find { it.value.contains(playerId) }?.key
    fun isCorrectTarget(attackerLeaderId: UUID, victimLeaderId: UUID): Boolean = targets[attackerLeaderId] == victimLeaderId
    fun getTeamName(player: Player): String = teamDisplayNames[getTeamLeader(player.uniqueId)] ?: "§7솔로"

    fun getTargetOf(player: Player): Player? {
        val playerLeaderId = getTeamLeader(player.uniqueId) ?: return null
        val targetLeaderId = targets[playerLeaderId] ?: return null
        val targetPlayerId = teams[targetLeaderId]?.firstOrNull() ?: return null
        return Bukkit.getPlayer(targetPlayerId)
    }

    fun getHunterOf(player: Player): Player? {
        val playerLeaderId = getTeamLeader(player.uniqueId) ?: return null
        val hunterEntry = targets.entries.find { it.value == playerLeaderId }
        val hunterLeaderId = hunterEntry?.key ?: return null
        val hunterPlayerId = teams[hunterLeaderId]?.firstOrNull() ?: return null
        return Bukkit.getPlayer(hunterPlayerId)
    }

    fun getTargetDisplayName(player: Player): String {
        val target = getTargetOf(player) ?: return "없음"
        return if (ConfigManager.useAnonymousNames) getTeamName(target) else target.name
    }

    fun getHunterDisplayName(player: Player): String {
        val hunter = getHunterOf(player) ?: return "없음"
        return if (ConfigManager.useAnonymousNames) getTeamName(hunter) else hunter.name
    }

    fun handlePlayerCapture(killer: Player, victim: Player) {
        val killerLeaderId = getTeamLeader(killer.uniqueId) ?: return
        val victimLeaderId = getTeamLeader(victim.uniqueId) ?: return
        if (killerLeaderId == victimLeaderId) return
        Bukkit.broadcastMessage("§e[꼬리잡기] ${getTeamName(killer)}§f이(가) ${getTeamName(victim)}§f을(를) 흡수했습니다!")
        val victimTeamMembers = teams[victimLeaderId]?.toList() ?: return
        val killerTeam = teams[killerLeaderId] ?: return
        val killerSCTeam = gameScoreboard?.getTeam(scoreboardTeamNames[killerLeaderId] ?: return)
        val victimSCTeam = gameScoreboard?.getTeam(scoreboardTeamNames[victimLeaderId] ?: return)
        victimSCTeam?.entries?.forEach { killerSCTeam?.addEntry(it) }; victimSCTeam?.unregister()
        killerTeam.addAll(victimTeamMembers); teams.remove(victimLeaderId)
        val newTarget = targets[victimLeaderId] ?: return
        targets[killerLeaderId] = newTarget; targets.remove(victimLeaderId)
        teamDisplayNames.remove(victimLeaderId); scoreboardTeamNames.remove(victimLeaderId)
        checkWinCondition()
    }

    fun updatePlayersInDanger() {
        if (state != GameState.RUNNING) return
        Bukkit.getOnlinePlayers().forEach { player ->
            if (getTeamLeader(player.uniqueId) == null) return@forEach
            val hunter = getHunterOf(player)
            if (hunter != null && player.world == hunter.world && player.location.distance(hunter.location) <= ConfigManager.proximityDistance) {
                ChasingTails.playersInDanger.add(player.uniqueId)
            } else {
                ChasingTails.playersInDanger.remove(player.uniqueId)
            }
        }
    }

    fun showWarningToPlayersInDanger() {
        if (state != GameState.RUNNING) return
        ChasingTails.playersInDanger.mapNotNull { Bukkit.getPlayer(it) }.forEach { player ->
            player.sendActionBar(Component.text("[ ♥ ]", NamedTextColor.RED, TextDecoration.BOLD))
            player.playSound(player.location, Sound.ENTITY_WARDEN_HEARTBEAT, 0.8f, 1.0f)
        }
    }

    private fun checkWinCondition() {
        if ((state == GameState.RUNNING || state == GameState.GRACE_PERIOD) && teams.size <= 1) {
            val winningTeamName = teamDisplayNames[teams.keys.firstOrNull()] ?: "최후의 팀"
            Bukkit.getOnlinePlayers().forEach { p ->
                p.sendTitle("§6승리!", "${winningTeamName}§f이(가) 최종 생존했습니다!", 10, 70, 20)
                p.playSound(p.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f)
            }
            stopGame()
        }
    }
}