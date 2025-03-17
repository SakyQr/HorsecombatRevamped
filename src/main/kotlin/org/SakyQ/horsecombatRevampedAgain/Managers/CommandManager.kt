package org.SakyQ.horsecombatRevampedAgain.managers

import org.SakyQ.horsecombatRevampedAgain.Commands.GiveLanceCommand
import org.SakyQ.horsecombatRevampedAgain.Commands.GiveLanceTabCompleter
import org.SakyQ.horsecombatRevampedAgain.Commands.HorseCombatTabCompleter
import org.SakyQ.horsecombatRevampedAgain.HorsecombatRevampedAgain
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.Location

class CommandManager(private val plugin: HorsecombatRevampedAgain) {

    // Reference to other managers
    private val townyManager: TownyManager by lazy {
        plugin.server.pluginManager.getPlugin("HorsecombatRevampedAgain")
            .let { it as HorsecombatRevampedAgain }
            .let { TownyManager(it) }
    }

    private val listenerManager: ListenerManager by lazy {
        plugin.server.pluginManager.getPlugin("HorsecombatRevampedAgain")
            .let { it as HorsecombatRevampedAgain }
            .let { ListenerManager(it) }
    }

    fun registerCommands() {
        val giveLanceCommand = GiveLanceCommand(plugin)
        val giveLanceTabCompleter = GiveLanceTabCompleter()

        plugin.getCommand("givelance")?.apply {
            setExecutor(giveLanceCommand)
            tabCompleter = giveLanceTabCompleter
        }

        plugin.getCommand("horsecombat")?.apply {
            setExecutor(plugin)
            tabCompleter = HorseCombatTabCompleter()
        }
    }

    fun handleHorseCombatCommand(sender: CommandSender, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            displayHelp(sender)
            return true
        }

        when (args[0].lowercase()) {
            "reload" -> handleReloadCommand(sender)
            "debug" -> handleDebugCommand(sender)
            "spawnhorse" -> handleSpawnHorseCommand(sender)
            "checkregion" -> handleCheckRegionCommand(sender)
            "listregs" -> handleListRegionsCommand(sender)
            else -> {
                sender.sendMessage("§c[HorseCombat] Unknown command. Use /horsecombat for help.")
            }
        }

        return true
    }

    private fun displayHelp(sender: CommandSender) {
        sender.sendMessage("§6[HorseCombat] §7Available commands:")
        sender.sendMessage("§6/horsecombat reload §7- Reload the configuration")
        sender.sendMessage("§6/horsecombat debug §7- Toggle debug mode")
        sender.sendMessage("§6/horsecombat spawnhorse §7- Force spawn a horse at your location")
        sender.sendMessage("§6/horsecombat checkregion §7- Check if you're in a horse spawn region")
        sender.sendMessage("§6/horsecombat listregs §7- List all configured horse spawn regions")
    }

    private fun handleReloadCommand(sender: CommandSender) {
        if (sender.hasPermission("horsecombat.reload")) {
            plugin.reloadPlugin()
            sender.sendMessage("§a[HorseCombat] Configuration reloaded!")
        } else {
            sender.sendMessage("§c[HorseCombat] You don't have permission to do that!")
        }
    }

    private fun handleDebugCommand(sender: CommandSender) {
        if (sender is Player && sender.hasPermission("horsecombat.debug")) {
            listenerManager.getHorseSpawnListener().toggleDebug(sender)
        } else {
            sender.sendMessage("§c[HorseCombat] This command can only be used by players with permission!")
        }
    }

    private fun handleSpawnHorseCommand(sender: CommandSender) {
        if (sender is Player && sender.hasPermission("horsecombat.admin")) {
            // Get the player's location
            val loc = sender.location

            // Check if in a custom region
            val region = listenerManager.getHorseSpawnListener().findMatchingRegion(loc)

            if (region != null) {
                sender.sendMessage("§a[HorseCombat] Attempting to spawn a custom horse...")
                // Check Towny integration
                if (listenerManager.getHorseSpawnListener().canSpawnAtLocation(loc, sender)) {
                    listenerManager.getHorseSpawnListener().forceSpawnHorse(loc)
                    sender.sendMessage("§a[HorseCombat] Horse spawned successfully!")
                } else {
                    sender.sendMessage("§c[HorseCombat] Cannot spawn horse at this location due to town protection!")
                }
            } else {
                sender.sendMessage("§c[HorseCombat] You are not in a configured horse spawn region!")
                sender.sendMessage("§c[HorseCombat] Current location: ${loc.world.name} (${loc.blockX}, ${loc.blockZ})")
            }
        } else {
            sender.sendMessage("§c[HorseCombat] You don't have permission to do that!")
        }
    }

    private fun handleCheckRegionCommand(sender: CommandSender) {
        if (sender is Player) {
            // Get the player's location
            val loc = sender.location

            // Display current coordinates
            sender.sendMessage("§6[HorseCombat] Your current location:")
            sender.sendMessage("§7World: ${loc.world.name}")
            sender.sendMessage("§7Coordinates: (${loc.blockX}, ${loc.blockY}, ${loc.blockZ})")

            // Check if in a custom region
            val region = listenerManager.getHorseSpawnListener().findMatchingRegion(loc)

            if (region != null) {
                sender.sendMessage("§a[HorseCombat] You are in a horse spawn region!")
                sender.sendMessage("§7Horse type: ${region.color} with ${region.style} style")
                sender.sendMessage("§7Region bounds: (${region.x1}, ${region.z1}) to (${region.x2}, ${region.z2})")

                // Check Towny status
                if (townyManager.shouldRespectTowny()) {
                    try {
                        val (inTown, townName) = townyManager.getTownAtLocation(loc)
                        if (inTown && townName != null) {
                            sender.sendMessage("§e[HorseCombat] Note: This location is in town: $townName")

                            if (!plugin.config.getBoolean("towny.allowTownHorseSpawns", false)) {
                                sender.sendMessage("§c[HorseCombat] Horses won't naturally spawn here due to town protection")
                            }
                        }
                    } catch (e: Exception) {
                        plugin.logger.warning("Error checking town at location: ${e.message}")
                    }
                }
            } else {
                sender.sendMessage("§c[HorseCombat] You are not in any configured horse spawn region.")
            }
        } else {
            sender.sendMessage("§c[HorseCombat] This command can only be used by players!")
        }
    }

    private fun handleListRegionsCommand(sender: CommandSender) {
        sender.sendMessage("§6[HorseCombat] Configured horse spawn regions:")
        listenerManager.getHorseSpawnListener().listRegions(sender)
    }
}