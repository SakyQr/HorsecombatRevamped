package org.SakyQ.horsecombatRevampedAgain.managers

import org.SakyQ.horsecombatRevampedAgain.HorsecombatRevampedAgain
import org.SakyQ.horsecombatRevampedAgain.listeners.HorseCombatListener
import org.SakyQ.horsecombatRevampedAgain.listeners.HorseDismountListener
import org.SakyQ.horsecombatRevampedAgain.listeners.HorseSpawnListener

class ListenerManager(private val plugin: HorsecombatRevampedAgain) {

    private val horseSpawnListener: HorseSpawnListener = HorseSpawnListener(plugin)
    private val horseCombatListener: HorseCombatListener = HorseCombatListener(plugin)
    private val horseDismountListener: HorseDismountListener = HorseDismountListener(plugin)

    fun registerAllListeners() {
        plugin.server.pluginManager.apply {
            registerEvents(horseSpawnListener, plugin)
            registerEvents(horseCombatListener, plugin)
            registerEvents(horseDismountListener, plugin)
        }
    }

    fun getHorseSpawnListener(): HorseSpawnListener = horseSpawnListener
    fun getHorseCombatListener(): HorseCombatListener = horseCombatListener
    fun getHorseDismountListener(): HorseDismountListener = horseDismountListener
}