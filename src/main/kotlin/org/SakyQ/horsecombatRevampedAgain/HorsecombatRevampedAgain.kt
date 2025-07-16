package org.SakyQ.horsecombatRevampedAgain

import org.SakyQ.horsecombatRevampedAgain.integration.TownyManager
import org.SakyQ.horsecombatRevampedAgain.listeners.HorseCombatListener
import org.SakyQ.horsecombatRevampedAgain.managers.*
import org.SakyQ.horsecombatRevampedAgain.utils.MomentumUtils
import org.bukkit.Location
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin

/**
 * Simplified main plugin class
 * - Reduced initialization complexity
 * - Cleaner error handling
 * - Simplified manager structure
 */
class HorsecombatRevampedAgain : JavaPlugin(), Listener {

    // Core systems only
    lateinit var townyManager: TownyManager
    lateinit var worldGuardManager: WorldGuardManager
    lateinit var horseCombatListener: HorseCombatListener
    lateinit var particleManager: ParticleManager
    lateinit var commandManager: CommandManager
    lateinit var recipeManager: RecipeManager

    private var debugMode = false

    override fun onEnable() {
        try {
            logger.info("Starting HorseCombat v${description.version}...")

            // Initialize core systems
            saveDefaultConfig()
            debugMode = config.getBoolean("debug", false)

            // Initialize momentum system
            MomentumUtils.initialize(this)

            // Initialize managers (simplified order)
            initializeManagers()

            // Register events
            server.pluginManager.registerEvents(this, this)
            server.pluginManager.registerEvents(horseCombatListener, this)

            // Initialize systems
            initializeSystems()

            logger.info("HorseCombat enabled successfully!")

        } catch (e: Exception) {
            logger.severe("Failed to enable HorseCombat: ${e.message}")
            server.pluginManager.disablePlugin(this)
        }
    }

    override fun onDisable() {
        try {
            logger.info("Disabling HorseCombat...")

            // Cleanup in simple order
            if (::horseCombatListener.isInitialized) horseCombatListener.cleanup()
            MomentumUtils.cleanup()
            server.scheduler.cancelTasks(this)

            logger.info("HorseCombat disabled successfully")
        } catch (e: Exception) {
            logger.warning("Error during disable: ${e.message}")
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        MomentumUtils.cleanupPlayer(event.player)
        if (::horseCombatListener.isInitialized) {
            horseCombatListener.cleanupPlayer(event.player)
        }
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        return if (command.name.equals("horsecombat", ignoreCase = true) && ::commandManager.isInitialized) {
            commandManager.handleHorseCombatCommand(sender, args)
        } else false
    }

    // === PRIVATE INITIALIZATION METHODS ===
    private fun initializeManagers() {
        townyManager = TownyManager(this)
        worldGuardManager = WorldGuardManager(this)
        particleManager = ParticleManager(this)
        horseCombatListener = HorseCombatListener(this)
        commandManager = CommandManager(this)
        recipeManager = RecipeManager(this)
    }

    private fun initializeSystems() {
        townyManager.initialize()
        worldGuardManager.initialize()
        particleManager.loadParticleEffects()
        commandManager.registerCommands()
        recipeManager.registerRecipes()
    }

    // === PUBLIC API (Simplified) ===
    fun reloadPlugin() {
        try {
            reloadConfig()
            debugMode = config.getBoolean("debug", false)
            townyManager.initialize()
            worldGuardManager.initialize()
            particleManager.loadParticleEffects()
            logger.info("Configuration reloaded")
        } catch (e: Exception) {
            logger.severe("Error reloading: ${e.message}")
        }
    }

    fun isDebugEnabled(): Boolean = debugMode

    fun toggleDebugMode() {
        debugMode = !debugMode
        config.set("debug", debugMode)
        saveConfig()
        logger.info("Debug mode: ${if (debugMode) "ON" else "OFF"}")
    }

    // === INTEGRATION HELPERS ===
    fun shouldRespectTowny(): Boolean = ::townyManager.isInitialized && townyManager.shouldRespectTowny()

    fun shouldRespectWorldGuard(): Boolean = ::worldGuardManager.isInitialized && worldGuardManager.isWorldGuardEnabled()

    fun isCombatAllowedAtLocation(loc: Location): Boolean {
        return if (::worldGuardManager.isInitialized) {
            worldGuardManager.isCombatAllowed(loc)
        } else true
    }
}