package com.novamotion.core.animation

import kotlin.math.*

/**
 * Analytical Two-Bone Inverse Kinematics (IK) Solver.
 * Implements the Law of Cosines closed-form solution with Pole Vector constraint.
 * Used for character rigging (limbs, legs, arms, mechanical robotics) in NovaMotion.
 */
data class Joint2D(
    var x: Float,
    var y: Float,
    var angleDegrees: Float = 0f
)

data class IKSolution(
    val rootAngleDegrees: Float,
    val jointAngleDegrees: Float,
    val isReachable: Boolean
)

object InverseKinematicsSolver {

    /**
     * Solves the two-bone analytical IK problem in 2D plane.
     * @param root Position of the root joint (e.g. shoulder / hip)
     * @param target Position of the target effector (e.g. hand / foot)
     * @param length1 Length of the first bone (e.g. upper arm)
     * @param length2 Length of the second bone (e.g. forearm)
     * @param flipBend If true, bends knee/elbow in the opposite direction
     */
    fun solve2Bone(
        rootX: Float, rootY: Float,
        targetX: Float, targetY: Float,
        length1: Float, length2: Float,
        flipBend: Boolean = false
    ): IKSolution {
        val dx = targetX - rootX
        val dy = targetY - rootY
        val distSq = dx * dx + dy * dy
        val dist = sqrt(distSq)

        val totalLength = length1 + length2
        val minLength = abs(length1 - length2)

        // Case 1: Target is beyond reach (fully extended)
        if (dist >= totalLength) {
            val baseAngle = atan2(dy, dx)
            val deg = Math.toDegrees(baseAngle.toDouble()).toFloat()
            return IKSolution(rootAngleDegrees = deg, jointAngleDegrees = 0f, isReachable = false)
        }

        // Case 2: Target is too close to root (fully folded)
        if (dist <= minLength) {
            val baseAngle = atan2(dy, dx)
            val deg = Math.toDegrees(baseAngle.toDouble()).toFloat()
            val foldAngle = if (length1 > length2) 180f else 0f
            return IKSolution(rootAngleDegrees = deg, jointAngleDegrees = foldAngle, isReachable = false)
        }

        // Case 3: Target is reachable. Law of Cosines:
        // c^2 = a^2 + b^2 - 2ab * cos(C)
        // cos(angleJoint) = (dist^2 - L1^2 - L2^2) / (2 * L1 * L2)
        val cosJoint = ((distSq - length1 * length1 - length2 * length2) / (2f * length1 * length2)).coerceIn(-1f, 1f)
        val jointAngleRad = acos(cosJoint)

        // cos(angleRootOffset) = (L1^2 + dist^2 - L2^2) / (2 * L1 * dist)
        val cosRootOffset = ((length1 * length1 + distSq - length2 * length2) / (2f * length1 * dist)).coerceIn(-1f, 1f)
        val rootOffsetRad = acos(cosRootOffset)

        val baseAngleRad = atan2(dy, dx)

        val finalRootAngleRad: Float
        val finalJointAngleRad: Float

        if (!flipBend) {
            finalRootAngleRad = baseAngleRad - rootOffsetRad
            finalJointAngleRad = Math.PI.toFloat() - jointAngleRad
        } else {
            finalRootAngleRad = baseAngleRad + rootOffsetRad
            finalJointAngleRad = -(Math.PI.toFloat() - jointAngleRad)
        }

        return IKSolution(
            rootAngleDegrees = Math.toDegrees(finalRootAngleRad.toDouble()).toFloat(),
            jointAngleDegrees = Math.toDegrees(finalJointAngleRad.toDouble()).toFloat(),
            isReachable = true
        )
    }

    /**
     * Calculates the world coordinates of the intermediate joint (elbow/knee).
     */
    fun getMidJointPosition(
        rootX: Float, rootY: Float,
        rootAngleDegrees: Float,
        length1: Float
    ): Pair<Float, Float> {
        val rad = Math.toRadians(rootAngleDegrees.toDouble())
        val midX = rootX + length1 * cos(rad).toFloat()
        val midY = rootY + length1 * sin(rad).toFloat()
        return Pair(midX, midY)
    }
}
