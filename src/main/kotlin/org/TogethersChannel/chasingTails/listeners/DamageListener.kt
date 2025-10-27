package org.TogethersChannel.chasingTails.listeners

import org.TogethersChannel.chasingTails.game.GameManager
import org.TogethersChannel.chasingTails.game.GameState
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.entity.TNTPrimed
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent

class DamageListener : Listener {

    @EventHandler
    fun onPlayerDamage(event: EntityDamageByEntityEvent) {
        if (GameManager.state == GameState.WAITING || GameManager.state == GameState.ENDED) return

        // 피해자가 플레이어가 아니면 무시
        if (event.entity !is Player) return
        val victim = event.entity as Player

        // ▼▼▼ [수정된 부분] 공격자를 정확히 추적하는 로직 ▼▼▼
        var attacker: Player? = null

        when (val damager = event.damager) {
            is Player -> {
                // 1. 직접 공격
                attacker = damager
            }
            is Projectile -> {
                // 2. 화살, 트라이던트 등 발사체 공격
                if (damager.shooter is Player) {
                    attacker = damager.shooter as Player
                }
            }
            is TNTPrimed -> {
                // 3. TNT 공격
                if (damager.source is Player) {
                    attacker = damager.source as Player
                }
            }
        }

        // 공격자가 플레이어가 아니면 (예: 몬스터, 환경) 이 리스너는 관여하지 않음
        if (attacker == null) return

        // --- 이제 'attacker' 변수에 공격한 플레이어가 확실히 담겼으므로, 기존 PvP 규칙을 적용 ---

        // 평화 시간 중일 경우 공격 취소
        if (GameManager.state == GameState.GRACE_PERIOD) {
            attacker.sendMessage("§c[알림] §f평화 시간 중에는 다른 플레이어를 공격할 수 없습니다.")
            event.isCancelled = true
            return
        }

        val attackerLeaderId = GameManager.getTeamLeader(attacker.uniqueId) ?: return
        val victimLeaderId = GameManager.getTeamLeader(victim.uniqueId) ?: return

        // 같은 팀원 공격 방지
        if (attackerLeaderId == victimLeaderId) {
            event.isCancelled = true
            attacker.sendMessage("§c같은 팀원을 공격할 수 없습니다!")
            return
        }

        // 목표가 아닌 대상 공격 방지
        if (!GameManager.isCorrectTarget(attackerLeaderId, victimLeaderId)) {
            event.isCancelled = true
            attacker.sendMessage("§c이 플레이어는 당신의 목표가 아닙니다!")
        }
    }
}