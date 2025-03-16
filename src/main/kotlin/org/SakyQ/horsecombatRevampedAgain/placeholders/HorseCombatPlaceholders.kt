package org.SakyQ.horsecombatRevampedAgain.placeholders

import org.SakyQ.horsecombatRevampedAgain.HorsecombatRevampedAgain
import org.SakyQ.horsecombatRevampedAgain.listeners.HorseSpawnListener
import org.bukkit.entity.Player
import me.clip.placeholderapi.expansion.PlaceholderExpansion

class HorseCombatPlaceholders(
    private val plugin: HorsecombatRevampedAgain,
    private val horseSpawnListener: HorseSpawnListener
) : PlaceholderExpansion() {

    override fun getIdentifier(): String {
        return "horsecombat"
    }

    override fun getAuthor(): String {
        return plugin.description.authors.joinToString()
    }

    override fun getVersion(): String {
        return plugin.description.version
    }

    override fun persist(): Boolean {
        return true
    }

    override fun onPlaceholderRequest(player: Player?, identifier: String): String? {
        if (player == null) return null

        when (identifier) {
            "in_horse_region" -> {
                val region = horseSpawnListener.findMatchingRegion(player.location)
                return if (region != null) "Yes" else "No"
            }
            "horse_region_name" -> {
                val region = horseSpawnListener.findMatchingRegion(player.location)
                return region?.name ?: "None"
            }
            "horse_region_color" -> {
                val region = horseSpawnListener.findMatchingRegion(player.location)
                return region?.color?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "None"
            }
            "horse_region_style" -> {
                val region = horseSpawnListener.findMatchingRegion(player.location)
                return region?.style?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "None"
            }
        }

        return null
    }
}