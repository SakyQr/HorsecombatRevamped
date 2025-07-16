package org.SakyQ.horsecombatRevampedAgain.managers

import org.SakyQ.horsecombatRevampedAgain.HorsecombatRevampedAgain
import org.bukkit.event.HandlerList

class ListenerManager(private val plugin: HorsecombatRevampedAgain) {


    // Track registered listeners for cleanup
    private val registeredListeners = mutableListOf<org.bukkit.event.Listener>()
    private var listenersRegistered = false

    /**
     * Register all plugin listeners with the server
     */
    fun registerAllListeners() {
        if (listenersRegistered) {
            plugin.logger.warning("Listeners already registered! Skipping duplicate registration.")
            return
        }

        try {
            plugin.logger.info("Registering HorseCombat listeners...")

            listenersRegistered = true
            plugin.logger.info("All listeners registered successfully (${registeredListeners.size} total)")

            // Note: HorseCombatListener is registered separately in the main plugin class
            // Note: HorseDismountListener has been removed - functionality merged into HorseCombatListener

        } catch (e: Exception) {
            plugin.logger.severe("Failed to register listeners: ${e.message}")
            e.printStackTrace()

            // Attempt cleanup if registration failed partway through
            unregisterAllListeners()
            throw e
        }
    }

    /**
     * Unregister all listeners (used during plugin disable or reload)
     */
    fun unregisterAllListeners() {
        try {
            if (!listenersRegistered && registeredListeners.isEmpty()) {
                return
            }

            plugin.logger.info("Unregistering HorseCombat listeners...")

            // Unregister each listener individually
            registeredListeners.forEach { listener ->
                try {
                    HandlerList.unregisterAll(listener)
                } catch (e: Exception) {
                    plugin.logger.warning("Error unregistering listener ${listener.javaClass.simpleName}: ${e.message}")
                }
            }

            registeredListeners.clear()
            listenersRegistered = false

            plugin.logger.info("All listeners unregistered successfully")

        } catch (e: Exception) {
            plugin.logger.severe("Error during listener cleanup: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Reload all listeners (useful for configuration changes)
     */
    fun reloadListeners() {
        try {
            plugin.logger.info("Reloading listeners...")

            // Unregister existing listeners
            unregisterAllListeners()

            // Wait a tick for cleanup
            plugin.server.scheduler.runTaskLater(plugin, Runnable {
                try {
                    // Re-register listeners
                    registerAllListeners()

                    plugin.logger.info("Listeners reloaded successfully")
                } catch (e: Exception) {
                    plugin.logger.severe("Error during listener reload: ${e.message}")
                    e.printStackTrace()
                }
            }, 1L)

        } catch (e: Exception) {
            plugin.logger.severe("Error initiating listener reload: ${e.message}")
            e.printStackTrace()
        }
    }


    // === STATUS METHODS ===

    /**
     * Check if listeners are currently registered
     */
    fun areListenersRegistered(): Boolean {
        return listenersRegistered
    }

    /**
     * Get the number of registered listeners
     */
    fun getRegisteredListenerCount(): Int {
        return registeredListeners.size
    }

    /**
     * Get a list of registered listener class names (for debugging)
     */
    fun getRegisteredListenerNames(): List<String> {
        return registeredListeners.map { it.javaClass.simpleName }
    }

    // === UTILITY METHODS ===

    /**
     * Check if a specific listener type is registered
     */
    fun isListenerRegistered(listenerClass: Class<*>): Boolean {
        return registeredListeners.any { it.javaClass == listenerClass }
    }

    /**
     * Get debug information about listener status
     */
    fun getDebugInfo(): Map<String, Any> {
        return mapOf(
            "listenersRegistered" to listenersRegistered,
            "registeredCount" to registeredListeners.size,
            "listeners" to getRegisteredListenerNames()
        )
    }

    /**
     * Cleanup method called during plugin disable
     */
    fun cleanup() {
        try {
            plugin.logger.info("Cleaning up ListenerManager...")

            // Unregister all listeners
            unregisterAllListeners()

            plugin.logger.info("ListenerManager cleanup completed")
        } catch (e: Exception) {
            plugin.logger.severe("Error during ListenerManager cleanup: ${e.message}")
            e.printStackTrace()
        }
    }
}