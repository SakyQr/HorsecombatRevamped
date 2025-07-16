package org.SakyQ.horsecombatRevampedAgain.managers

import org.SakyQ.horsecombatRevampedAgain.HorsecombatRevampedAgain
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Horse
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

class ParticleManager(private val plugin: HorsecombatRevampedAgain) {

    private val particleEffects = mutableMapOf<String, ParticleEffect>()
    private val activeParticleEffects = mutableMapOf<UUID, BukkitRunnable>()
    private val playerHorseTrails = mutableMapOf<UUID, ParticleTrailEffect>()

    fun loadParticleEffects() {
        // Clear existing effects first
        particleEffects.clear()

        // Load effect configurations from config
        val effectsSection = plugin.config.getConfigurationSection("effects.particleEffects")

        if (effectsSection != null) {
            for (key in effectsSection.getKeys(false)) {
                val section = effectsSection.getConfigurationSection(key) ?: continue

                try {
                    val particleType = section.getString("particle") ?: "CLOUD"
                    val count = section.getInt("count", 5)
                    val offsetX = section.getDouble("offsetX", 0.2)
                    val offsetY = section.getDouble("offsetY", 0.2)
                    val offsetZ = section.getDouble("offsetZ", 0.2)
                    val speed = section.getDouble("speed", 0.1)
                    val colorR = section.getInt("colorR", 255)
                    val colorG = section.getInt("colorG", 255)
                    val colorB = section.getInt("colorB", 255)
                    val sound = section.getString("sound") ?: ""
                    val volume = section.getDouble("volume", 1.0).toFloat()
                    val pitch = section.getDouble("pitch", 1.0).toFloat()

                    val particle = try {
                        Particle.valueOf(particleType)
                    } catch (e: IllegalArgumentException) {
                        plugin.logger.warning("Invalid particle type: $particleType. Using CLOUD instead.")
                        Particle.CLOUD
                    }

                    particleEffects[key] = ParticleEffect(
                        particle, count, offsetX, offsetY, offsetZ, speed,
                        Color.fromRGB(colorR, colorG, colorB),
                        sound, volume, pitch
                    )

                    if (plugin.isDebugEnabled()) {
                        plugin.logger.info("Loaded particle effect: $key (${particle.name})")
                    }

                } catch (e: Exception) {
                    plugin.logger.warning("Failed to load particle effect $key: ${e.message}")
                }
            }
        }

        plugin.logger.info("Loaded ${particleEffects.size} particle effects")
    }

    // Play a predefined effect at a location
    fun playEffect(effectName: String, location: Location) {
        val effect = particleEffects[effectName] ?: return

        try {
            // Play the particle effect
            if (effect.particle == Particle.DUST     || effect.particle.name == "REDSTONE") {
                // Handle colored dust particles
                val dustOptions = Particle.DustOptions(effect.color, 1.0f)
                location.world.spawnParticle(
                    effect.particle,
                    location.x, location.y, location.z,
                    effect.count,
                    effect.offsetX, effect.offsetY, effect.offsetZ,
                    effect.speed,
                    dustOptions
                )
            } else {
                location.world.spawnParticle(
                    effect.particle,
                    location.x, location.y, location.z,
                    effect.count,
                    effect.offsetX, effect.offsetY, effect.offsetZ,
                    effect.speed
                )
            }

            // Play the associated sound if one exists
            if (effect.sound.isNotEmpty()) {
                try {
                    val soundEnum = Sound.valueOf(effect.sound)
                    location.world.playSound(location, soundEnum, effect.volume, effect.pitch)
                } catch (e: IllegalArgumentException) {
                    if (plugin.isDebugEnabled()) {
                        plugin.logger.warning("Invalid sound name: ${effect.sound}")
                    }
                }
            }
        } catch (e: Exception) {
            if (plugin.isDebugEnabled()) {
                plugin.logger.warning("Error playing effect $effectName: ${e.message}")
            }
        }
    }

    // Play momentum-based particle effects
    fun playMomentumEffect(player: Player, momentum: Int, location: Location) {
        // Scale effects based on momentum levels
        when {
            momentum >= 80 -> {
                playEffect("highMomentum", location)
                startSpeedTrail(player, momentum)
            }
            momentum >= 50 -> {
                playEffect("mediumMomentum", location)
                startSpeedTrail(player, momentum)
            }
            momentum >= 20 -> {
                playEffect("lowMomentum", location)
                stopSpeedTrail(player)
            }
            else -> {
                stopSpeedTrail(player)
            }
        }
    }

    // Play a circular effect around a player's horse
    fun playChargeEffect(player: Player, momentum: Int) {
        if (momentum < 50 || player.vehicle !is Horse) return

        val horse = player.vehicle as Horse
        val location = horse.location.clone().add(0.0, 1.0, 0.0)
        val radius = 1.0
        val particleCount = (momentum / 10).coerceAtLeast(5)

        object : BukkitRunnable() {
            private var angle = 0.0
            private var runCount = 0

            override fun run() {
                runCount++

                for (i in 0 until 2) {
                    angle += Math.PI / 8

                    val x = radius * cos(angle)
                    val z = radius * sin(angle)

                    val particleLocation = location.clone().add(x, 0.0, z)

                    if (momentum >= 80) {
                        location.world.spawnParticle(Particle.FLAME, particleLocation, 1, 0.0, 0.0, 0.0, 0.0)
                    } else {
                        location.world.spawnParticle(Particle.CLOUD, particleLocation, 1, 0.0, 0.0, 0.0, 0.0)
                    }
                }

                // Only run for a short time
                if (runCount >= 10 || player.vehicle !is Horse) {
                    cancel()
                }
            }
        }.runTaskTimer(plugin, 0L, 2L)
    }

    // Play braking effect when horse slows down
    fun playBrakingEffect(player: Player, momentum: Int, oldMomentum: Int) {
        if (player.vehicle !is Horse || oldMomentum - momentum < 10) return

        val horse = player.vehicle as Horse
        val location = horse.location.clone()
        val direction = horse.location.direction.normalize().multiply(-1)

        try {
            // Check if REDSTONE particle exists
            val redstoneParticle = getRedstoneParticle()
            val dustOptions = Particle.DustOptions(Color.fromRGB(139, 69, 19), 1.5f)

            for (i in 0 until (oldMomentum - momentum).coerceAtMost(20)) {
                val offset = direction.clone().multiply(Math.random() * 0.5)
                val particleLoc = location.clone().add(offset).add(0.0, 0.1, 0.0)

                // Spawn the appropriate particle
                if (redstoneParticle != null) {
                    location.world.spawnParticle(
                        redstoneParticle,
                        particleLoc,
                        1, 0.1, 0.0, 0.1, 0.0, dustOptions
                    )
                } else {
                    // Fallback to a regular particle if REDSTONE not available
                    location.world.spawnParticle(
                        Particle.CLOUD,
                        particleLoc,
                        1, 0.1, 0.0, 0.1, 0.0
                    )
                }
            }

            // Play sound effect
            if (oldMomentum - momentum > 20) {
                location.world.playSound(location, Sound.BLOCK_GRAVEL_BREAK, 1.0f, 0.5f)
            }
        } catch (e: Exception) {
            if (plugin.isDebugEnabled()) {
                plugin.logger.warning("Error displaying braking effect: ${e.message}")
            }
        }
    }

    // Safe way to get REDSTONE particle (version compatible)
    private fun getRedstoneParticle(): Particle? {
        return try {
            Particle.valueOf("REDSTONE")
        } catch (e: IllegalArgumentException) {
            // For older versions, try alternative names
            try {
                Particle.valueOf("RED_DUST")
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }

    // Start continuous trailing effect for a player
    fun startSpeedTrail(player: Player, momentum: Int) {
        if (player.vehicle !is Horse) return

        // If a trail is already active, stop it first
        stopSpeedTrail(player)

        // Create a new trail effect
        val trail = ParticleTrailEffect(plugin, player, momentum)
        playerHorseTrails[player.uniqueId] = trail
        trail.runTaskTimer(plugin, 0L, 2L)
    }

    // Stop an active trail effect
    fun stopSpeedTrail(player: Player) {
        val trail = playerHorseTrails.remove(player.uniqueId)
        if (trail != null && !trail.isCancelled) {
            trail.cancel()
        }
    }

    // Get a specific particle effect
    fun getEffect(name: String): ParticleEffect? {
        return particleEffects[name]
    }

    // Data class for particle effects
    data class ParticleEffect(
        val particle: Particle,
        val count: Int,
        val offsetX: Double,
        val offsetY: Double,
        val offsetZ: Double,
        val speed: Double,
        val color: Color,
        val sound: String,
        val volume: Float,
        val pitch: Float
    )

    // Inner class for continuous trail effects
    inner class ParticleTrailEffect(
        private val plugin: HorsecombatRevampedAgain,
        private val player: Player,
        private var momentum: Int
    ) : BukkitRunnable() {
        private val previousLocations = LinkedList<Location>()
        private val maxTrailLength = 5

        override fun run() {
            val horse = player.vehicle

            if (horse !is Horse) {
                cancel()
                return
            }

            // Update momentum value
            momentum = plugin.horseCombatListener.getMomentum(player)

            // Add current location to the list
            val currentLoc = horse.location.clone().add(0.0, 0.5, 0.0)
            previousLocations.addFirst(currentLoc)

            // Keep the list at the maximum size
            while (previousLocations.size > maxTrailLength) {
                previousLocations.removeLast()
            }

            // Skip if not enough movement
            if (previousLocations.size < 2) return

            // Generate trail based on momentum
            val particle = when {
                momentum >= 80 -> Particle.FLAME
                momentum >= 50 -> Particle.CLOUD
                else -> Particle.CLOUD
            }

            val count = when {
                momentum >= 80 -> 3
                momentum >= 50 -> 2
                else -> 1
            }

            // Draw trail particles at previous locations with fading effect
            for (i in 1 until previousLocations.size) {
                val loc = previousLocations[i]
                val fadeRatio = 1.0 - (i.toDouble() / previousLocations.size)

                try {
                    // Handle special particle types
                    val redstoneParticle = getRedstoneParticle()

                    if (redstoneParticle != null && particle.name == "REDSTONE") {
                        // For colored dust particles
                        val color = when {
                            momentum >= 80 -> Color.fromRGB(255, 50, 0)
                            momentum >= 50 -> Color.fromRGB(255, 160, 0)
                            else -> Color.fromRGB(200, 200, 200)
                        }

                        val dustOptions = Particle.DustOptions(color, fadeRatio.toFloat() * 1.5f)
                        loc.world.spawnParticle(redstoneParticle, loc, count, 0.05, 0.05, 0.05, 0.01, dustOptions)
                    } else {
                        loc.world.spawnParticle(particle, loc, count, 0.05, 0.05, 0.05, 0.01 * fadeRatio)
                    }
                } catch (e: Exception) {
                    if (plugin.isDebugEnabled()) {
                        plugin.logger.warning("Error displaying trail: ${e.message}")
                    }
                }
            }

            // Cancel if momentum is too low
            if (momentum < 20) {
                cancel()
                playerHorseTrails.remove(player.uniqueId)
            }
        }
    }
}