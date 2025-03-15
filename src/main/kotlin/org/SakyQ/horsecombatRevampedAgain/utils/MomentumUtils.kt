package org.SakyQ.horsecombatRevampedAgain.utils

import org.bukkit.entity.Player
import java.util.*

object MomentumUtils {
    private val momentumMap: MutableMap<UUID, Int> = HashMap()

    fun increaseMomentum(player: Player, amount: Int) {
        val uuid = player.uniqueId
        val currentMomentum = momentumMap.getOrDefault(uuid, 0)
        val newMomentum = (currentMomentum + amount).coerceIn(0, 100)
        momentumMap[uuid] = newMomentum
    }

    fun reduceMomentum(player: Player, amount: Int) {
        val uuid = player.uniqueId
        val currentMomentum = momentumMap.getOrDefault(uuid, 0)
        val newMomentum = (currentMomentum - amount).coerceAtLeast(0)
        momentumMap[uuid] = newMomentum
    }

    fun getMomentum(player: Player): Int {
        return momentumMap.getOrDefault(player.uniqueId, 0)
    }

    fun resetMomentum(player: Player) {
        momentumMap[player.uniqueId] = 0
    }
}