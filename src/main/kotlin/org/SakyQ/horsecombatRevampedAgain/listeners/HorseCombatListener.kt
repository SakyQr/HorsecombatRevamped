package org.SakyQ.horsecombatRevampedAgain.listeners

import org.SakyQ.horsecombatRevampedAgain.HorsecombatRevampedAgain
import org.SakyQ.horsecombatRevampedAgain.utils.MomentumUtils
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Horse
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDismountEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.vehicle.VehicleEnterEvent
import org.bukkit.event.vehicle.VehicleExitEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * CRITICAL FIXES APPLIED:
 * - Thread safety with ConcurrentHashMap
 * - Proper memory leak prevention
 * - Automatic cleanup on player disconnect
 * - Task cancellation safety
 * - Defensive programming patterns
 */
class HorseCombatListener(private val plugin: HorsecombatRevampedAgain) : Listener {

    // === THREAD-SAFE DATA STORAGE ===
    private val horsePlayers = ConcurrentHashMap.newKeySet<UUID>()
    private val lastPositions = ConcurrentHashMap<UUID, Vector>()
    private val lastYaw = ConcurrentHashMap<UUID, Float>()
    private val lastY = ConcurrentHashMap<UUID, Double>()

    // Task management
    private var movementTracker: BukkitTask? = null
    private val isShuttingDown = java.util.concurrent.atomic.AtomicBoolean(false)

    init {
        startMovementTracker()
    }

    // === SAFE TASK MANAGEMENT ===
    private fun startMovementTracker() {
        if (isShuttingDown.get()) return

        try {
            movementTracker = plugin.server.scheduler.runTaskTimer(plugin, this::updateMomentum, 0L, 4L)
        } catch (e: Exception) {
            plugin.logger.severe("Failed to start movement tracker: ${e.message}")
        }
    }

    private fun stopMovementTracker() {
        movementTracker?.let { task ->
            if (!task.isCancelled) {
                try {
                    task.cancel()
                } catch (e: Exception) {
                    plugin.logger.warning("Error cancelling movement tracker: ${e.message}")
                }
            }
        }
        movementTracker = null
    }

    // === MOUNT/DISMOUNT - THREAD SAFE ===
    @EventHandler
    fun onPlayerMountHorse(event: VehicleEnterEvent) {
        if (event.vehicle !is Horse || event.entered !is Player) return
        if (isShuttingDown.get()) return

        val player = event.entered as Player
        val horse = event.vehicle as Horse
        val playerId = player.uniqueId

        try {
            // Thread-safe operations
            horsePlayers.add(playerId)
            lastPositions[playerId] = horse.location.toVector()
            lastYaw[playerId] = horse.location.yaw
            lastY[playerId] = horse.location.y

            // Reset momentum
            MomentumUtils.resetMomentum(player)
            updateMomentumDisplay(player)

            if (plugin.isDebugEnabled()) {
                plugin.logger.info("${player.name} mounted horse")
            }
        } catch (e: Exception) {
            plugin.logger.warning("Error handling horse mount for ${player.name}: ${e.message}")
            // Cleanup on error
            cleanupPlayerData(playerId)
        }
    }

    @EventHandler
    fun onPlayerDismountHorse(event: VehicleExitEvent) {
        if (event.vehicle !is Horse || event.exited !is Player) return
        cleanupPlayer(event.exited as Player)
    }

    @EventHandler
    fun onEntityDismount(event: EntityDismountEvent) {
        if (event.dismounted !is Horse || event.entity !is Player) return
        cleanupPlayer(event.entity as Player)
    }

    // === CRITICAL: PLAYER DISCONNECT CLEANUP ===
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        cleanupPlayer(event.player)
    }

    // === THREAD-SAFE MOMENTUM SYSTEM ===
    private fun updateMomentum() {
        if (isShuttingDown.get()) return

        val playersToRemove = mutableSetOf<UUID>()

        // Create snapshot to avoid concurrent modification
        val currentPlayers = horsePlayers.toSet()

        for (playerId in currentPlayers) {
            try {
                val player = plugin.server.getPlayer(playerId)
                val horse = player?.vehicle as? Horse

                // Clean up if not on horse
                if (player == null || !player.isOnline || horse == null) {
                    playersToRemove.add(playerId)
                    continue
                }

                // Process momentum update safely
                processPlayerMomentum(player, horse, playerId)

            } catch (e: Exception) {
                plugin.logger.warning("Error updating momentum for player $playerId: ${e.message}")
                playersToRemove.add(playerId)
            }
        }

        // Clean up disconnected players
        playersToRemove.forEach(this::cleanupPlayerData)
    }

    private fun processPlayerMomentum(player: Player, horse: Horse, playerId: UUID) {
        // Get current data
        val currentPos = horse.location.toVector()
        val currentYaw = horse.location.yaw
        val currentY = horse.location.y

        // Get previous data safely
        val lastPos = lastPositions[playerId] ?: currentPos
        val previousYaw = lastYaw[playerId] ?: currentYaw
        val previousY = lastY[playerId] ?: currentY

        val distance = currentPos.distance(lastPos)
        val speed = distance / 0.2
        val yawDifference = Math.abs(angleDifference(currentYaw, previousYaw))
        val yChange = currentY - previousY

        // Debug logging (rate limited)
        if (plugin.isDebugEnabled() && System.currentTimeMillis() % 2000 < 100) {
            plugin.logger.info("${player.name}: speed=$speed, turn=${yawDifference}°, yChange=$yChange, momentum=${MomentumUtils.getMomentum(player)}")
        }

        // Apply momentum changes
        when {
            // JUMP PENALTY
            yChange > 0.3 -> {
                val jumpPenalty = when {
                    yChange > 1.5 -> 25
                    yChange > 1.0 -> 20
                    else -> 15
                }
                MomentumUtils.reduceMomentum(player, jumpPenalty)
            }
            // TURN PENALTY
            yawDifference > 37.0 && distance > 0.01 -> {
                val turnPenalty = if (yawDifference > 45.0) 50 else 20
                MomentumUtils.reduceMomentum(player, turnPenalty)
            }
            // NO MOVEMENT - RESET
            distance < 0.01 -> {
                if (MomentumUtils.getMomentum(player) > 0) {
                    MomentumUtils.resetMomentum(player)
                }
            }
            // MOVEMENT - GAIN
            else -> {
                val gain = when {
                    speed > 4.0 -> 4
                    speed > 2.0 -> 3
                    speed > 0.5 -> 2
                    else -> 1
                }
                MomentumUtils.increaseMomentum(player, gain)
            }
        }

        // Update tracking data atomically
        lastPositions[playerId] = currentPos
        lastYaw[playerId] = currentYaw
        lastY[playerId] = currentY

        // Update display
        updateMomentumDisplay(player)
    }

    // === COMBAT SYSTEM ===
    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerAttack(event: EntityDamageByEntityEvent) {
        if (event.isCancelled || event.damager !is Player || event.entity !is LivingEntity) return

        val damager = event.damager as Player
        val target = event.entity as LivingEntity

        if (!isHorseCombat(damager)) return
        if (!canCombatHere(damager, target)) {
            event.isCancelled = true
            return
        }

        event.isCancelled = true
        processCombat(damager, target)
    }

    private fun isHorseCombat(player: Player): Boolean {
        val item = player.inventory.itemInMainHand
        val onHorse = player.vehicle is Horse
        val hasLance = item.type == Material.STICK && item.hasItemMeta() &&
                (item.itemMeta?.customModelData in listOf(12345, 12346))

        return onHorse || hasLance
    }

    private fun canCombatHere(damager: Player, target: LivingEntity): Boolean {
        return try {
            when {
                plugin.shouldRespectWorldGuard() && !plugin.isCombatAllowedAtLocation(target.location) -> {
                    damager.sendMessage("§d[HorseCombat] Combat not allowed here!")
                    false
                }
                plugin.shouldRespectTowny() && !plugin.townyManager.shouldBypassTowny(damager) -> {
                    val (inTown, _) = plugin.townyManager.getTownAtLocation(target.location)
                    if (inTown && !plugin.townyManager.isPvPAllowedInTown(target.location)) {
                        damager.sendMessage("§d[HorseCombat] Combat not allowed in this town!")
                        false
                    } else true
                }
                else -> true
            }
        } catch (e: Exception) {
            plugin.logger.warning("Error checking combat permissions: ${e.message}")
            true // Default to allow on error
        }
    }

    private fun processCombat(damager: Player, target: LivingEntity) {
        try {
            val momentum = MomentumUtils.getMomentum(damager)
            val isOnHorse = damager.vehicle is Horse
            val lance = damager.inventory.itemInMainHand
            val isFancyLance = lance.hasItemMeta() && lance.itemMeta?.customModelData == 12346

            // Calculate damage
            val damage = when {
                isOnHorse -> calculateMountedDamage(momentum, target, isFancyLance)
                else -> calculateFootDamage(target, isFancyLance)
            }

            // Apply damage and effects
            target.damage(damage)
            playImpactEffects(target.location, momentum)
            playHitSound(target.location)

            // Handle knockoff
            if (isOnHorse && momentum >= 50 && target.vehicle != null) {
                plugin.server.scheduler.runTaskLater(plugin, Runnable {
                    if (target.isValid && target.vehicle != null) {
                        target.vehicle?.eject()
                    }
                }, 2L)
            }

            // Apply consequences
            if (isOnHorse) {
                MomentumUtils.reduceMomentum(damager, 30)
                updateMomentumDisplay(damager)
            } else {
                damager.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, 100, 1))
            }
        } catch (e: Exception) {
            plugin.logger.warning("Error processing combat: ${e.message}")
        }
    }

    private fun calculateMountedDamage(momentum: Int, target: LivingEntity, isFancy: Boolean): Double {
        val baseDamage = when {
            momentum >= 100 -> 10.0
            momentum >= 75 -> 7.5
            momentum >= 50 -> 5.0
            momentum >= 25 -> 2.5
            else -> 1.0
        }

        val fancyMultiplier = if (isFancy) 1.3 else 1.0
        val mobMultiplier = if (target !is Player) 1.5 else 1.0

        return baseDamage * fancyMultiplier * mobMultiplier
    }

    private fun calculateFootDamage(target: LivingEntity, isFancy: Boolean): Double {
        val baseDamage = if (target is Player) 0.5 else 1.0
        val fancyMultiplier = if (isFancy) 1.2 else 1.0
        return baseDamage * fancyMultiplier
    }

    private fun playImpactEffects(location: org.bukkit.Location, momentum: Int) {
        try {
            val effectName = when {
                momentum >= 80 -> "highImpact"
                momentum >= 50 -> "mediumImpact"
                else -> "lowImpact"
            }
            plugin.particleManager.playEffect(effectName, location)
        } catch (e: Exception) {
            // Ignore particle errors
        }
    }

    private fun playHitSound(location: org.bukkit.Location) {
        try {
            val sound = try {
                Sound.valueOf(plugin.config.getString("sounds.hit") ?: "ENTITY_SLIME_ATTACK")
            } catch (e: Exception) {
                Sound.ENTITY_SLIME_ATTACK
            }
            location.world.playSound(location, sound, 1f, 1f)
        } catch (e: Exception) {
            // Ignore sound errors
        }
    }

    // === BEAUTIFUL PURPLE GRADIENT DISPLAY ===
    private fun updateMomentumDisplay(player: Player) {
        try {
            val momentum = MomentumUtils.getMomentum(player)
            val blockBar = createBeautifulMomentumBar(momentum)
            val message = "§d⚡ Momentum: $blockBar"
            player.sendActionBar(message)
        } catch (e: Exception) {
            // Ignore display errors
        }
    }

    private fun createBeautifulMomentumBar(momentum: Int): String {
        val totalBlocks = 25
        val filledBlocks = (momentum * totalBlocks) / 100

        val bar = StringBuilder()

        for (i in 0 until totalBlocks) {
            if (i < filledBlocks) {
                val color = getPurpleGradientColor(i, totalBlocks)
                bar.append(color).append("▰")
            } else {
                bar.append("§8▱")
            }
        }

        return bar.toString()
    }

    private fun getPurpleGradientColor(position: Int, total: Int): String {
        val percentage = position.toDouble() / total.toDouble()

        return when {
            percentage < 0.15 -> "§4"
            percentage < 0.25 -> "§c"
            percentage < 0.35 -> "§6"
            percentage < 0.45 -> "§e"
            percentage < 0.55 -> "§a"
            percentage < 0.65 -> "§b"
            percentage < 0.75 -> "§9"
            percentage < 0.85 -> "§1"
            percentage < 0.95 -> "§5"
            else -> "§d"
        }
    }

    // === HELPER METHODS ===
    private fun angleDifference(angle1: Float, angle2: Float): Float {
        var diff = (angle1 - angle2) % 360
        if (diff < -180) diff += 360
        if (diff > 180) diff -= 360
        return Math.abs(diff)
    }

    // === CRITICAL CLEANUP METHODS ===
    private fun cleanupPlayerData(playerId: UUID) {
        try {
            horsePlayers.remove(playerId)
            lastPositions.remove(playerId)
            lastYaw.remove(playerId)
            lastY.remove(playerId)
        } catch (e: Exception) {
            plugin.logger.warning("Error cleaning up player data for $playerId: ${e.message}")
        }
    }

    fun cleanupPlayer(player: Player) {
        try {
            val playerId = player.uniqueId
            cleanupPlayerData(playerId)
            MomentumUtils.resetMomentum(player)

            if (plugin.isDebugEnabled()) {
                plugin.logger.info("${player.name} dismounted - cleaned up data")
            }
        } catch (e: Exception) {
            plugin.logger.warning("Error cleaning up player ${player.name}: ${e.message}")
        }
    }

    // === CRITICAL SHUTDOWN CLEANUP ===
    fun cleanup() {
        try {
            isShuttingDown.set(true)

            // Stop the movement tracker first
            stopMovementTracker()

            // Clear all data
            horsePlayers.clear()
            lastPositions.clear()
            lastYaw.clear()
            lastY.clear()

            plugin.logger.info("HorseCombatListener cleanup completed")
        } catch (e: Exception) {
            plugin.logger.severe("Error during HorseCombatListener cleanup: ${e.message}")
        }
    }

    fun getMomentum(player: Player): Int = MomentumUtils.getMomentum(player)
}