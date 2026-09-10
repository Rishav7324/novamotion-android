package com.novamotion.ui.curve

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novamotion.core.animation.EasingType
import com.novamotion.core.model.BezierControlPoints
import com.novamotion.ui.components.GlassmorphicCard
import com.novamotion.ui.theme.*

/**
 * Apple iOS Cupertino Bézier Timing Curve Editor.
 * Interactive speed/value curve editor with tangent handles and 1-tap easing presets.
 */
@Composable
fun BezierGraphEditor(
    curve: BezierControlPoints,
    onCurveChanged: (BezierControlPoints) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var p1 by remember(curve) { mutableStateOf(Offset(curve.x1, curve.y1)) }
    var p2 by remember(curve) { mutableStateOf(Offset(curve.x2, curve.y2)) }
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(IosSecondaryBackground)
            .padding(14.dp)
    ) {
        // Top Header
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "BÉZIER TIMING CURVE",
                color = IosLabelSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Curve",
                    tint = IosLabelSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Interactive Bézier Curve Glass Canvas
        GlassmorphicCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            shape = RoundedCornerShape(16.dp),
            backgroundColor = IosSystemBackground,
            borderBrush = IosGlassBorder,
            elevation = 6.dp
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val newX = (p2.x + dragAmount.x / size.width).coerceIn(0f, 1f)
                            val newY = (p2.y - dragAmount.y / size.height).coerceIn(-0.5f, 1.5f)
                            p2 = Offset(newX, newY)
                            onCurveChanged(BezierControlPoints(p1.x, p1.y, p2.x, p2.y))
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    }
            ) {
                val w = size.width
                val h = size.height

                // Subtle Grid lines
                drawLine(Color(0x1AFFFFFF), Offset(0f, h), Offset(w, 0f), strokeWidth = 1f)
                drawLine(Color(0x0DFFFFFF), Offset(0f, h / 2f), Offset(w, h / 2f), strokeWidth = 1f)
                drawLine(Color(0x0DFFFFFF), Offset(w / 2f, 0f), Offset(w / 2f, h), strokeWidth = 1f)

                // Tangent handle lines
                val start = Offset(0f, h)
                val end = Offset(w, 0f)
                val handle1 = Offset(p1.x * w, h - (p1.y * h))
                val handle2 = Offset(p2.x * w, h - (p2.y * h))

                drawLine(IosPurple.copy(alpha = 0.6f), start, handle1, strokeWidth = 1.5.dp.toPx())
                drawLine(IosCyan.copy(alpha = 0.6f), end, handle2, strokeWidth = 1.5.dp.toPx())

                // Tangent control pins
                drawCircle(IosPurple, radius = 7.dp.toPx(), center = handle1)
                drawCircle(Color.White, radius = 3.dp.toPx(), center = handle1)

                drawCircle(IosCyan, radius = 7.dp.toPx(), center = handle2)
                drawCircle(Color.White, radius = 3.dp.toPx(), center = handle2)

                // Draw Glowing Bézier Curve
                val path = Path().apply {
                    moveTo(start.x, start.y)
                    cubicTo(handle1.x, handle1.y, handle2.x, handle2.y, end.x, end.y)
                }

                drawPath(
                    path = path,
                    color = IosCyan,
                    style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 1-Tap Easing Preset Buttons
        Text(
            text = "EASING PRESETS",
            color = IosLabelSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (preset in EasingType.values()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x331C1C1E))
                        .border(0.5.dp, Color(0x26FFFFFF), RoundedCornerShape(10.dp))
                        .clickable {
                            p1 = Offset(preset.curve.x1, preset.curve.y1)
                            p2 = Offset(preset.curve.x2, preset.curve.y2)
                            onCurveChanged(preset.curve)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = preset.title,
                        color = IosLabelPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
