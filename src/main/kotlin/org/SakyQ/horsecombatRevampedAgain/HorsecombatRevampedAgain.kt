package org.SakyQ.horsecombatRevampedAgain

import org.SakyQ.horsecombatRevampedAgain.listeners.HorseCombatListener
import org.SakyQ.horsecombatRevampedAgain.managers.CommandManager
import org.SakyQ.horsecombatRevampedAgain.managers.ListenerManager
import org.SakyQ.horsecombatRevampedAgain.managers.PlaceholderManager
import org.SakyQ.horsecombatRevampedAgain.managers.TownyManager
import org.bukkit.Location
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin

class HorsecombatRevampedAgain : JavaPlugin() {

    // Managers - making these public so they can be accessed from listeners
    lateinit var listenerManager: ListenerManager
        private set
    lateinit var placeholderManager: PlaceholderManager
        private set
    lateinit var townyManager: TownyManager
        private set
    lateinit var commandManager: CommandManager
        private set
    lateinit var horseCombatListener: HorseCombatListener
        private set

    // Debug flag for more verbose logging
    private var debugMode = false

    override fun onEnable() {
        // Save default config if it doesn't exist
        saveDefaultConfig()

        // Initialize debug mode
        debugMode = config.getBoolean("debug", false)
        if (debugMode) {
            logger.info("Debug mode enabled!")
        }

        // Initialize managers - order matters here
        townyManager = TownyManager(this)
        listenerManager = ListenerManager(this)
        placeholderManager = PlaceholderManager(this, listenerManager.getHorseSpawnListener())
        commandManager = CommandManager(this)

        // Initialize legacy listener (for backward compatibility)
        horseCombatListener = HorseCombatListener(this)
        server.pluginManager.registerEvents(horseCombatListener, this)

        // Initialize systems
        townyManager.initialize()
        listenerManager.registerAllListeners()
        placeholderManager.setupPlaceholders()
        commandManager.registerCommands()


        logger.info("HorseCombatRevampedAgain enabled successfully!")
    }

    override fun onDisable() {
        logger.info("HorseCombatRevampedAgain disabled")
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (command.name.equals("horsecombat", ignoreCase = true)) {
            return commandManager.handleHorseCombatCommand(sender, args)
        }
        return false
    }

    // Method to reload the plugin
    fun reloadPlugin() {
        reloadConfig()
        debugMode = config.getBoolean("debug", false)
        townyManager.initialize()
        listenerManager.getHorseSpawnListener().loadRegionsFromConfig()
        logger.info("HorseCombatRevampedAgain configuration reloaded")
    }

    // These methods are kept for backward compatibility with existing listeners
    fun shouldRespectTowny(): Boolean = townyManager.shouldRespectTowny()
    fun getTownyAPI(): Any? = townyManager.getTownyAPI()
    fun getTownAtLocation(loc: Location): Pair<Boolean, String?> = townyManager.getTownAtLocation(loc)

    // Method to check debug status
    fun isDebugEnabled(): Boolean {
        return debugMode
    }

    // Toggle debug mode
    fun toggleDebugMode() {
        debugMode = !debugMode
        config.set("debug", debugMode)
        saveConfig()
    }
}