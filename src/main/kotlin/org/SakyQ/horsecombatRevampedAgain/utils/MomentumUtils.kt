package org.SakyQ.horsecombatRevampedAgain.utils

import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Ultra-simplified MomentumUtils - no complex decay system needed
 * Simple: moving = gain momentum, stopped = instant reset
 */
object MomentumUtils {

    private lateinit var plugin: JavaPlugin
    private val playerMomentum = ConcurrentHashMap<UUID, Int>()

    fun initialize(plugin: JavaPlugin) {
        this.plugin = plugin
    }

    // === CORE MOMENTUM OPERATIONS ===
    fun getMomentum(player: Player): Int = playerMomentum.getOrDefault(player.uniqueId, 0)

    fun setMomentum(player: Player, amount: Int) {
        playerMomentum[player.uniqueId] = amount.coerceIn(0, 100)
    }

    fun increaseMomentum(player: Player, amount: Int) {
        val current = getMomentum(player)
        setMomentum(player, current + amount)
    }

    fun reduceMomentum(player: Player, amount: Int) {
        val current = getMomentum(player)
        setMomentum(player, current - amount)
    }

    fun resetMomentum(player: Player) {
        playerMomentum[player.uniqueId] = 0
    }

    // === CLEANUP ===
    fun cleanupPlayer(player: Player) {
        playerMomentum.remove(player.uniqueId)
    }

    fun cleanup() {
        playerMomentum.clear()
    }

    // === UTILITY ===
    fun getAllActivePlayers(): Set<UUID> = playerMomentum.keys.toSet()

    fun getDebugInfo(): Map<String, Any> = mapOf(
        "activePlayers" to playerMomentum.size
    )
}