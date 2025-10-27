package org.TogethersChannel.chasingTails.listeners

import org.TogethersChannel.chasingTails.game.GameManager
import org.TogethersChannel.chasingTails.game.GameState
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

/* class ConnectionListener : Listener {

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        if (GameManager.state == GameState.RUNNING) {
            GameManager.handlePlayerQuit(event.player)
        }
    }
} */ // 1.5 업데이트로 삭제됨