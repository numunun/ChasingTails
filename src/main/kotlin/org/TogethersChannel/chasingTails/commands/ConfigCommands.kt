package org.TogethersChannel.chasingTails.commands

import org.TogethersChannel.chasingTails.ChasingTails
import org.TogethersChannel.chasingTails.game.ConfigManager
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin

class ConfigCommands : CommandExecutor {

    private val plugin = JavaPlugin.getPlugin(ChasingTails::class.java)

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.isOp) {
            sender.sendMessage("§c명령어를 사용할 권한이 없습니다.")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("§e/tailconfig <reload|set>")
            return true
        }

        when (args[0].lowercase()) {
            "reload" -> {
                ConfigManager.loadConfig()
                sender.sendMessage("§a[꼬리잡기] §f설정 파일을 다시 불러왔습니다.")
            }
            "set" -> {
                if (args.size < 3) {
                    sender.sendMessage("§e/tailconfig set <key> <value>")
                    sender.sendMessage("§7Keys: keep-inventory, spawn-range, world-border, anonymous-names")
                    return true
                }
                val key = args[1].lowercase()
                val value = args[2]

                // ▼▼▼ [수정된 부분] ▼▼▼
                when (key) {
                    "keep-inventory" -> {
                        val booleanValue = value.toBoolean()
                        setConfigAndReload("keep-inventory-on-normal-death", booleanValue)
                        sender.sendMessage("§a[꼬리잡기] §f일반 사망 시 인벤토리 세이브를 §e${booleanValue}§f(으)로 설정했습니다.")
                    }
                    "spawn-range" -> {
                        val intValue = value.toIntOrNull()
                        if (intValue == null) {
                            sender.sendMessage("§c숫자 값을 입력해주세요.")
                            return true
                        }
                        setConfigAndReload("random-spawn-range", intValue)
                        sender.sendMessage("§a[꼬리잡기] §f랜덤 스폰 범위를 §e${intValue}§f(으)로 설정했습니다.")
                    }
                    "world-border" -> {
                        val doubleValue = value.toDoubleOrNull()
                        if (doubleValue == null) {
                            sender.sendMessage("§c숫자 값을 입력해주세요.")
                            return true
                        }
                        setConfigAndReload("world-border-size", doubleValue)
                        sender.sendMessage("§a[꼬리잡기] §f월드보더 크기를 §e${doubleValue}§f(으)로 설정했습니다.")
                    }
                    "anonymous-names" -> {
                        val booleanValue = value.toBoolean()
                        setConfigAndReload("use-anonymous-names", booleanValue)
                        sender.sendMessage("§a[꼬리잡기] §f익명 이름 사용을 §e${booleanValue}§f(으)로 설정했습니다.")
                    }
                    else -> {
                        sender.sendMessage("§c알 수 없는 설정 키입니다: $key")
                    }
                }
            }
            else -> {
                sender.sendMessage("§c알 수 없는 명령어입니다. /tailconfig <reload|set>")
            }
        }
        return true
    }

    private fun setConfigAndReload(key: String, value: Any) {
        plugin.config.set(key, value)
        plugin.saveConfig()
        ConfigManager.loadConfig()
    }
}