package com.novamotion.ui.curve

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novamotion.core.animation.EasingType
import com.novamotion.core.model.BezierControlPoints
import com.novamotion.ui.theme.*

@Composable
fun BezierGraphEditor(
    curve: BezierControlPoints,
    onCurveChanged: (BezierControlPoints) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var p1 by remember(curve) { mutableStateOf(Offset(curve.x1, curve.y1)) }
    var p2 by remember(curve) { mutableStateOf(Offset(curve.x2, curve.y2)) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioSurface)
            .border(1.dp, StudioBorder)
            .padding(12.dp)
    ) {
        // Top Header
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Bézier Timing Curve (Speed / Value)",
                color = TextPrimary,
                fontSize = 13.sp
            )
            IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Curve",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Interactive Bézier Curve Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(StudioBackground, RoundedCornerShape(8.dp))
                .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            // Simple interactive handle modification
                            val newX = (p2.x + dragAmount.x / size.width).coerceIn(0f, 1f)
                            val newY = (p2.y - dragAmount.y / size.height).coerceIn(-0.5f, 1.5f)
                            p2 = Offset(newX, newY)
                            onCurveChanged(BezierControlPoints(p1.x, p1.y, p2.x, p2.y))
                        }
                    }
            ) {
                val w = size.width
                val h = size.height

                // Draw grid lines
                drawLine(Color(0xFF282E40), Offset(0f, h), Offset(w, 0f), strokeWidth = 1f)

                // Tangent handle lines
                val start = Offset(0f, h)
                val end = Offset(w, 0f)
                val handle1 = Offset(p1.x * w, h - (p1.y * h))
                val handle2 = Offset(p2.x * w, h - (p2.y * h))

                drawLine(ElectricIndigo.copy(alpha = 0.5f), start, handle1, strokeWidth = 2f)
                drawLine(NeonCyan.copy(alpha = 0.5f), end, handle2, strokeWidth = 2f)

                drawCircle(ElectricIndigo, radius = 6.dp.toPx(), center = handle1)
                drawCircle(NeonCyan, radius = 6.dp.toPx(), center = handle2)

                // Draw Bézier Curve
                val path = Path().apply {
                    moveTo(start.x, start.y)
                    cubicTo(handle1.x, handle1.y, handle2.x, handle2.y, end.x, end.y)
                }

                drawPath(
                    path = path,
                    color = NeonCyan,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 1-Tap Easing Preset Buttons Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (preset in EasingType.values()) {
                Box(
                    modifier = Modifier
                        .background(StudioSurfaceVariant, RoundedCornerShape(6.dp))
                        .border(0.5.dp, StudioBorder, RoundedCornerShape(6.dp))
                        .clickable {
                            p1 = Offset(preset.curve.x1, preset.curve.y1)
                            p2 = Offset(preset.curve.x2, preset.curve.y2)
                            onCurveChanged(preset.curve)
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = preset.title,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
