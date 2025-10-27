package org.TogethersChannel.chasingTails.commands

import org.TogethersChannel.chasingTails.game.GameManager
import org.TogethersChannel.chasingTails.game.GameState
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class GameCommands : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("이 명령어는 플레이어만 사용할 수 있습니다.")
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage("§e/tail <start|stop|target>")
            return true
        }

        when (args[0].lowercase()) {
            "start", "stop" -> {
                if (!sender.isOp) {
                    sender.sendMessage("§c명령어를 사용할 권한이 없습니다.")
                    return true
                }
                if (args[0].lowercase() == "start") {
                    if (GameManager.state != GameState.WAITING) {
                        sender.sendMessage("§c이미 게임이 진행 중이거나 대기 상태가 아닙니다.")
                        return true
                    }
                    // ▼▼▼ [수정된 부분] 관전 모드 플레이어 제외 ▼▼▼
                    val players = Bukkit.getOnlinePlayers().filter { it.gameMode != GameMode.SPECTATOR }.toList()

                    if (players.size < 2) {
                        sender.sendMessage("§c게임 시작에 필요한 최소 인원은 2명입니다. (관전 모드 제외)")
                        return true
                    }
                    GameManager.startGame(players)
                    // 스폰포인트 설정은 GameManager에서 처리
                } else { // stop
                    if (GameManager.state == GameState.WAITING) {
                        sender.sendMessage("§c시작된 게임이 없습니다.")
                        return true
                    }
                    GameManager.stopGame()
                    Bukkit.broadcastMessage("§a[꼬리잡기] §f관리자에 의해 게임이 강제 종료되었습니다.")
                }
            }
            "target" -> {
                if (GameManager.state == GameState.WAITING || GameManager.state == GameState.ENDED) {
                    sender.sendMessage("§c게임이 진행 중일 때만 사용할 수 있습니다.")
                    return true
                }

                val targetName = GameManager.getTargetDisplayName(sender)
                val hunterName = GameManager.getHunterDisplayName(sender)

                sender.sendMessage("§7===== §e나의 정보 §7=====")
                sender.sendMessage("§c목표물: §f$targetName")
                sender.sendMessage("§9포식자: §f$hunterName")
                sender.sendMessage("§7===================")
            }
            else -> {
                sender.sendMessage("§c알 수 없는 명령어입니다. /tail <start|stop|target>")
            }
        }
        return true
    }
}