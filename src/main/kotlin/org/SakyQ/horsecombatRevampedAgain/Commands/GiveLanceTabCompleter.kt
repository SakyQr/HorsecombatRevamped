package org.SakyQ.horsecombatRevampedAgain.Commands

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class GiveLanceTabCompleter : TabCompleter {
    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String>? {
        // If the sender doesn't have permission, return empty list
        if (!sender.hasPermission("horsecombat.givelance")) {
            return emptyList()
        }

        // First argument should be player names
        if (args.size == 1) {
            val partialName = args[0].lowercase()
            return Bukkit.getOnlinePlayers()
                .filter { it.name.lowercase().startsWith(partialName) }
                .map { it.name }
                .toList()
        }

        // No suggestions for additional arguments
        return emptyList()
    }
}