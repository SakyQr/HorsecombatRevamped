package org.SakyQ.horsecombatRevampedAgain.Commands

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class HorseCombatTabCompleter : TabCompleter {
    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String>? {
        if (args.size == 1) {
            val subcommands = mutableListOf<String>()

            // Add subcommands based on permissions
            subcommands.add("checkregion") // Available to all players

            if (sender.hasPermission("horsecombat.reload")) {
                subcommands.add("reload")
            }

            if (sender.hasPermission("horsecombat.debug")) {
                subcommands.add("debug")
            }

            if (sender.hasPermission("horsecombat.admin")) {
                subcommands.add("spawnhorse")
            }

            // Filter by what the player has already typed
            val partialCommand = args[0].lowercase()
            return subcommands.filter { it.startsWith(partialCommand) }
        }

        // No suggestions for additional arguments
        return emptyList()
    }
}