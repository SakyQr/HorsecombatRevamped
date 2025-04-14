package org.SakyQ.horsecombatRevampedAgain.managers

import org.SakyQ.horsecombatRevampedAgain.HorsecombatRevampedAgain
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

class TownyManager(private val plugin: HorsecombatRevampedAgain) {

    private var townyEnabled = false
    private var townyPlugin: Plugin? = null

    fun initialize() {
        // Check for Towny integration
        if (plugin.server.pluginManager.getPlugin("Towny") != null &&
            plugin.config.getBoolean("towny.enabled", true)) {
            townyEnabled = true
            townyPlugin = plugin.server.pluginManager.getPlugin("Towny")
            plugin.logger.info("Towny integration enabled for HorseCombatRevampedAgain!")
        } else {
            townyEnabled = false
            townyPlugin = null
            plugin.logger.info("Towny not found or disabled in config. Running without town protection.")
        }
    }

    // Helper method to check if we should respect Towny
    fun shouldRespectTowny(): Boolean {
        return townyEnabled && townyPlugin != null && townyPlugin!!.isEnabled
    }

    // Helper method to safely get the TownyAPI
    fun getTownyAPI(): Any? {
        if (!shouldRespectTowny()) return null

        try {
            // Using reflection to avoid hard dependency
            val townyAPIClass = Class.forName("com.palmergames.bukkit.towny.TownyAPI")
            val getInstance = townyAPIClass.getMethod("getInstance")
            return getInstance.invoke(null)
        } catch (e: Exception) {
            plugin.logger.warning("Failed to get TownyAPI: ${e.message}")
            return null
        }
    }

    // Method to check town at location
    fun getTownAtLocation(loc: Location): Pair<Boolean, String?> {
        if (!shouldRespectTowny()) return Pair(false, null)

        try {
            val api = getTownyAPI() ?: return Pair(false, null)
            val apiClass = api.javaClass

            // Get the getTown method that takes a Location parameter
            val getTownMethod = apiClass.getMethod("getTownBlock", Location::class.java)
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
            plugin.logger.warning("Error checking town at location: ${e.message}")
            return Pair(false, null)
        }
    }

    // New method to check if a player has town bypass permission
    fun shouldBypassTowny(player: Player): Boolean {
        // If Towny isn't enabled at all, no need to check bypass
        if (!shouldRespectTowny()) return true

        // If the player has the bypass permission and the bypass command has been used
        val townyBypassCommand = plugin.commandManager.getTownyBypassCommand()
        return player.hasPermission("horsecombat.admin.townybypass") &&
                townyBypassCommand.hasBypass(player)
    }
}