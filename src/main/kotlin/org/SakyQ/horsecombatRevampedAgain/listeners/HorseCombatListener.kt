package org.SakyQ.horsecombatRevampedAgain.listeners

import org.SakyQ.horsecombatRevampedAgain.HorsecombatRevampedAgain
import org.SakyQ.horsecombatRevampedAgain.utils.MomentumUtils
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Horse
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import java.util.*

class HorseCombatListener(private val plugin: HorsecombatRevampedAgain) : Listener {

    // Map to store the previous yaw of each horse
    private val horseYawMap: MutableMap<UUID, Float> = HashMap()

    // Map to store the previous location of each horse
    private val horseLocationMap: MutableMap<UUID, Vector> = HashMap()

    // Map to store the last movement time of each horse
    private val horseLastMoveMap: MutableMap<UUID, Long> = HashMap()

    // Set to track entities currently being damaged to prevent infinite loops
    private val entitiesBeingDamaged: MutableSet<UUID> = HashSet()

    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerAttack(event: EntityDamageByEntityEvent) {
        val damager = event.damager
        val target = event.entity

        // Skip if the event is already cancelled (e.g., by a claim plugin)
        if (event.isCancelled) return

        // Add Towny checks
        if (plugin.shouldRespectTowny()) {
            val damagerLoc = damager.location
            val targetLoc = target.location

            try {
                // Get TownyAPI safely through the main plugin
                val townyAPI = plugin.getTownyAPI()

                // Use reflection to call methods
                val getTownMethod = townyAPI?.javaClass?.getMethod("getTown", Location::class.java)
                val town = getTownMethod?.invoke(townyAPI, targetLoc)

                if (town != null) {
                    // Get the resident
                    val getResidentMethod = townyAPI?.javaClass?.getMethod("getResident", UUID::class.java)
                    val resident = getResidentMethod?.invoke(townyAPI, damager.uniqueId)

                    // Check if war is active
                    val isWarTimeMethod = townyAPI?.javaClass?.getMethod("isWarTime")
                    val isWarTime = isWarTimeMethod?.invoke(townyAPI) as? Boolean ?: false

                    // If target is in a town and:
                    // 1. There's no war
                    // 2. Attacker doesn't belong to town
                    // Then cancel the event
                    if (!isWarTime) {
                        val townClass = town.javaClass
                        val hasResidentMethod = townClass.getMethod("hasResident", UUID::class.java)
                        val hasResident = hasResidentMethod.invoke(town, damager.uniqueId) as? Boolean ?: false

                        if (resident == null || !hasResident) {
                            // Check if PvP is allowed in this town
                            val isPVPMethod = townClass.getMethod("isPVP")
                            val isPVP = isPVPMethod.invoke(town) as? Boolean ?: false

                            if (!isPVP) {
                                damager.sendMessage("§c[HorseCombat] You cannot attack in this town!")
                                event.isCancelled = true
                                return
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                plugin.logger.warning("Error checking Towny protection: ${e.message}")
            }
        }

        // Skip if the attacker is not a player
        if (damager !is Player) return

        // Skip if the target is not a living entity (we want to hit mobs and players)
        if (target !is LivingEntity) return

        // Get UUID for tracking damage processing
        val targetUuid = target.uniqueId

        // Check if we're already processing damage for this entity - prevents infinite loops
        if (entitiesBeingDamaged.contains(targetUuid)) {
            return
        }

        val lance = damager.inventory.itemInMainHand
        val meta = lance.itemMeta

        // Check if the item is a lance with the correct custom model data and display name
        val customModelData = plugin.config.getInt("lance.customModelData", 12345)
        if (lance.type != Material.STICK || meta?.displayName != plugin.config.getString("lance.displayName") || meta.customModelData != customModelData) return

        // Cancel vanilla damage to prevent stacking
        event.isCancelled = true

        try {
            // Add to processing set to prevent recursion
            entitiesBeingDamaged.add(targetUuid)

            if (damager.vehicle is Horse) {
                // Attacker is on a horse
                val momentum = MomentumUtils.getMomentum(damager)
                val maxDamage = plugin.config.getDouble("combat.maxDamage", 10.0)

                // Get mob-specific damage multiplier if target is not a player
                val mobDamageMultiplier = if (target !is Player) {
                    plugin.config.getDouble("combat.mobDamageMultiplier", 1.5)
                } else {
                    1.0
                }

                // Calculate damage based on momentum thresholds
                val baseDamage = when {
                    momentum >= 100 -> maxDamage
                    momentum >= 75 -> maxDamage * 0.75
                    momentum >= 50 -> maxDamage * 0.5
                    momentum >= 25 -> maxDamage * 0.25
                    else -> maxDamage * 0.1 // Minimal damage for less than 25 momentum
                }

                // Apply the mob multiplier if target is not a player
                val damage = baseDamage * mobDamageMultiplier

                // Apply damage directly without causing another event
                target.damage(damage)

                // Only for debug - can be disabled in production
                if (plugin.config.getBoolean("debug", false)) {
                    plugin.logger.info("Entity hit with momentum: $momentum, damage: $damage, entity type: ${target.type}")
                }

                // Handle knockoff for riders based on momentum thresholds
                val knockoffThreshold = plugin.config.getInt("combat.knockoffThreshold", 50)
                if (momentum >= knockoffThreshold && target.vehicle != null) {
                    // Add delay before ejecting to avoid damage cancellation
                    plugin.server.scheduler.scheduleSyncDelayedTask(plugin, {
                        if (target.isValid && target.vehicle != null) {
                            target.vehicle?.eject() // Knock off mount after a delay
                        }
                    }, 2L) // 2 tick delay (0.1 seconds)
                }

                // Apply knockback to mobs (not for players, as it can be annoying in PvP)
                if (target !is Player && momentum >= 25) {
                    val knockbackStrength = momentum / 50.0 // 0.5 to 2.0 based on momentum
                    val direction = target.location.toVector().subtract(damager.location.toVector()).normalize()
                    target.velocity = direction.multiply(knockbackStrength)
                }

                // Use only sounds based on momentum levels - no heavy particles or entities
                when {
                    momentum >= 100 -> {
                        damager.world.playSound(target.location, Sound.ENTITY_SLIME_SQUISH, 1f, 0.5f)
                        damager.world.playSound(target.location, Sound.ENTITY_SLIME_JUMP, 1f, 0.5f)
                    }
                    momentum >= 75 -> {
                        damager.world.playSound(target.location, Sound.ENTITY_SLIME_SQUISH, 0.8f, 0.7f)
                        damager.world.playSound(target.location, Sound.ENTITY_SLIME_JUMP, 0.8f, 0.7f)
                    }
                    momentum >= 50 -> {
                        damager.world.playSound(target.location, Sound.ENTITY_SLIME_SQUISH, 0.6f, 0.9f)
                    }
                    momentum >= 25 -> {
                        damager.world.playSound(target.location, Sound.ENTITY_SLIME_SQUISH, 0.4f, 1.1f)
                    }
                }

                // Reset momentum after attack
                MomentumUtils.resetMomentum(damager)
            } else {
                // Attacker is on foot
                val footDamage = if (target is Player) {
                    plugin.config.getDouble("combat.footDamage", 0.5)
                } else {
                    // Different damage for mobs when on foot
                    plugin.config.getDouble("combat.footDamageMobs", 1.0)
                }

                target.damage(footDamage) // Apply configurable damage

                val slownessDuration = plugin.config.getInt("combat.slownessDuration", 100)
                val slownessLevel = plugin.config.getInt("combat.slownessLevel", 1)
                damager.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, slownessDuration, slownessLevel))
            }

            // Play hit sound - safely handle nullable value
            val hitSoundString = plugin.config.getString("sounds.hit") ?: "ENTITY_SLIME_ATTACK"
            try {
                val hitSound = Sound.valueOf(hitSoundString)
                damager.world.playSound(target.location, hitSound, 1f, 1f)
            } catch (e: IllegalArgumentException) {
                // Log invalid sound and fall back to default
                plugin.logger.warning("Invalid sound in config: $hitSoundString. Using default sound.")
                damager.world.playSound(target.location, Sound.ENTITY_SLIME_ATTACK, 1f, 1f)
            }
        } finally {
            // Always remove from processing set when done
            entitiesBeingDamaged.remove(targetUuid)
        }
    }

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        if (player.vehicle is Horse) {
            val horse = player.vehicle as Horse
            val horseUuid = horse.uniqueId
            val currentTime = System.currentTimeMillis()

            // Get current location as vector for distance calculation
            val currentLocation = horse.location.toVector()
            val currentYaw = horse.location.yaw

            // Get previous states
            val previousYaw = horseYawMap.getOrDefault(horseUuid, currentYaw)
            val previousLocation = horseLocationMap.getOrDefault(horseUuid, currentLocation)

            // Check if the horse has moved
            val distance = currentLocation.distance(previousLocation)
            val movementThreshold = plugin.config.getDouble("momentum.movementThreshold", 0.05)

            if (distance < movementThreshold) {
                // Horse isn't moving (or moving very little)
                val stallTime = plugin.config.getLong("momentum.stallTimeMs", 1000) // 1 second default
                val lastMoveTime = horseLastMoveMap.getOrDefault(horseUuid, currentTime)

                if (currentTime - lastMoveTime > stallTime) {
                    // Horse has been still for the configured time
                    MomentumUtils.resetMomentum(player)
                    updateMomentumBar(player)

                    // Use only sound feedback for stopping - minimal particles
                    if (plugin.config.getBoolean("effects.showStopEffects", true)) {
                        player.world.playSound(horse.location, Sound.ENTITY_HORSE_BREATHE, 0.5f, 0.8f)
                    }
                }
            } else {
                // Horse is moving, update last move time
                horseLastMoveMap[horseUuid] = currentTime

                // Calculate the difference in yaw (angle change)
                val yawDifference = Math.abs(angleDifference(currentYaw, previousYaw))
                val turnThreshold = plugin.config.getDouble("momentum.turnThreshold", 30.0)

                if (yawDifference > turnThreshold) {
                    // Sharp turn detected
                    val momentumLoss = plugin.config.getInt("momentum.turnLoss", 10)
                    MomentumUtils.reduceMomentum(player, momentumLoss)
                    updateMomentumBar(player)

                    // Just use sound feedback for turns - no particles
                    if (plugin.config.getBoolean("effects.showTurnEffects", true)) {
                        player.world.playSound(horse.location, Sound.ENTITY_HORSE_BREATHE, 1f, 1f)
                    }
                } else {
                    // Straight movement, increase momentum
                    val momentumGain = plugin.config.getInt("momentum.straightGain", 1)
                    MomentumUtils.increaseMomentum(player, momentumGain)
                    updateMomentumBar(player)
                }
            }

            // Update the stored values
            horseYawMap[horseUuid] = currentYaw
            horseLocationMap[horseUuid] = currentLocation
        }
    }

    // Helper function to properly calculate angle differences (handles wrap-around)
    private fun angleDifference(angle1: Float, angle2: Float): Float {
        var diff = (angle1 - angle2) % 360
        if (diff < -180) diff += 360
        if (diff > 180) diff -= 360
        return Math.abs(diff)
    }

    private fun updateMomentumBar(player: Player) {
        val momentum = MomentumUtils.getMomentum(player)

        // Update XP bar if configured to do so
        if (plugin.config.getBoolean("display.useXpBar", true)) {
            player.level = momentum
            player.exp = momentum / 100f
        }

        // Show action bar message if configured to do so
        if (plugin.config.getBoolean("display.useActionBar", true)) {
            val format = plugin.config.getString("display.actionBarFormat") ?: "§bMomentum: %momentum%/100"
            val message = format.replace("%momentum%", momentum.toString())
            player.sendActionBar(message)
        }
    }
}