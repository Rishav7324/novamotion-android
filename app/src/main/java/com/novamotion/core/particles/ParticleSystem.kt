package com.novamotion.core.particles

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var size: Float,
    var life: Float = 1.0f, // 1.0 (born) down to 0.0 (dead)
    var decayRate: Float = 0.5f,
    var rotation: Float = 0f,
    var rotationSpeed: Float = 0f,
    var colorHex: Long = 0xFF06B6D4
) {
    val isDead: Boolean get() = life <= 0f
}

enum class EmitterShape {
    POINT,
    BOX,
    RADIAL_BURST
}

class ParticleSystem(
    val maxParticles: Int = 1000,
    var emitterX: Float = 540f,
    var emitterY: Float = 960f,
    var emitterShape: EmitterShape = EmitterShape.RADIAL_BURST,
    var gravityX: Float = 0f,
    var gravityY: Float = 250f, // Downward gravity
    var windX: Float = 0f,
    var turbulenceIntensity: Float = 60f,
    var attractorX: Float? = null,
    var attractorY: Float? = null,
    var attractorStrength: Float = 50000f
) {
    val particles = mutableListOf<Particle>()
    private val random = Random(System.currentTimeMillis())

    fun emit(count: Int, colorHex: Long = 0xFF06B6D4) {
        for (i in 0 until count) {
            if (particles.size >= maxParticles) break

            val angle = random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = 100f + random.nextFloat() * 400f
            val vx = cos(angle) * speed
            val vy = sin(angle) * speed

            val startX = when (emitterShape) {
                EmitterShape.POINT, EmitterShape.RADIAL_BURST -> emitterX
                EmitterShape.BOX -> emitterX + (random.nextFloat() - 0.5f) * 300f
            }

            val startY = when (emitterShape) {
                EmitterShape.POINT, EmitterShape.RADIAL_BURST -> emitterY
                EmitterShape.BOX -> emitterY + (random.nextFloat() - 0.5f) * 100f
            }

            particles.add(
                Particle(
                    x = startX,
                    y = startY,
                    vx = vx,
                    vy = vy,
                    size = 4f + random.nextFloat() * 12f,
                    decayRate = 0.3f + random.nextFloat() * 0.7f,
                    rotationSpeed = (random.nextFloat() - 0.5f) * 360f,
                    colorHex = colorHex
                )
            )
        }
    }

    /**
     * Physics update step with Verlet/Euler integration and force fields.
     */
    fun update(dtSec: Float) {
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.life -= p.decayRate * dtSec

            if (p.isDead) {
                iterator.remove()
                continue
            }

            // Apply gravity and wind
            p.vx += (gravityX + windX) * dtSec
            p.vy += gravityY * dtSec

            // Apply turbulence force (harmonic noise)
            val turbAngle = (p.x * 0.01f + p.y * 0.01f)
            p.vx += cos(turbAngle) * turbulenceIntensity * dtSec
            p.vy += sin(turbAngle) * turbulenceIntensity * dtSec

            // Apply Point Attractor (Gravitational Pull / Black Hole)
            attractorX?.let { ax ->
                attractorY?.let { ay ->
                    val dx = ax - p.x
                    val dy = ay - p.y
                    val distSq = dx * dx + dy * dy + 100f // Epsilon softening
                    val force = attractorStrength / distSq
                    val dist = sqrt(distSq)
                    p.vx += (dx / dist) * force * dtSec
                    p.vy += (dy / dist) * force * dtSec
                }
            }

            // Position integration
            p.x += p.vx * dtSec
            p.y += p.vy * dtSec

            // Rotation integration
            p.rotation += p.rotationSpeed * dtSec
        }
    }
}
