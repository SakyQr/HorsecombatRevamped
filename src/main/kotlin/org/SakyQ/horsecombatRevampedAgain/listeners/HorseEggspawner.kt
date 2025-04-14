package org.SakyQ.horsecombatRevampedAgain.listeners

import org.SakyQ.horsecombatRevampedAgain.HorsecombatRevampedAgain
import org.bukkit.Location
import org.bukkit.entity.Horse
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.*

class HorseEggspawner(private val plugin: HorsecombatRevampedAgain) : Listener {

    private val regions = mutableListOf<HorseRegion>()

    init {
        loadRegionsFromConfig()
    }

    // Register the listener
    fun register() {
        plugin.server.pluginManager.registerEvents(this, plugin)
        plugin.logger.info("[HorseCombat] Horse Egg Spawner listener registered")
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        // Check if player is right-clicking with a horse egg
        if (event.action != Action.RIGHT_CLICK_BLOCK || event.item == null) {
            return
        }

        val item = event.item ?: return

        // Check if it's a horse spawn egg
        if (item.type != org.bukkit.Material.HORSE_SPAWN_EGG) {
            return
        }

        // Check if it has our custom data
        val meta = item.itemMeta ?: return
        val dataKey = org.bukkit.NamespacedKey(plugin, "horsecombat_type")

        if (!meta.persistentDataContainer.has(dataKey, PersistentDataType.STRING)) {
            return
        }

        // Get the horse type
        val horseType = meta.persistentDataContainer.get(dataKey, PersistentDataType.STRING) ?: return
        val (color, style) = parseHorseType(horseType) ?: return

        // Cancel the original event
        event.isCancelled = true

        // Spawn custom horse at clicked location
        val clickedBlock = event.clickedBlock ?: return
        val spawnLoc = clickedBlock.location.add(0.5, 1.0, 0.5)

        // Spawn the horse
        val horse = spawnLoc.world.spawnEntity(spawnLoc, org.bukkit.entity.EntityType.HORSE) as Horse

        // Set horse properties
        horse.color = color
        if (style != null) {
            horse.style = style
        }

        // Set horse attributes based on type
        setHorseAttributesFromType(horse, horseType)

        // Consume one egg if not in creative mode
        if (event.player.gameMode != org.bukkit.GameMode.CREATIVE) {
            val itemInHand = event.item!!
            if (itemInHand.amount > 1) {
                itemInHand.amount = itemInHand.amount - 1
            } else {
                event.player.inventory.setItemInMainHand(null)
            }
        }

        if (plugin.isDebugEnabled()) {
            plugin.logger.info("Player ${event.player.name} spawned a custom horse of type $horseType")
        }
    }

    fun loadRegionsFromConfig() {
        regions.clear()

        val configRegions = plugin.config.getConfigurationSection("regions") ?: return

        for (key in configRegions.getKeys(false)) {
            val regionSection = configRegions.getConfigurationSection(key) ?: continue

            val name = regionSection.getString("name") ?: key
            val world = regionSection.getString("world") ?: continue

            val x1 = regionSection.getInt("x1")
            val y1 = regionSection.getInt("y1")
            val z1 = regionSection.getInt("z1")
            val x2 = regionSection.getInt("x2")
            val y2 = regionSection.getInt("y2")
            val z2 = regionSection.getInt("z2")

            val colorName = regionSection.getString("horseColor")
            val color = if (colorName != null) {
                try {
                    Horse.Color.valueOf(colorName.uppercase(Locale.ROOT))
                } catch (e: IllegalArgumentException) {
                    null
                }
            } else null

            val styleName = regionSection.getString("horseStyle")
            val style = if (styleName != null) {
                try {
                    Horse.Style.valueOf(styleName.uppercase(Locale.ROOT))
                } catch (e: IllegalArgumentException) {
                    null
                }
            } else null

            val region = HorseRegion(name, world, x1, y1, z1, x2, y2, z2, color, style)
            regions.add(region)

            if (plugin.isDebugEnabled()) {
                plugin.logger.info("Loaded horse region: $name in world $world")
            }
        }

        if (plugin.isDebugEnabled()) {
            plugin.logger.info("Loaded ${regions.size} horse regions")
        }
    }

    fun findMatchingRegion(location: Location): HorseRegion? {
        val world = location.world?.name ?: return null
        val x = location.blockX
        val y = location.blockY
        val z = location.blockZ

        return regions.find { region ->
            region.world == world &&
                    x in minOf(region.x1, region.x2)..maxOf(region.x1, region.x2) &&
                    y in minOf(region.y1, region.y2)..maxOf(region.y1, region.y2) &&
                    z in minOf(region.z1, region.z2)..maxOf(region.z1, region.z2)
        }
    }

    // Getter for regions
    fun getRegions(): List<HorseRegion> {
        return regions.toList()
    }

    private fun parseHorseType(type: String): Pair<Horse.Color, Horse.Style?>? {
        return when (type.lowercase()) {
            "white_horse" -> Pair(Horse.Color.WHITE, null)
            "black_horse" -> Pair(Horse.Color.BLACK, null)
            "brown_horse" -> Pair(Horse.Color.BROWN, null)
            "chestnut_horse" -> Pair(Horse.Color.CHESTNUT, null)
            "creamy_horse" -> Pair(Horse.Color.CREAMY, null)
            "dark_brown_horse" -> Pair(Horse.Color.DARK_BROWN, null)
            "gray_horse" -> Pair(Horse.Color.GRAY, null)
            "mountain_horse" -> Pair(Horse.Color.DARK_BROWN, Horse.Style.WHITE_DOTS)
            "desert_horse" -> Pair(Horse.Color.CREAMY, Horse.Style.WHITE)
            "forest_horse" -> Pair(Horse.Color.BROWN, Horse.Style.BLACK_DOTS)
            "arctic_horse" -> Pair(Horse.Color.WHITE, Horse.Style.WHITE_DOTS)
            "spotted_horse" -> Pair(Horse.Color.WHITE, Horse.Style.BLACK_DOTS)
            "black_dots_horse" -> Pair(Horse.Color.GRAY, Horse.Style.BLACK_DOTS)
            "white_dots_horse" -> Pair(Horse.Color.BLACK, Horse.Style.WHITE_DOTS)
            "white_stripe_horse" -> Pair(Horse.Color.DARK_BROWN, Horse.Style.WHITE)
            else -> null
        }
    }

    private fun setHorseAttributesFromType(horse: Horse, type: String) {
        // Set base attributes
        horse.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH)?.baseValue = 20.0
        horse.health = 20.0
        horse.isTamed = true
        horse.inventory.saddle = ItemStack(org.bukkit.Material.SADDLE)

        // Set specific attributes based on horse type
        when (type.lowercase()) {
            "mountain_horse" -> {
                horse.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MOVEMENT_SPEED)?.baseValue = 0.175
                horse.jumpStrength = 0.7
            }
            "desert_horse" -> {
                horse.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MOVEMENT_SPEED)?.baseValue = 0.25
                horse.jumpStrength = 0.5
            }
            "forest_horse" -> {
                horse.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MOVEMENT_SPEED)?.baseValue = 0.2
                horse.jumpStrength = 0.6
            }
            "arctic_horse" -> {
                horse.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MOVEMENT_SPEED)?.baseValue = 0.18
                horse.jumpStrength = 0.8
            }
            else -> {
                // Default values
                horse.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MOVEMENT_SPEED)?.baseValue = 0.2
                horse.jumpStrength = 0.6
            }
        }
    }
}

data class HorseRegion(
    val name: String,
    val world: String,
    val x1: Int,
    val y1: Int,
    val z1: Int,
    val x2: Int,
    val y2: Int,
    val z2: Int,
    val color: Horse.Color?,
    val style: Horse.Style?
)