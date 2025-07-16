package org.SakyQ.horsecombatRevampedAgain.managers

import org.SakyQ.horsecombatRevampedAgain.HorsecombatRevampedAgain
import org.bukkit.Location

class WorldGuardManager(private val plugin: HorsecombatRevampedAgain) {

    private var worldGuardEnabled = false
    private var worldGuardPlugin: Any? = null
    private var worldGuardInstance: Any? = null

    fun initialize() {
        val wgPlugin = plugin.server.pluginManager.getPlugin("WorldGuard")

        if (wgPlugin != null && wgPlugin.isEnabled &&
            plugin.config.getBoolean("worldguard.enabled", true)) {

            worldGuardPlugin = wgPlugin
            worldGuardInstance = getWorldGuardInstance()

            if (worldGuardInstance != null) {
                worldGuardEnabled = true
                plugin.logger.info("WorldGuard integration enabled for HorseCombatRevampedAgain!")
            } else {
                plugin.logger.warning("WorldGuard found but instance not accessible")
                worldGuardEnabled = false
            }
        } else {
            worldGuardEnabled = false
            plugin.logger.info("WorldGuard not found or disabled in config. Running without region protection.")
        }
    }

    private fun getWorldGuardInstance(): Any? {
        return try {
            val worldGuardClass = Class.forName("com.sk89q.worldguard.WorldGuard")
            val getInstance = worldGuardClass.getMethod("getInstance")
            getInstance.invoke(null)
        } catch (e: Exception) {
            if (plugin.isDebugEnabled()) {
                plugin.logger.warning("Failed to get WorldGuard instance: ${e.message}")
            }
            null
        }
    }

    // Check if WorldGuard is enabled
    fun isWorldGuardEnabled(): Boolean {
        return worldGuardEnabled && worldGuardPlugin != null && worldGuardInstance != null &&
                (worldGuardPlugin as? org.bukkit.plugin.Plugin)?.isEnabled == true
    }

    // Check if combat is allowed at the given location
    fun isCombatAllowed(location: Location): Boolean {
        if (!isWorldGuardEnabled()) return true

        return try {
            val allowed = checkRegionFlag(location, "pvp")

            if (plugin.isDebugEnabled()) {
                plugin.logger.info("[WorldGuard] Combat check at ${location.blockX}, ${location.blockY}, ${location.blockZ}: $allowed")
            }

            allowed
        } catch (e: Exception) {
            if (plugin.isDebugEnabled()) {
                plugin.logger.warning("[WorldGuard] Error checking combat permission: ${e.message}")
            }
            true // Default to allow if error
        }
    }

    // Check if horse spawning is allowed at the given location
    fun isHorseSpawningAllowed(location: Location): Boolean {
        if (!isWorldGuardEnabled()) return true

        return try {
            // First check general mob spawning
            val mobSpawnAllowed = checkRegionFlag(location, "mob-spawning")

            // Then check if there's a custom horse spawning flag
            val horseSpawnAllowed = checkRegionFlag(location, "horse-spawning")

            val allowed = mobSpawnAllowed && horseSpawnAllowed

            if (plugin.isDebugEnabled()) {
                plugin.logger.info("[WorldGuard] Horse spawning check: mob-spawning=$mobSpawnAllowed, horse-spawning=$horseSpawnAllowed, result=$allowed")
            }

            allowed
        } catch (e: Exception) {
            if (plugin.isDebugEnabled()) {
                plugin.logger.warning("[WorldGuard] Error checking horse spawning permission: ${e.message}")
            }
            true // Default to allow if error
        }
    }

    private fun checkRegionFlag(location: Location, flagName: String): Boolean {
        if (!isWorldGuardEnabled()) return true

        return try {
            val wg = worldGuardInstance ?: return true

            // Get platform
            val platform = wg.javaClass.getMethod("getPlatform").invoke(wg)

            // Get region container
            val regionContainer = platform.javaClass.getMethod("getRegionContainer").invoke(platform)

            // Create query
            val query = regionContainer.javaClass.getMethod("createQuery").invoke(regionContainer)

            // Adapt the world
            val bukkitAdapterClass = Class.forName("com.sk89q.worldguard.bukkit.BukkitAdapter")
            val adaptWorldMethod = bukkitAdapterClass.getMethod("adapt", org.bukkit.World::class.java)
            val wgWorld = adaptWorldMethod.invoke(null, location.world)

            // Create BlockVector3
            val blockVector3Class = Class.forName("com.sk89q.worldedit.math.BlockVector3")
            val atMethod = blockVector3Class.getMethod("at", Int::class.java, Int::class.java, Int::class.java)
            val vector = atMethod.invoke(null, location.blockX, location.blockY, location.blockZ)

            // Get the flag
            val flagClass = when (flagName) {
                "pvp" -> {
                    val flagsClass = Class.forName("com.sk89q.worldguard.protection.flags.Flags")
                    val pvpField = flagsClass.getDeclaredField("PVP")
                    pvpField.get(null)
                }
                "mob-spawning" -> {
                    val flagsClass = Class.forName("com.sk89q.worldguard.protection.flags.Flags")
                    val mobSpawnField = flagsClass.getDeclaredField("MOB_SPAWNING")
                    mobSpawnField.get(null)
                }
                "horse-spawning" -> {
                    // Custom flag - may not exist
                    getCustomFlag("horse-spawning") ?: return true
                }
                else -> return true
            }

            // Test the flag
            val queryStateMethod = query.javaClass.getMethod("queryState",
                Class.forName("com.sk89q.worldedit.util.Location"),
                java.lang.Class.forName("com.sk89q.worldguard.protection.flags.Flag"))

            // Create WorldEdit Location
            val weLocationClass = Class.forName("com.sk89q.worldedit.util.Location")
            val weLocationConstructor = weLocationClass.getConstructor(
                Class.forName("com.sk89q.worldedit.world.World"),
                Class.forName("com.sk89q.worldedit.math.BlockVector3")
            )
            val weLocation = weLocationConstructor.newInstance(wgWorld, vector)

            val result = queryStateMethod.invoke(query, weLocation, flagClass)

            // Handle StateFlag results
            if (result != null) {
                val stateClass = Class.forName("com.sk89q.worldguard.protection.flags.StateFlag\$State")
                val allowField = stateClass.getField("ALLOW")
                val allowState = allowField.get(null)

                return result == allowState
            }

            true // Default to allow if no explicit deny

        } catch (e: Exception) {
            if (plugin.isDebugEnabled()) {
                plugin.logger.warning("[WorldGuard] Error checking flag '$flagName': ${e.message}")
            }
            true // Default to allow if error
        }
    }

    private fun getCustomFlag(flagName: String): Any? {
        return try {
            val flagRegistryClass = Class.forName("com.sk89q.worldguard.protection.flags.registry.FlagRegistry")
            val wg = worldGuardInstance ?: return null
            val platform = wg.javaClass.getMethod("getPlatform").invoke(wg)
            val flagRegistry = platform.javaClass.getMethod("getFlagRegistry").invoke(platform)

            val getMethod = flagRegistryClass.getMethod("get", String::class.java)
            getMethod.invoke(flagRegistry, flagName)
        } catch (e: Exception) {
            if (plugin.isDebugEnabled()) {
                plugin.logger.warning("[WorldGuard] Custom flag '$flagName' not found: ${e.message}")
            }
            null
        }
    }

    // Register a custom WorldGuard flag for horse spawning
    fun registerCustomFlags() {
        if (!isWorldGuardEnabled()) return

        try {
            val wg = worldGuardInstance ?: return
            val platform = wg.javaClass.getMethod("getPlatform").invoke(wg)
            val flagRegistry = platform.javaClass.getMethod("getFlagRegistry").invoke(platform)

            // Check if horse-spawning flag already exists
            val existingFlag = getCustomFlag("horse-spawning")
            if (existingFlag != null) {
                if (plugin.isDebugEnabled()) {
                    plugin.logger.info("[WorldGuard] Horse spawning flag already exists")
                }
                return
            }

            // Create new StateFlag for horse spawning
            val stateFlagClass = Class.forName("com.sk89q.worldguard.protection.flags.StateFlag")
            val constructor = stateFlagClass.getConstructor(String::class.java, Boolean::class.java)
            val horseSpawnFlag = constructor.newInstance("horse-spawning", true) // true = default allow

            // Register the flag
            val registerMethod = flagRegistry.javaClass.getMethod("register",
                Class.forName("com.sk89q.worldguard.protection.flags.Flag"))
            registerMethod.invoke(flagRegistry, horseSpawnFlag)

            if (plugin.isDebugEnabled()) {
                plugin.logger.info("[WorldGuard] Registered custom horse-spawning flag")
            }

        } catch (e: Exception) {
            if (plugin.isDebugEnabled()) {
                plugin.logger.warning("[WorldGuard] Failed to register custom flags: ${e.message}")
            }
        }
    }

    // Alternative simpler method that just checks if we're in a protected region
    fun isInProtectedRegion(location: Location): Boolean {
        if (!isWorldGuardEnabled()) return false

        return try {
            val wg = worldGuardInstance ?: return false
            val platform = wg.javaClass.getMethod("getPlatform").invoke(wg)
            val regionContainer = platform.javaClass.getMethod("getRegionContainer").invoke(platform)

            // Adapt the world
            val bukkitAdapterClass = Class.forName("com.sk89q.worldguard.bukkit.BukkitAdapter")
            val adaptWorldMethod = bukkitAdapterClass.getMethod("adapt", org.bukkit.World::class.java)
            val wgWorld = adaptWorldMethod.invoke(null, location.world)

            // Get region manager
            val regionManagerMethod = regionContainer.javaClass.getMethod("get",
                Class.forName("com.sk89q.worldedit.world.World"))
            val regionManager = regionManagerMethod.invoke(regionContainer, wgWorld) ?: return false

            // Create BlockVector3
            val blockVector3Class = Class.forName("com.sk89q.worldedit.math.BlockVector3")
            val atMethod = blockVector3Class.getMethod("at", Int::class.java, Int::class.java, Int::class.java)
            val vector = atMethod.invoke(null, location.blockX, location.blockY, location.blockZ)

            // Get applicable regions
            val getApplicableRegionsMethod = regionManager.javaClass.getMethod("getApplicableRegions",
                Class.forName("com.sk89q.worldedit.math.BlockVector3"))
            val applicableRegions = getApplicableRegionsMethod.invoke(regionManager, vector)

            // Check if there are any regions
            val sizeMethod = applicableRegions.javaClass.getMethod("size")
            val regionCount = sizeMethod.invoke(applicableRegions) as Int

            regionCount > 0

        } catch (e: Exception) {
            if (plugin.isDebugEnabled()) {
                plugin.logger.warning("[WorldGuard] Error checking protected region: ${e.message}")
            }
            false
        }
    }
}