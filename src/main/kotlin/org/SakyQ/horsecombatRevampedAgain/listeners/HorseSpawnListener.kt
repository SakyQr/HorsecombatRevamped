package org.SakyQ.horsecombatRevampedAgain.listeners

import org.SakyQ.horsecombatRevampedAgain.HorsecombatRevampedAgain
import org.bukkit.Location
import org.bukkit.attribute.Attribute
import org.bukkit.command.CommandSender
import org.bukkit.entity.EntityType
import org.bukkit.entity.Horse
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.world.ChunkLoadEvent
import java.util.Random
import java.util.UUID
import org.bukkit.Material
import org.bukkit.block.Biome

// Data class to hold region information
data class SpawnRegionInfo(
    val worldName: String,
    val x1: Int, val x2: Int,
    val z1: Int, val z2: Int,
    val color: Horse.Color,
    val style: Horse.Style,
    val spawnChance: Double,
    val speed: Double? = null,
    val jumpStrength: Double? = null,
    val maxHealth: Double? = null,
    val name: String? = null
)

class HorseSpawnListener(private val plugin: HorsecombatRevampedAgain) : Listener {

    private val random = Random()
    private val spawnRegions = mutableListOf<SpawnRegionInfo>()
    private val debugPlayers = mutableSetOf<UUID>()
    private val playersInRegions = mutableMapOf<UUID, SpawnRegionInfo?>()

    init {
        loadRegionsFromConfig()
        plugin.logger.info("[DEBUG] HorseSpawnListener initialized with ${spawnRegions.size} regions")
        printRegions() // Debug print all loaded regions
    }

    // Debug function to print loaded regions
    private fun printRegions() {
        plugin.logger.info("=== LOADED HORSE SPAWN REGIONS ===")
        if (spawnRegions.isEmpty()) {
            plugin.logger.warning("No regions were loaded from config!")
        }

        spawnRegions.forEachIndexed { index, region ->
            plugin.logger.info("Region #${index+1}:")
            plugin.logger.info("  World: ${region.worldName}")
            plugin.logger.info("  Coordinates: (${region.x1},${region.z1}) to (${region.x2},${region.z2})")
            plugin.logger.info("  Horse: ${region.color} color, ${region.style} style")
            plugin.logger.info("  Spawn chance: ${region.spawnChance}")
        }
        plugin.logger.info("================================")
    }

    fun isPlayerInRegion(player: Player): Boolean {
        return playersInRegions[player.uniqueId] != null
    }

    fun getPlayerRegionInfo(player: Player): SpawnRegionInfo? {
        return playersInRegions[player.uniqueId]
    }

    // Load regions from config
    fun loadRegionsFromConfig() {
        spawnRegions.clear()

        // Get the regions section from config
        val regionsSection = plugin.config.getConfigurationSection("horseSpawning.regions")

        if (regionsSection == null) {
            plugin.logger.warning("[DEBUG] Config section 'horseSpawning.regions' not found!")
            return
        }

        // Iterate through each defined region
        for (regionKey in regionsSection.getKeys(false)) {
            val regionSection = regionsSection.getConfigurationSection(regionKey)

            if (regionSection == null) {
                plugin.logger.warning("[DEBUG] Region '$regionKey' section is null!")
                continue
            }

            try {
                // Get world name, defaulting to "world" if not specified
                val worldName = regionSection.getString("world", "world") ?: "world"

                // Get coordinates
                val x1 = regionSection.getInt("x1")
                val x2 = regionSection.getInt("x2")
                val z1 = regionSection.getInt("z1")
                val z2 = regionSection.getInt("z2")

                // Get horse properties
                val colorName = regionSection.getString("color", "BROWN")
                val styleName = regionSection.getString("style", "NONE")

                // Parse horse color and style
                val color = try {
                    Horse.Color.valueOf(colorName?.uppercase() ?: "BROWN")
                } catch (e: IllegalArgumentException) {
                    plugin.logger.warning("Invalid horse color '$colorName' in region $regionKey, using BROWN")
                    Horse.Color.BROWN
                }

                val style = try {
                    Horse.Style.valueOf(styleName?.uppercase() ?: "NONE")
                } catch (e: IllegalArgumentException) {
                    plugin.logger.warning("Invalid horse style '$styleName' in region $regionKey, using NONE")
                    Horse.Style.NONE
                }

                // Get spawn chance
                val spawnChance = regionSection.getDouble("spawnChance", 0.1)

                // Get optional attributes
                val speed = if (regionSection.contains("speed")) regionSection.getDouble("speed") else null
                val jumpStrength = if (regionSection.contains("jumpStrength")) regionSection.getDouble("jumpStrength") else null
                val maxHealth = if (regionSection.contains("maxHealth")) regionSection.getDouble("maxHealth") else null
                val name = if (regionSection.contains("name")) regionSection.getString("name") else null

                // Create and add the region
                spawnRegions.add(
                    SpawnRegionInfo(
                        worldName, x1, x2, z1, z2, color, style, spawnChance,
                        speed, jumpStrength, maxHealth, name
                    )
                )

                plugin.logger.info("Loaded horse spawn region $regionKey: $worldName ($x1,$z1) to ($x2,$z2) with ${color.name} horse")

            } catch (e: Exception) {
                plugin.logger.warning("Failed to load horse spawn region $regionKey: ${e.message}")
                e.printStackTrace() // Print full stack trace for debugging
            }
        }

        plugin.logger.info("Loaded ${spawnRegions.size} horse spawn regions")
    }

    // Find a matching region for a given location
    fun findMatchingRegion(location: Location): SpawnRegionInfo? {
        val worldName = location.world.name
        val x = location.blockX
        val z = location.blockZ

        val matchingRegion = spawnRegions.find { region ->
            region.worldName == worldName &&
                    x >= region.x1 && x <= region.x2 &&
                    z >= region.z1 && z <= region.z2
        }

        return matchingRegion
    }

    // Debug event to track player movement in regions
    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player

        // Only check every few ticks to avoid spam
        if (player.ticksLived % 20 != 0) return

        val location = player.location
        val region = findMatchingRegion(location)

        // Update the player's region status
        playersInRegions[player.uniqueId] = region

        // Only show debug messages if player has debug enabled
        if (region != null) {
            if (debugPlayers.contains(player.uniqueId)) {
                player.sendMessage("§6[Debug] You are in a horse spawn region: ${region.color} horses can spawn here")
            }
        } else {
            if (debugPlayers.contains(player.uniqueId)) {
                player.sendMessage("§6[Debug] You left the horse spawn region")
            }
        }
    }

    // Handle natural horse spawns
    @EventHandler
    fun onCreatureSpawn(event: CreatureSpawnEvent) {
        // Only handle horse entities
        if (event.entity !is Horse) {
            return
        }

        val horse = event.entity as Horse
        val location = horse.location

        plugin.logger.info("[DEBUG] Horse spawn detected at ${location.blockX}, ${location.blockY}, ${location.blockZ} with reason ${event.spawnReason}")

        // Check if the spawn should be controlled by our special regions
        val spawnRegionInfo = findMatchingRegion(location)

        if (spawnRegionInfo != null) {
            plugin.logger.info("[DEBUG] Horse is in a custom region. Applying custom properties...")

            // Only replace natural spawns
            if (event.spawnReason == CreatureSpawnEvent.SpawnReason.NATURAL) {
                // Cancel the natural spawn and replace with our custom horse
                event.isCancelled = true

                // Spawn our custom horse in the same location
                spawnCustomHorse(location, spawnRegionInfo)
            } else {
                // For non-natural spawns, just modify the existing horse
                customizeHorse(horse, spawnRegionInfo)
                plugin.logger.info("[DEBUG] Modified non-natural horse spawn")
            }
        } else {
            plugin.logger.info("[DEBUG] Horse spawn not in any custom region")
        }
    }

    // Handle chunk loading to potentially spawn horses
    @EventHandler
    fun onChunkLoad(event: ChunkLoadEvent) {
        // Check if we should attempt to spawn a horse in this chunk
        if (!plugin.config.getBoolean("horseSpawning.enabledOnChunkLoad", true)) {
            return
        }

        // Get a random location in the chunk
        val chunk = event.chunk
        val world = chunk.world

        // Only do this occasionally to avoid too many spawns
        if (random.nextDouble() > plugin.config.getDouble("horseSpawning.chunkSpawnChance", 0.05)) {
            return
        }

        // Find a random block in the chunk
        val x = (chunk.x * 16) + random.nextInt(16)
        val z = (chunk.z * 16) + random.nextInt(16)

        // Find the highest non-air block at this location
        val y = world.getHighestBlockYAt(x, z)
        val location = Location(world, x.toDouble(), y.toDouble(), z.toDouble())

        // Check if this location is in one of our special regions
        val spawnRegionInfo = findMatchingRegion(location)

        if (spawnRegionInfo != null) {
            plugin.logger.info("[DEBUG] Chunk load in region, attempting to spawn horse at $x, $y, $z")

            // Spawn our custom horse in this location if configured chance is met
            if (random.nextDouble() <= spawnRegionInfo.spawnChance) {
                spawnCustomHorse(location, spawnRegionInfo)
                plugin.logger.info("[DEBUG] Custom horse spawned from chunk load")
            }
        }
    }

    // Helper function to spawn a custom horse
    private fun spawnCustomHorse(location: Location, regionInfo: SpawnRegionInfo) {
        val world = location.world

        try {
            // Create the horse
            val horse = world.spawnEntity(location, EntityType.HORSE) as Horse

            // Apply custom properties
            customizeHorse(horse, regionInfo)

            plugin.logger.info("[DEBUG] Custom horse spawned successfully at ${location.blockX}, ${location.blockY}, ${location.blockZ}")

        } catch (e: Exception) {
            plugin.logger.severe("[DEBUG] Error spawning custom horse: ${e.message}")
            e.printStackTrace()
        }
    }

    // Check if horse spawn should be allowed at this location
    fun canSpawnAtLocation(location: Location, player: Player? = null): Boolean {
        // Check if TownyAPI is available
        if (plugin.shouldRespectTowny()) {
            try {
                // Get TownyAPI safely through the main plugin
                val townyAPI = plugin.getTownyAPI()

                // Use reflection to call getTown method
                val getTownMethod = townyAPI?.javaClass?.getMethod("getTown", Location::class.java)
                val town = getTownMethod?.invoke(townyAPI, location)

                if (town != null) {
                    // Location is in a town
                    plugin.logger.info("[DEBUG] Horse spawn prevented in town: ${town.toString()}")

                    // Check if town spawns are allowed in config
                    if (!plugin.config.getBoolean("towny.allowTownHorseSpawns", false)) {
                        player?.sendMessage("§c[HorseCombat] Horse spawning is not allowed in towns!")
                        return false
                    }
                }
            } catch (e: Exception) {
                plugin.logger.warning("Error checking Towny town: ${e.message}")
            }
        }

        // Get the block at the location and the block below
        val block = location.block
        val blockBelow = block.getRelative(0, -1, 0)

        // Check if location is underground (cave)
        val highestBlock = location.world.getHighestBlockYAt(location)
        if (location.y < highestBlock - 10) {
            plugin.logger.info("[DEBUG] Horse spawn prevented in cave")
            player?.sendMessage("§c[HorseCombat] Horses cannot spawn in caves!")
            return false
        }

        // Check if location is in water, lava, or problematic biomes
        if (block.type == Material.WATER ||
            block.type == Material.LAVA ||
            blockBelow.type == Material.WATER ||
            blockBelow.type == Material.LAVA ||
            block.biome == Biome.RIVER ||
            block.biome == Biome.SWAMP ||
            block.biome == Biome.WARM_OCEAN ||
            block.biome == Biome.LUKEWARM_OCEAN ||
            block.biome == Biome.COLD_OCEAN ||
            block.biome == Biome.FROZEN_OCEAN ||
            block.biome == Biome.DEEP_OCEAN ||
            block.biome == Biome.DEEP_FROZEN_OCEAN ||
            block.biome == Biome.DEEP_LUKEWARM_OCEAN ||
            block.biome == Biome.MANGROVE_SWAMP ||
            // Add beach biomes
            block.biome == Biome.BEACH ||
            block.biome == Biome.SNOWY_BEACH) {
            plugin.logger.info("[DEBUG] Horse spawn prevented in water/lava/beach")
            player?.sendMessage("§c[HorseCombat] Horses cannot spawn in water, lava, or beaches!")
            return false
        }

        // Ensure the block below is solid
        if (!blockBelow.type.isSolid) {
            plugin.logger.info("[DEBUG] Horse spawn prevented - no solid block underneath")
            player?.sendMessage("§c[HorseCombat] Horses must spawn on solid ground!")
            return false
        }

        return true
    }

    // Helper function to customize a horse with region properties
    private fun customizeHorse(horse: Horse, regionInfo: SpawnRegionInfo) {
        // Set horse color and style
        horse.color = regionInfo.color
        horse.style = regionInfo.style

        // Set attributes if specified
        regionInfo.speed?.let { speed ->
            horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)?.baseValue = speed
        }

        regionInfo.jumpStrength?.let { jump ->
            horse.jumpStrength = jump
        }

        regionInfo.maxHealth?.let { health ->
            val healthAttribute = horse.getAttribute(Attribute.GENERIC_MAX_HEALTH)
            healthAttribute?.baseValue = health
            horse.health = health
        }

        // Set custom name if provided
        regionInfo.name?.let { name ->
            horse.customName = name
            horse.isCustomNameVisible = true
        }
    }

    // Debug command to toggle debug mode
    fun toggleDebug(player: Player): Boolean {
        val uuid = player.uniqueId
        return if (debugPlayers.contains(uuid)) {
            debugPlayers.remove(uuid)
            player.sendMessage("§6[HorseSpawn] Debug mode disabled")
            false
        } else {
            debugPlayers.add(uuid)
            player.sendMessage("§6[HorseSpawn] Debug mode enabled")
            true
        }
    }

    // Method to force spawn a horse at a location - needed for /horsecombat spawnhorse command
    fun forceSpawnHorse(location: Location) {
        // Find the matching region
        val region = findMatchingRegion(location)

        if (region != null) {
            // Spawn a horse using the region's properties
            spawnCustomHorse(location, region)
        } else {
            // If no region matches, spawn a default horse
            val world = location.world
            val horse = world.spawnEntity(location, EntityType.HORSE) as Horse

            // Set some default properties
            horse.color = Horse.Color.BROWN
            horse.style = Horse.Style.NONE

            plugin.logger.info("[DEBUG] Default horse spawned at ${location.blockX}, ${location.blockY}, ${location.blockZ}")
        }
    }

    // Method to list all regions to a command sender - needed for /horsecombat listregs command
    fun listRegions(sender: CommandSender) {
        if (spawnRegions.isEmpty()) {
            sender.sendMessage("§c[HorseCombat] No horse spawn regions are configured.")
            return
        }

        // List all regions with their properties
        spawnRegions.forEachIndexed { index, region ->
            sender.sendMessage("§7Region #${index+1}: §f${region.worldName}")
            sender.sendMessage("§7  Area: §f(${region.x1}, ${region.z1}) to (${region.x2}, ${region.z2})")
            sender.sendMessage("§7  Horse: §f${region.color} with ${region.style} style")

            // Show custom attributes if present
            region.name?.let { sender.sendMessage("§7  Name: §f$it") }
            region.speed?.let { sender.sendMessage("§7  Speed: §f$it") }
            region.jumpStrength?.let { sender.sendMessage("§7  Jump: §f$it") }
            region.maxHealth?.let { sender.sendMessage("§7  Health: §f$it") }

            sender.sendMessage("§7  Spawn Chance: §f${region.spawnChance}")
        }
    }
}