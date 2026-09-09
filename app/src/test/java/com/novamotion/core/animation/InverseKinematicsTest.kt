package com.novamotion.core.animation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InverseKinematicsTest {

    @Test
    fun testTwoBoneReachable() {
        val solution = InverseKinematicsSolver.solve2Bone(
            rootX = 0f, rootY = 0f,
            targetX = 150f, targetY = 0f,
            length1 = 100f, length2 = 100f,
            flipBend = false
        )

        assertTrue(solution.isReachable)
        // Joint angle should be non-zero for bent arm
        assertTrue(solution.jointAngleDegrees > 0f)
    }

    @Test
    fun testTwoBoneUnreachableTarget() {
        val solution = InverseKinematicsSolver.solve2Bone(
            rootX = 0f, rootY = 0f,
            targetX = 300f, targetY = 0f,
            length1 = 100f, length2 = 100f, // Max reach is 200
            flipBend = false
        )

        // Beyond reach
        assertEquals(false, solution.isReachable)
        assertEquals(0f, solution.jointAngleDegrees, 0.001f) // Fully straightened
    }
}
