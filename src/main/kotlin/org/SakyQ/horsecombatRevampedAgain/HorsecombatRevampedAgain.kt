package org.SakyQ.horsecombatRevampedAgain

import org.SakyQ.horsecombatRevampedAgain.Commands.GiveLanceCommand
import org.SakyQ.horsecombatRevampedAgain.Commands.GiveLanceTabCompleter
import org.SakyQ.horsecombatRevampedAgain.Commands.HorseCombatTabCompleter
import org.SakyQ.horsecombatRevampedAgain.listeners.HorseCombatListener
import org.SakyQ.horsecombatRevampedAgain.listeners.HorseSpawnListener
import org.SakyQ.horsecombatRevampedAgain.placeholders.HorseCombatPlaceholders
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin


class HorsecombatRevampedAgain : JavaPlugin() {

    private lateinit var horseSpawnListener: HorseSpawnListener
    private lateinit var horseCombatListener: HorseCombatListener

    private var townyEnabled = false
    private var townyPlugin: Plugin? = null

    override fun onEnable() {
        // Save default config if it doesn't exist
        saveDefaultConfig()


        // Check for Towny integration
        if (server.pluginManager.getPlugin("Towny") != null && config.getBoolean("towny.enabled", true)) {
            townyEnabled = true
            townyPlugin = server.pluginManager.getPlugin("Towny")
            logger.info("Towny integration enabled for HorseCombatRevampedAgain!")
        } else {
            logger.info("Towny not found or disabled in config. Running without town protection.")
        }

        // Initialize listeners
        horseSpawnListener = HorseSpawnListener(this)
        horseCombatListener = HorseCombatListener(this)

        // Register listeners
        server.pluginManager.registerEvents(horseSpawnListener, this)
        server.pluginManager.registerEvents(horseCombatListener, this)
        setupPlaceholders()

        // Register commands
        val giveLanceCommand = GiveLanceCommand(this)
        val giveLanceTabCompleter = GiveLanceTabCompleter()

        getCommand("givelance")?.apply {
            setExecutor(giveLanceCommand)
            tabCompleter = giveLanceTabCompleter
        }

        getCommand("horsecombat")?.apply {
            setExecutor(this@HorsecombatRevampedAgain)
            tabCompleter = HorseCombatTabCompleter()
        }

        logger.info("HorseCombatRevampedAgain enabled successfully!")
    }



    override fun onDisable() {
        logger.info("HorseCombatRevampedAgain disabled")
    }

    private fun setupPlaceholders() {
        // Check if PlaceholderAPI is present
        if (server.pluginManager.getPlugin("PlaceholderAPI") != null) {
            logger.info("PlaceholderAPI found, registering placeholders...")
            val placeholders = HorseCombatPlaceholders(this, horseSpawnListener)
            if (placeholders.register()) {
                logger.info("HorseCombat placeholders registered successfully!")
            } else {
                logger.warning("Failed to register HorseCombat placeholders!")
            }
        } else {
            logger.info("PlaceholderAPI not found, placeholders won't be available")
        }
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (command.name.equals("horsecombat", ignoreCase = true)) {
            if (args.isEmpty()) {
                sender.sendMessage("§6[HorseCombat] §7Available commands:")
                sender.sendMessage("§6/horsecombat reload §7- Reload the configuration")
                sender.sendMessage("§6/horsecombat debug §7- Toggle debug mode")
                sender.sendMessage("§6/horsecombat spawnhorse §7- Force spawn a horse at your location")
                sender.sendMessage("§6/horsecombat checkregion §7- Check if you're in a horse spawn region")
                sender.sendMessage("§6/horsecombat listregs §7- List all configured horse spawn regions")
                return true
            }

            when (args[0].lowercase()) {
                "reload" -> {
                    if (sender.hasPermission("horsecombat.reload")) {
                        reloadPlugin()
                        sender.sendMessage("§a[HorseCombat] Configuration reloaded!")
                    } else {
                        sender.sendMessage("§c[HorseCombat] You don't have permission to do that!")
                    }
                    return true
                }
                "debug" -> {
                    if (sender is Player && sender.hasPermission("horsecombat.debug")) {
                        horseSpawnListener.toggleDebug(sender)
                    } else {
                        sender.sendMessage("§c[HorseCombat] This command can only be used by players with permission!")
                    }
                    return true
                }
                "spawnhorse" -> {
                    if (sender is Player && sender.hasPermission("horsecombat.admin")) {
                        // Get the player's location
                        val loc = sender.location

                        // Check if in a custom region
                        val region = horseSpawnListener.findMatchingRegion(loc)

                        if (region != null) {
                            sender.sendMessage("§a[HorseCombat] Attempting to spawn a custom horse...")
                            // Check Towny integration
                            if (horseSpawnListener.canSpawnAtLocation(loc, sender)) {
                                horseSpawnListener.forceSpawnHorse(loc)
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
                    return true
                }
                "checkregion" -> {
                    if (sender is Player) {
                        // Get the player's location
                        val loc = sender.location

                        // Display current coordinates
                        sender.sendMessage("§6[HorseCombat] Your current location:")
                        sender.sendMessage("§7World: ${loc.world.name}")
                        sender.sendMessage("§7Coordinates: (${loc.blockX}, ${loc.blockY}, ${loc.blockZ})")

                        // Check if in a custom region
                        val region = horseSpawnListener.findMatchingRegion(loc)

                        if (region != null) {
                            sender.sendMessage("§a[HorseCombat] You are in a horse spawn region!")
                            sender.sendMessage("§7Horse type: ${region.color} with ${region.style} style")
                            sender.sendMessage("§7Region bounds: (${region.x1}, ${region.z1}) to (${region.x2}, ${region.z2})")

                            // Check Towny status
// Inside the checkregion command block:
// Check Towny status
                            if (shouldRespectTowny()) {
                                try {
                                    val (inTown, townName) = getTownAtLocation(loc)
                                    if (inTown && townName != null) {
                                        sender.sendMessage("§e[HorseCombat] Note: This location is in town: $townName")

                                        if (!config.getBoolean("towny.allowTownHorseSpawns", false)) {
                                            sender.sendMessage("§c[HorseCombat] Horses won't naturally spawn here due to town protection")
                                        }
                                    }
                                } catch (e: Exception) {
                                    logger.warning("Error checking town at location: ${e.message}")
                                }
                            }
                        } else {
                            sender.sendMessage("§c[HorseCombat] You are not in any configured horse spawn region.")
                        }
                    } else {
                        sender.sendMessage("§c[HorseCombat] This command can only be used by players!")
                    }
                    return true
                }
                "listregs" -> {
                    sender.sendMessage("§6[HorseCombat] Configured horse spawn regions:")
                    horseSpawnListener.listRegions(sender)
                    return true
                }
                else -> {
                    sender.sendMessage("§c[HorseCombat] Unknown command. Use /horsecombat for help.")
                    return true
                }
            }
        }
        return false
    }

    // Reload function
    fun reloadPlugin() {
        reloadConfig()

        // Update Towny integration status
        if (server.pluginManager.getPlugin("Towny") != null && config.getBoolean("towny.enabled", true)) {
            townyEnabled = true
            townyPlugin = server.pluginManager.getPlugin("Towny")
            logger.info("Towny integration enabled!")
        } else {
            townyEnabled = false
            townyPlugin = null
            logger.info("Towny integration disabled.")
        }

        horseSpawnListener.loadRegionsFromConfig()
        logger.info("HorseCombatRevampedAgain configuration reloaded")
    }

    // Helper method to check if we should respect Towny
    fun shouldRespectTowny(): Boolean {
        return townyEnabled && townyPlugin != null && townyPlugin!!.isEnabled
    }

    // Helper method to safely get the TownyAPI
// Helper method to safely get the TownyAPI
    fun getTownyAPI(): Any? {
        if (!shouldRespectTowny()) return null

        try {
            // Using reflection to avoid hard dependency
            val townyAPIClass = Class.forName("com.palmergames.bukkit.towny.TownyAPI")
            val getInstance = townyAPIClass.getMethod("getInstance")
            return getInstance.invoke(null)
        } catch (e: Exception) {
            logger.warning("Failed to get TownyAPI: ${e.message}")
            return null
        }
    }

    // Add this method to check town at location
    fun getTownAtLocation(loc: org.bukkit.Location): Pair<Boolean, String?> {
        if (!shouldRespectTowny()) return Pair(false, null)

        try {
            val api = getTownyAPI() ?: return Pair(false, null)
            val apiClass = api.javaClass

            // Get the getTown method that takes a Location parameter
            val getTownMethod = apiClass.getMethod("getTownBlock", org.bukkit.Location::class.java)
            val townBlock = getTownMethod.invoke(api, loc) ?: return Pair(false, null)

            // Check if the townBlock has a town
            val townBlockClass = townBlock.javaClass
            val hasTownMethod = townBlockClass.getMethod("hasTown")
            val hasTown = hasTownMethod.invoke(townBlock) as Boolean

            if (hasTown) {
                // Get the town
                val getTownMethod = townBlockClass.getMethod("getTown")
                val town = getTownMethod.invoke(townBlock)

                // Get the town name
                val townClass = town.javaClass
                val getNameMethod = townClass.getMethod("getName")
                val townName = getNameMethod.invoke(town) as String

                return Pair(true, townName)
            }

            return Pair(false, null)
        } catch (e: Exception) {
            logger.warning("Error checking town at location: ${e.message}")
            return Pair(false, null)
        }
    }


}