package org.TogethersChannel.chasingTails.listeners

import org.TogethersChannel.chasingTails.game.GameManager
import org.TogethersChannel.chasingTails.game.GameState
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerTeleportEvent

class WorldListener : Listener {

    @EventHandler
    fun onPlayerTeleport(event: PlayerTeleportEvent) {
        // 게임이 진행 중일 때만 작동
        if (GameManager.state == GameState.WAITING || GameManager.state == GameState.ENDED) return

        // 엔드 게이트웨이를 통한 텔레포트(엔더 시티 이동)를 감지
        if (event.cause == PlayerTeleportEvent.TeleportCause.END_GATEWAY) {
            event.isCancelled = true
            event.player.sendMessage("§c[꼬리잡기] §f게임 중에는 엔더 시티로 이동할 수 없습니다.")
        }
    }
}