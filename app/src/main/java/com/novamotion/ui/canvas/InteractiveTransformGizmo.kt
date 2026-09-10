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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novamotion.ui.theme.*

/**
 * Ultra-refined iOS Liquid Glass Selection & Transform Gizmo.
 * Features specular gradient hairline boundaries, circular glass scale pins,
 * an extended rotation stalk dial, and an animated center anchor pivot.
 */
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
            .border(
                width = 1.25.dp,
                brush = IosActiveGlowBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onTranslate(dragAmount.x, dragAmount.y)
                }
            }
    ) {
        // ── 1. Center Anchor Pivot (+) with Subtle Glowing Reticle ───────
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(20.dp)
                .align(Alignment.Center)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val c = Offset(size.width / 2, size.height / 2)
                drawLine(
                    color = IosCyan,
                    start = Offset(c.x - 8.dp.toPx(), c.y),
                    end = Offset(c.x + 8.dp.toPx(), c.y),
                    strokeWidth = 1.5.dp.toPx()
                )
                drawLine(
                    color = IosCyan,
                    start = Offset(c.x, c.y - 8.dp.toPx()),
                    end = Offset(c.x, c.y + 8.dp.toPx()),
                    strokeWidth = 1.5.dp.toPx()
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.5.dp.toPx(),
                    center = c
                )
            }
        }

        // ── 2. 4 Circular Glass Scale Pins (Uniform Diagonal Scale) ──────
        GlassCornerHandle(Alignment.TopStart, onScale)
        GlassCornerHandle(Alignment.TopEnd, onScale)
        GlassCornerHandle(Alignment.BottomStart, onScale)
        GlassCornerHandle(Alignment.BottomEnd, onScale)

        // ── 3. Top Rotation Stalk Handle ────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-40).dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(IosCyan, IosIndigo)
                        )
                    )
                    .border(1.dp, Brush.verticalGradient(listOf(Color.White, Color(0x66FFFFFF))), CircleShape)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onRotate(dragAmount.x * 0.5f)
                        }
                    }
            ) {
                Text(
                    text = "↻",
                    color = Color.White,
                    fontSize = 13.sp
                )
            }

            Box(
                modifier = Modifier
                    .width(1.5.dp)
                    .height(14.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(IosCyan, Color.Transparent)
                        )
                    )
            )
        }
    }
}

@Composable
private fun BoxScope.GlassCornerHandle(
    alignment: Alignment,
    onScale: (Float) -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(16.dp)
            .align(alignment)
            .clip(CircleShape)
            .background(Color.White)
            .border(1.5.dp, IosIndigo, CircleShape)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val delta = dragAmount.x + dragAmount.y
                    onScale(1.0f + delta * 0.01f)
                }
            }
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(IosIndigo, CircleShape)
        )
    }
}
