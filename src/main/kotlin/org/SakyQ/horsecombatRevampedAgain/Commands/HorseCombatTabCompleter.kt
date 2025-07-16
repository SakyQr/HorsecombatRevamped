package org.SakyQ.horsecombatRevampedAgain.Commands

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class HorseCombatTabCompleter : TabCompleter {
    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String>? {
        if (args.size == 1) {
            val subcommands = mutableListOf<String>()

            // Always available commands
            subcommands.add("help")

            // Permission-based commands
            if (sender.hasPermission("horsecombat.admin.reload")) {
                subcommands.add("reload")
            }

            if (sender.hasPermission("horsecombat.debug")) {
                subcommands.add("debug")
            }

            if (sender.hasPermission("horsecombat.admin.gui")) {
                subcommands.add("gui")
                subcommands.add("admin")
            }

            // Filter by what the player has already typed
            val partialCommand = args[0].lowercase()
            return subcommands.filter { it.startsWith(partialCommand) }
        }

        return emptyList()
    }
}