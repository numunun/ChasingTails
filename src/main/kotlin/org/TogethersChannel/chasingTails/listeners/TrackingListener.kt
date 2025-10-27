package org.TogethersChannel.chasingTails.listeners

import org.TogethersChannel.chasingTails.ChasingTails
import org.TogethersChannel.chasingTails.game.ConfigManager
import org.TogethersChannel.chasingTails.game.GameManager
import org.TogethersChannel.chasingTails.game.GameState
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable

class TrackingListener : Listener {

    private val plugin = JavaPlugin.getPlugin(ChasingTails::class.java)

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (GameManager.state != GameState.RUNNING) return
        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) return

        val player = event.player
        val item = player.inventory.itemInMainHand

        // ▼▼▼ [수정된 부분] when 대신 if를 사용 ▼▼▼
        if (item.type == ConfigManager.trackingItem) {
            val target = GameManager.getTargetOf(player)
            if (target != null) {
                item.amount -= 1
                player.sendMessage("§b[알림] §f아이템을 소모하여 목표물의 위치를 추적합니다!")
                showParticlePath(player, target)
            } else {
                player.sendMessage("§b[알림] §f추적할 목표물이 없습니다.")
            }
            event.isCancelled = true
        }
    }

    private fun showParticlePath(from: Player, to: Player) {
        object : BukkitRunnable() {
            var duration = 0
            override fun run() {
                if (duration >= 100 || !from.isOnline || !to.isOnline) {
                    this.cancel()
                    return
                }

                val start = from.location.add(0.0, 1.0, 0.0)
                val end = to.location.add(0.0, 1.0, 0.0)
                val direction = end.toVector().subtract(start.toVector()).normalize()

                for (i in 1..20) {
                    val particleLocation = start.clone().add(direction.clone().multiply(i))
                    // 1.20.4 호환 파티클 이름으로 수정
                    from.spawnParticle(Particle.WAX_OFF, particleLocation, 1, 0.0, 0.0, 0.0, 0.0)
                }
                duration += 5
            }
        }.runTaskTimer(plugin, 0L, 5L)
    }
}