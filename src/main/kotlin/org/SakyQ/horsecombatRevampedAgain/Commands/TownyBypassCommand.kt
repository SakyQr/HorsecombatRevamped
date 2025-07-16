package org.SakyQ.horsecombatRevampedAgain.Commands

import org.SakyQ.horsecombatRevampedAgain.HorsecombatRevampedAgain
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.*

class TownyBypassCommand(private val plugin: HorsecombatRevampedAgain) : CommandExecutor, TabCompleter {

    private val bypassEnabled = HashSet<UUID>()

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        // Check if sender is a player
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.RED}This command can only be used by players.")
            return true
        }

        // Check for admin permission
        if (!sender.hasPermission("horsecombat.admin.townybypass")) {
            sender.sendMessage("${ChatColor.RED}You don't have permission to use this command.")
            return true
        }

        // If Towny isn't enabled, inform the admin
        if (!plugin.shouldRespectTowny()) {
            sender.sendMessage("${ChatColor.YELLOW}Towny integration is not enabled, so bypass is not necessary.")
            return true
        }

        // Toggle bypass mode
        if (args.isEmpty() || args[0].equals("toggle", ignoreCase = true)) {
            toggleBypass(sender)
            return true
        }

        // Explicitly set bypass mode
        when (args[0].lowercase()) {
            "on" -> {
                bypassEnabled.add(sender.uniqueId)
                sender.sendMessage("${ChatColor.GREEN}Towny bypass has been enabled. You can now use horse combat in towns.")
            }
            "off" -> {
                bypassEnabled.remove(sender.uniqueId)
                sender.sendMessage("${ChatColor.YELLOW}Towny bypass has been disabled. Horse combat will respect town protections.")
            }
            "status" -> {
                val status = if (bypassEnabled.contains(sender.uniqueId)) "enabled" else "disabled"
                sender.sendMessage("${ChatColor.AQUA}Your Towny bypass status is currently $status.")
            }
            else -> {
                sender.sendMessage("${ChatColor.RED}Unknown argument. Use /hcbypass [toggle|on|off|status]")
            }
        }

        return true
    }

    private fun toggleBypass(player: Player) {
        if (bypassEnabled.contains(player.uniqueId)) {
            bypassEnabled.remove(player.uniqueId)
            player.sendMessage("${ChatColor.YELLOW}Towny bypass has been disabled. Horse combat will respect town protections.")
        } else {
            bypassEnabled.add(player.uniqueId)
            player.sendMessage("${ChatColor.GREEN}Towny bypass has been enabled. You can now use horse combat in towns.")
        }
    }

    // Check if a player has bypass enabled
    fun hasBypass(player: Player): Boolean {
        return bypassEnabled.contains(player.uniqueId)
    }

    // Tab completion
    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        if (args.size <= 1) {
            val completions = listOf("toggle", "on", "off", "status")
            return completions.filter { it.startsWith(args[0].lowercase()) }
        }
        return emptyList()
    }
}