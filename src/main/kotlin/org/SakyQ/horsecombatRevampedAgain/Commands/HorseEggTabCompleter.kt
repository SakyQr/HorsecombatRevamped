package org.SakyQ.horsecombatRevampedAgain.Commands

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class HorseEggTabCompleter : TabCompleter {
    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String>? {
        // If the sender doesn't have permission, return empty list
        if (!sender.hasPermission("horsecombat.horseegg")) {
            return emptyList()
        }

        // First argument should be horse types
        if (args.size == 1) {
            val partialType = args[0].lowercase()
            val horseTypes = listOf(
                "white_horse",
                "black_horse",
                "brown_horse",
                "chestnut_horse",
                "creamy_horse",
                "dark_brown_horse",
                "gray_horse",
                "mountain_horse",
                "desert_horse",
                "forest_horse",
                "arctic_horse",
                "spotted_horse",
                "black_dots_horse",
                "white_dots_horse",
                "black_stripe_horse",
                "white_stripe_horse"
            )

            return horseTypes.filter { it.startsWith(partialType) }
        }

        // No suggestions for additional arguments
        return emptyList()
    }
}