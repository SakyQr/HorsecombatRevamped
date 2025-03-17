package org.SakyQ.horsecombatRevampedAgain.listeners

import org.SakyQ.horsecombatRevampedAgain.HorsecombatRevampedAgain
import org.SakyQ.horsecombatRevampedAgain.utils.MomentumUtils
import org.bukkit.entity.Horse
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDismountEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import java.util.*

class HorseDismountListener(private val plugin: HorsecombatRevampedAgain) : Listener {

    // Store original XP values before mounting
    private val originalPlayerXp = HashMap<UUID, Float>()
    private val originalPlayerLevels = HashMap<UUID, Int>()

    // Triggered when a player dismounts
    @EventHandler
    fun onEntityDismount(event: EntityDismountEvent) {
        // Check if the entity that was dismounted is a horse
        if (event.dismounted is Horse && event.entity is org.bukkit.entity.Player) {
            val player = event.entity as org.bukkit.entity.Player

            // Reset momentum
            MomentumUtils.resetMomentum(player)

            // Reset XP bar to original values or zero if not stored
            val originalExp = originalPlayerXp.getOrDefault(player.uniqueId, 0f)
            val originalLevel = originalPlayerLevels.getOrDefault(player.uniqueId, 0)

            player.exp = originalExp
            player.level = originalLevel

            // Clear stored values
            originalPlayerXp.remove(player.uniqueId)
            originalPlayerLevels.remove(player.uniqueId)

            // Debug message if enabled
            if (plugin.config.getBoolean("debug", false)) {
                plugin.logger.info("[HorseCombat] Reset XP for ${player.name} after dismount")
            }
        }
    }

    // Store original XP values when a player mounts
    fun storeOriginalXp(player: org.bukkit.entity.Player) {
        originalPlayerXp[player.uniqueId] = player.exp
        originalPlayerLevels[player.uniqueId] = player.level

        // Debug message if enabled
        if (plugin.config.getBoolean("debug", false)) {
            plugin.logger.info("[HorseCombat] Stored original XP for ${player.name}: ${player.level} levels, ${player.exp} exp")
        }
    }
}