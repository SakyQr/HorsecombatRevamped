package org.SakyQ.horsecombatRevampedAgain.managers

import org.SakyQ.horsecombatRevampedAgain.HorsecombatRevampedAgain
import org.SakyQ.horsecombatRevampedAgain.listeners.HorseSpawnListener
import org.SakyQ.horsecombatRevampedAgain.placeholders.HorseCombatPlaceholders

class PlaceholderManager(
    private val plugin: HorsecombatRevampedAgain,
    private val horseSpawnListener: HorseSpawnListener
) {

    fun setupPlaceholders() {
        // Check if PlaceholderAPI is present
        if (plugin.server.pluginManager.getPlugin("PlaceholderAPI") != null) {
            plugin.logger.info("PlaceholderAPI found, registering placeholders...")
            val placeholders = HorseCombatPlaceholders(plugin, horseSpawnListener)
            if (placeholders.register()) {
                plugin.logger.info("HorseCombat placeholders registered successfully!")
            } else {
                plugin.logger.warning("Failed to register HorseCombat placeholders!")
            }
        } else {
            plugin.logger.info("PlaceholderAPI not found, placeholders won't be available")
        }
    }
}