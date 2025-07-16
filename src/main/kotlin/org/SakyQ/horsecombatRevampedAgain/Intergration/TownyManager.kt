package org.SakyQ.horsecombatRevampedAgain.integration

import org.SakyQ.horsecombatRevampedAgain.HorsecombatRevampedAgain
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

class TownyManager(private val plugin: HorsecombatRevampedAgain) {

    private var townyEnabled = false
    private var townyPlugin: Plugin? = null
    private var townyAPIInstance: Any? = null

    fun initialize() {
        val townyPlugin = plugin.server.pluginManager.getPlugin("Towny")

        if (townyPlugin != null && townyPlugin.isEnabled &&
            plugin.config.getBoolean("towny.enabled", true)) {

            this.townyPlugin = townyPlugin
            this.townyAPIInstance = getTownyAPIInstance()

            if (this.townyAPIInstance != null) {
                townyEnabled = true
                plugin.logger.info("Towny integration enabled for HorseCombatRevampedAgain!")
            } else {
                plugin.logger.warning("Towny found but API not accessible")
                townyEnabled = false
            }
        } else {
            townyEnabled = false
            plugin.logger.info("Towny not found or disabled in config. Running without town protection.")
        }
    }

    fun shouldRespectTowny(): Boolean {
        return townyEnabled && townyPlugin?.isEnabled == true && townyAPIInstance != null
    }

    private fun getTownyAPIInstance(): Any? {
        return try {
            val townyAPIClass = Class.forName("com.palmergames.bukkit.towny.TownyAPI")
            val getInstance = townyAPIClass.getMethod("getInstance")
            getInstance.invoke(null)
        } catch (e: Exception) {
            plugin.logger.warning("Failed to get TownyAPI: ${e.message}")
            null
        }
    }

    fun getTownyAPI(): Any? = townyAPIInstance

    // Safer method to check town at location
    fun getTownAtLocation(loc: Location): Pair<Boolean, String?> {
        if (!shouldRespectTowny()) return Pair(false, null)

        return try {
            val api = townyAPIInstance ?: return Pair(false, null)

            val townBlock = getTownBlockSafely(api, loc) ?: return Pair(false, null)

            val hasTown = checkHasTown(townBlock)
            if (hasTown) {
                val townName = getTownName(townBlock)
                Pair(true, townName)
            } else {
                Pair(false, null)
            }
        } catch (e: Exception) {
            if (plugin.isDebugEnabled()) {
                plugin.logger.warning("Error checking town at location: ${e.message}")
            }
            Pair(false, null)
        }
    }

    private fun getTownBlockSafely(api: Any, loc: Location): Any? {
        val apiClass = api.javaClass

        // Try different method names for different Towny versions
        val methodNames = listOf("getTownBlock", "getTown")

        for (methodName in methodNames) {
            try {
                val method = apiClass.getMethod(methodName, Location::class.java)
                return method.invoke(api, loc)
            } catch (e: NoSuchMethodException) {
                continue
            } catch (e: Exception) {
                if (plugin.isDebugEnabled()) {
                    plugin.logger.warning("Error calling $methodName: ${e.message}")
                }
                continue
            }
        }

        return null
    }

    private fun checkHasTown(townBlock: Any): Boolean {
        return try {
            val townBlockClass = townBlock.javaClass
            val hasTownMethod = townBlockClass.getMethod("hasTown")
            hasTownMethod.invoke(townBlock) as? Boolean ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun getTownName(townBlock: Any): String? {
        return try {
            val townBlockClass = townBlock.javaClass
            val getTownMethod = townBlockClass.getMethod("getTown")
            val town = getTownMethod.invoke(townBlock)

            val townClass = town.javaClass
            val getNameMethod = townClass.getMethod("getName")
            getNameMethod.invoke(town) as? String
        } catch (e: Exception) {
            null
        }
    }

    // Improved method to check if a player can bypass Towny
    fun shouldBypassTowny(player: Player): Boolean {
        if (!shouldRespectTowny()) return true

        val townyBypassCommand = plugin.commandManager.getTownyBypassCommand()
        return player.hasPermission("horsecombat.admin.townybypass") &&
                townyBypassCommand.hasBypass(player)
    }

    // New method: Check if PvP is allowed in town
    fun isPvPAllowedInTown(location: Location): Boolean {
        if (!shouldRespectTowny()) return true

        return try {
            val api = townyAPIInstance ?: return true
            val townBlock = getTownBlockSafely(api, location) ?: return true

            if (!checkHasTown(townBlock)) return true

            val town = getTownFromTownBlock(townBlock) ?: return true
            checkTownPvP(town)
        } catch (e: Exception) {
            if (plugin.isDebugEnabled()) {
                plugin.logger.warning("Error checking PvP status: ${e.message}")
            }
            true // Default to allow if error
        }
    }

    private fun getTownFromTownBlock(townBlock: Any): Any? {
        return try {
            val getTownMethod = townBlock.javaClass.getMethod("getTown")
            getTownMethod.invoke(townBlock)
        } catch (e: Exception) {
            null
        }
    }

    private fun checkTownPvP(town: Any): Boolean {
        val townClass = town.javaClass

        // Try different PvP method names
        val pvpMethods = listOf("isPVP", "isForcePVP", "getPVP", "hasPVP")

        for (methodName in pvpMethods) {
            try {
                val method = townClass.getMethod(methodName)
                return method.invoke(town) as? Boolean ?: false
            } catch (e: NoSuchMethodException) {
                continue
            } catch (e: Exception) {
                continue
            }
        }

        return false // Default to no PvP if can't determine
    }

    // Check if player is resident of town
    fun isPlayerResidentOfTown(player: Player, location: Location): Boolean {
        if (!shouldRespectTowny()) return false

        return try {
            val api = townyAPIInstance ?: return false
            val townBlock = getTownBlockSafely(api, location) ?: return false

            if (!checkHasTown(townBlock)) return false

            val town = getTownFromTownBlock(townBlock) ?: return false
            checkPlayerResident(town, player)
        } catch (e: Exception) {
            if (plugin.isDebugEnabled()) {
                plugin.logger.warning("Error checking resident status: ${e.message}")
            }
            false
        }
    }

    private fun checkPlayerResident(town: Any, player: Player): Boolean {
        val townClass = town.javaClass

        // Try different resident check methods
        val residentMethods = listOf(
            Pair("hasResident", String::class.java),
            Pair("hasResident", java.util.UUID::class.java),
            Pair("isResident", String::class.java)
        )

        for ((methodName, paramType) in residentMethods) {
            try {
                val method = townClass.getMethod(methodName, paramType)
                val param = if (paramType == String::class.java) {
                    player.name
                } else {
                    player.uniqueId
                }
                return method.invoke(town, param) as? Boolean ?: false
            } catch (e: NoSuchMethodException) {
                continue
            } catch (e: Exception) {
                continue
            }
        }

        return false
    }

    // Check if war is active
    fun isWarTime(): Boolean {
        if (!shouldRespectTowny()) return false

        return try {
            val api = townyAPIInstance ?: return false
            val apiClass = api.javaClass

            val warMethods = listOf("isWarTime", "hasActiveWar", "isWarTimeNow", "isAtWar")

            for (methodName in warMethods) {
                try {
                    val method = apiClass.getMethod(methodName)
                    return method.invoke(api) as? Boolean ?: false
                } catch (e: NoSuchMethodException) {
                    continue
                }
            }

            false
        } catch (e: Exception) {
            if (plugin.isDebugEnabled()) {
                plugin.logger.warning("Error checking war status: ${e.message}")
            }
            false
        }
    }
}