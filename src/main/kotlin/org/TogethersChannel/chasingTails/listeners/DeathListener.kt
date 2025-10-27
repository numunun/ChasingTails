package org.TogethersChannel.chasingTails.listeners

import org.TogethersChannel.chasingTails.ChasingTails
import org.TogethersChannel.chasingTails.game.ConfigManager
import org.TogethersChannel.chasingTails.game.GameManager
import org.TogethersChannel.chasingTails.game.GameState
import org.bukkit.*
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable

class DeathListener : Listener {

    private val plugin = JavaPlugin.getPlugin(ChasingTails::class.java)

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        // 게임이 'RUNNING' 상태일 때만 작동 (평화 시간, 준비 시간에는 PvP 자체가 불가능)
        if (GameManager.state != GameState.RUNNING) return

        val victim = event.entity
        val killer = victim.killer

        // 1. 포획 사망인지 판별
        var isCaptureDeath = false
        if (killer != null) {
            val killerLeaderId = GameManager.getTeamLeader(killer.uniqueId)
            val victimLeaderId = GameManager.getTeamLeader(victim.uniqueId)

            if (killerLeaderId != null && victimLeaderId != null) {
                if (GameManager.isCorrectTarget(killerLeaderId, victimLeaderId) && victim.uniqueId == victimLeaderId) {
                    isCaptureDeath = true
                }
            }
        }

        // 2. 사망 원인에 따라 로직 분기
        if (isCaptureDeath && killer != null) {
            // === [원인 1: 포획 사망] ===
            // 사망을 취소하고 '불사의 토템' 부활 효과를 줌
            event.isCancelled = true

            GameManager.handlePlayerCapture(killer, victim)

            victim.health = 10.0
            victim.world.playSound(victim.location, Sound.ITEM_TOTEM_USE, 1.0f, 1.0f)
            victim.world.spawnParticle(Particle.TOTEM_OF_UNDYING, victim.location.add(0.0, 1.0, 0.0), 50)

        } else {
            // === [원인 2: 그 외 모든 사망 (일반 PvP, 몬스터, 낙사 등)] ===
            // 사망 유예 시스템 발동
            event.isCancelled = true // 기본 사망 처리를 취소하고 직접 관리

            val deathLocation = victim.location
            val respawnLocation = victim.respawnLocation ?: victim.world.spawnLocation

            // 3-1. 더미(갑옷 거치대) 생성
            val dummy = createDeathDummy(victim, deathLocation)
            ChasingTails.deathDummies[victim.uniqueId] = dummy

            // 3-2. 플레이어 상태 변경
            victim.spigot().respawn() // 즉시 리스폰
            victim.gameMode = GameMode.SPECTATOR // 관전 모드로 변경
            victim.teleport(deathLocation) // 사망 위치로 다시 텔레포트
            victim.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, Int.MAX_VALUE, 255, false, false)) // 움직임 봉인
            victim.sendTitle("§c사망", "§f${ConfigManager.deathPenaltyDuration}초 후 부활합니다.", 5, 60, 5)

            // 3-3. 일정 시간 후 부활 및 더미 제거
            object : BukkitRunnable() {
                override fun run() {
                    val player = Bukkit.getPlayer(victim.uniqueId)
                    if (player != null && player.isOnline) {
                        player.removePotionEffect(PotionEffectType.SLOWNESS)
                        player.gameMode = GameMode.SURVIVAL
                        player.teleport(respawnLocation)
                        player.sendTitle("§a부활!", "§f다시 게임에 참여합니다.", 5, 40, 5)
                    }
                    ChasingTails.deathDummies.remove(victim.uniqueId)?.remove()
                }
            }.runTaskLater(plugin, 20L * ConfigManager.deathPenaltyDuration)
        }
    }

    private fun createDeathDummy(player: Player, location: Location): ArmorStand {
        val dummy = player.world.spawn(location, ArmorStand::class.java) {
            it.customName = "§7${player.name}의 흔적"
            it.isCustomNameVisible = true
            it.isInvulnerable = true
            it.setGravity(false)
            it.isSmall = true
        }

        val playerHead = ItemStack(Material.PLAYER_HEAD)
        val headMeta = playerHead.itemMeta as SkullMeta
        headMeta.owningPlayer = player
        playerHead.itemMeta = headMeta
        dummy.equipment.helmet = playerHead

        return dummy
    }
}