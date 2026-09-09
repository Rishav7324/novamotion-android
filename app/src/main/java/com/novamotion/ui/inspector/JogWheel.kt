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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novamotion.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * Virtual Thumb Jog Wheel for precision 1-frame or 0.1 degree adjustments.
 */
@Composable
fun JogWheel(
    onStep: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var angle by remember { mutableStateOf(0f) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(70.dp)
            .clip(CircleShape)
            .background(StudioSurfaceVariant)
            .border(1.5.dp, StudioBorder, CircleShape)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val delta = dragAmount.x + dragAmount.y
                    angle += delta * 2f
                    onStep(delta)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.width / 2

            // Draw notches
            for (i in 0 until 12) {
                val tickAngle = Math.toRadians((i * 30 + angle).toDouble())
                val start = Offset(
                    center.x + (radius - 8.dp.toPx()) * cos(tickAngle).toFloat(),
                    center.y + (radius - 8.dp.toPx()) * sin(tickAngle).toFloat()
                )
                val end = Offset(
                    center.x + radius * cos(tickAngle).toFloat(),
                    center.y + radius * sin(tickAngle).toFloat()
                )
                drawLine(
                    color = if (i % 3 == 0) NeonCyan else TextMuted,
                    start = start,
                    end = end,
                    strokeWidth = if (i % 3 == 0) 2.dp.toPx() else 1.dp.toPx()
                )
            }
        }

        Text(text = "JOG", color = TextMuted, fontSize = 9.sp)
    }
}
