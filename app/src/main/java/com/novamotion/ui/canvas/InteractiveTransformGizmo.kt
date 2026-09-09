package com.novamotion.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

@Composable
fun InteractiveTransformGizmo(
    width: Float = 240f,
    height: Float = 240f,
    rotation: Float = 0f,
    onScale: (scaleFactor: Float) -> Unit,
    onRotate: (deltaAngle: Float) -> Unit,
    onTranslate: (dx: Float, dy: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width.dp, height.dp)
            .border(1.5.dp, ElectricIndigo, RoundedCornerShape(4.dp))
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onTranslate(dragAmount.x, dragAmount.y)
                }
            }
    ) {
        // Center Anchor Pivot (+)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(16.dp)
                .align(Alignment.Center)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val c = Offset(size.width / 2, size.height / 2)
                drawLine(NeonCyan, Offset(c.x - 8.dp.toPx(), c.y), Offset(c.x + 8.dp.toPx(), c.y), strokeWidth = 2.dp.toPx())
                drawLine(NeonCyan, Offset(c.x, c.y - 8.dp.toPx()), Offset(c.x, c.y + 8.dp.toPx()), strokeWidth = 2.dp.toPx())
                drawCircle(NeonCyan, radius = 3.dp.toPx(), center = c)
            }
        }

        // 4 Corner Scale Pins (Uniform Scale)
        CornerHandle(Alignment.TopStart, onScale)
        CornerHandle(Alignment.TopEnd, onScale)
        CornerHandle(Alignment.BottomStart, onScale)
        CornerHandle(Alignment.BottomEnd, onScale)

        // Top Rotation Dial Handle
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-36).dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(NeonCyan)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onRotate(dragAmount.x * 0.5f)
                        }
                    }
            ) {
                Text(text = "↻", color = StudioBackground, fontSize = 14.sp)
            }

            Box(
                modifier = Modifier
                    .width(1.5.dp)
                    .height(12.dp)
                    .background(NeonCyan)
            )
        }
    }
}

@Composable
private fun BoxScope.CornerHandle(
    alignment: Alignment,
    onScale: (Float) -> Unit
) {
    Box(
        modifier = Modifier
            .size(14.dp)
            .align(alignment)
            .background(NeonCyan, RoundedCornerShape(2.dp))
            .border(1.dp, StudioBackground, RoundedCornerShape(2.dp))
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val delta = dragAmount.x + dragAmount.y
                    onScale(1.0f + delta * 0.01f)
                }
            }
    )
}
