package com.novamotion.ui.inspector

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novamotion.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * Apple Watch Digital Crown inspired Tactile Brushed Glass Jog Wheel.
 * Provides micro-nudge adjustments (0.1 unit steps) with tactile haptic feedback.
 */
@Composable
fun JogWheel(
    onStep: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var angle by remember { mutableFloatStateOf(0f) }
    val haptic = LocalHapticFeedback.current
    var lastTickAngle by remember { mutableFloatStateOf(0f) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(64.dp)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF2C2C2E),
                        Color(0xFF1C1C1E)
                    )
                )
            )
            .border(1.25.dp, IosGlassBorder, CircleShape)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val delta = dragAmount.x + dragAmount.y
                    angle += delta * 2.2f
                    onStep(delta)

                    // Trigger tactile haptic feedback every 30 degrees of rotation
                    if (kotlin.math.abs(angle - lastTickAngle) >= 25f) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        lastTickAngle = angle
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.width / 2

            // Draw radial Digital Crown notches
            for (i in 0 until 16) {
                val tickAngle = Math.toRadians((i * 22.5 + angle).toDouble())
                val isMajor = i % 4 == 0
                val tickLength = if (isMajor) 9.dp.toPx() else 5.dp.toPx()

                val start = Offset(
                    center.x + (radius - tickLength) * cos(tickAngle).toFloat(),
                    center.y + (radius - tickLength) * sin(tickAngle).toFloat()
                )
                val end = Offset(
                    center.x + radius * cos(tickAngle).toFloat(),
                    center.y + radius * sin(tickAngle).toFloat()
                )

                drawLine(
                    color = if (isMajor) IosCyan else Color(0x66FFFFFF),
                    start = start,
                    end = end,
                    strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()
                )
            }
        }

        // Center glass hub
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(0x66141416))
                .border(0.75.dp, IosGlassBorder, CircleShape)
        ) {
            Text(
                text = "JOG",
                color = IosCyan,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
