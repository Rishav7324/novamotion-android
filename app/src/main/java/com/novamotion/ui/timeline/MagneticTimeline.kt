package com.novamotion.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novamotion.core.model.Layer
import com.novamotion.core.model.LayerType
import com.novamotion.core.model.Project
import com.novamotion.ui.components.GlassmorphicCard
import com.novamotion.ui.theme.*

/**
 * Apple iOS Cupertino Pro Magnetic Timeline for NovaMotion Studio.
 * Multi-track NLE timeline with frosted glass track headers,
 * squircle clip blocks, real/smooth waveform visuals, and tactile magnetic scrub.
 */
@Composable
fun MagneticTimeline(
    project: Project,
    currentPlayheadMs: Long,
    selectedLayerId: String?,
    onSelectLayer: (String) -> Unit,
    onSeek: (Long) -> Unit,
    onLayerMoved: ((layerId: String, newStartMs: Long) -> Unit)? = null,
    onLayerTrimmed: ((layerId: String, newDurationMs: Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val pxPerMs = 0.1f // 100px per second
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(IosSystemBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(scrollState)
        ) {
            // ── 1. Frosted Glass Time Ruler ──────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(Color(0x66141416))
                    .border(width = 0.5.dp, brush = IosGlassBorder, shape = RectangleShape)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 12.dp)
                ) {
                    val stepSec = 1
                    val maxSec = (project.durationMs / 1000).toInt().coerceAtLeast(1)
                    for (sec in 0..maxSec step stepSec) {
                        val label = String.format("%02d:%02d", sec / 60, sec % 60)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.width((stepSec * 1000 * pxPerMs).dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(10.dp)
                                    .background(Color(0x33FFFFFF))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = label,
                                color = IosLabelTertiary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // ── 2. Layer Track Lanes ─────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (layer in project.layers) {
                    TimelineLayerTrack(
                        layer = layer,
                        isSelected = layer.id == selectedLayerId,
                        pxPerMs = pxPerMs,
                        projectDurationMs = project.durationMs,
                        onClick = { onSelectLayer(layer.id) },
                        onLayerMoved = { newStartMs ->
                            onLayerMoved?.invoke(layer.id, newStartMs)
                        },
                        onLayerTrimmed = { newDurationMs ->
                            onLayerTrimmed?.invoke(layer.id, newDurationMs)
                        }
                    )
                }
            }
        }

        // ── 3. Apple Neon Red Magnetic Playhead Needle ───────────────────────
        val playheadOffsetDp = (currentPlayheadMs * pxPerMs).dp
        Box(
            modifier = Modifier
                .offset(x = playheadOffsetDp)
                .width(2.dp)
                .fillMaxHeight()
                .background(
                    Brush.verticalGradient(
                        listOf(IosRed, IosRed.copy(alpha = 0.85f))
                    )
                )
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val deltaMs = (dragAmount.x / pxPerMs).toLong()
                        val newTime = (currentPlayheadMs + deltaMs).coerceIn(0L, project.durationMs)
                        onSeek(newTime)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                }
        ) {
            // Playhead Teardrop Glass Cap Handle
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .offset(x = (-6).dp, y = 0.dp)
                    .clip(CircleShape)
                    .background(IosRed)
                    .border(1.dp, Color.White, CircleShape)
            )
        }
    }
}

@Composable
private fun TimelineLayerTrack(
    layer: Layer,
    isSelected: Boolean,
    pxPerMs: Float,
    projectDurationMs: Long,
    onClick: () -> Unit,
    onLayerMoved: (newStartMs: Long) -> Unit,
    onLayerTrimmed: (newDurationMs: Long) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val trackAccent = when (layer.type) {
        LayerType.VIDEO       -> IosIndigo
        LayerType.IMAGE       -> IosPurple
        LayerType.TEXT        -> IosOrange
        LayerType.SHAPE       -> IosCyan
        LayerType.AUDIO       -> IosGreen
        LayerType.ADJUSTMENT  -> IosMint
        LayerType.NULL_OBJECT -> IosLabelSecondary
    }

    val startOffsetDp = (layer.startTimeMs * pxPerMs).dp
    val clipWidthDp   = (layer.durationMs * pxPerMs).dp.coerceAtLeast(40.dp)

    // Accumulated drag delta for clip movement (in pixels)
    var dragAccumPx by remember(layer.id) { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
    ) {
        // ── Clip Body (iOS Liquid Glass Squircle Card) ───────────────────────
        Box(
            modifier = Modifier
                .offset(x = startOffsetDp + dragAccumPx.dp)
                .width(clipWidthDp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (isSelected) trackAccent.copy(alpha = 0.35f) else Color(0x331C1C1E)
                )
                .border(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    brush = if (isSelected) IosActiveGlowBorder else Brush.verticalGradient(
                        listOf(trackAccent.copy(alpha = 0.7f), Color(0x1AFFFFFF))
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
                .clickable { onClick() }
                // Horizontal drag to reposition clip temporally
                .pointerInput(layer.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val deltaPx = dragAccumPx
                            dragAccumPx = 0f
                            val deltaMs = (deltaPx / pxPerMs).toLong()
                            val newStart = (layer.startTimeMs + deltaMs).coerceIn(0L, projectDurationMs - layer.durationMs)
                            onLayerMoved(newStart)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onClick()
                        },
                        onDragCancel = {
                            dragAccumPx = 0f
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        dragAccumPx += dragAmount
                    }
                }
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            // Audio track visual waveform gradient
            if (layer.type == LayerType.AUDIO) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val barSpacing = 4.dp.toPx()
                    val barWidth = 2.dp.toPx()
                    val numBars = (size.width / barSpacing).toInt()
                    val midY = size.height / 2f
                    for (i in 0 until numBars) {
                        val norm = kotlin.math.sin(i * 0.35f) * 0.5f + kotlin.math.cos(i * 0.18f) * 0.4f
                        val barHeight = (size.height * 0.75f * kotlin.math.abs(norm)).coerceAtLeast(4f)
                        drawRect(
                            color = IosGreen.copy(alpha = 0.55f),
                            topLeft = androidx.compose.ui.geometry.Offset(i * barSpacing, midY - barHeight / 2f),
                            size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Layer name + type icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val icon = when (layer.type) {
                        LayerType.VIDEO  -> Icons.Default.Videocam
                        LayerType.IMAGE  -> Icons.Default.Image
                        LayerType.TEXT   -> Icons.Default.TextFields
                        LayerType.SHAPE  -> Icons.Default.Category
                        LayerType.AUDIO  -> Icons.Default.MusicNote
                        else             -> Icons.Default.Layers
                    }
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = trackAccent,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = layer.name,
                        color = IosLabelPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }

                // Keyframe count badge
                val kfCount = layer.transform.posX.keyframes.size
                if (kfCount > 0) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(IosCyan.copy(alpha = 0.2f))
                            .border(0.5.dp, IosCyan, RoundedCornerShape(6.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "◆$kfCount",
                            color = IosCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ── Right Edge Trim Handle ───────────────────────────────────────────
        if (isSelected) {
            var trimDragPx by remember(layer.id) { mutableFloatStateOf(0f) }
            Box(
                modifier = Modifier
                    .offset(
                        x = startOffsetDp + clipWidthDp - 12.dp + trimDragPx.dp,
                        y = 4.dp
                    )
                    .width(12.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                    .background(Color.White)
                    .border(1.dp, trackAccent, RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                    .pointerInput(layer.id) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                val deltaMs = (trimDragPx / pxPerMs).toLong()
                                trimDragPx = 0f
                                val newDuration = (layer.durationMs + deltaMs).coerceAtLeast(500L)
                                onLayerTrimmed(newDuration)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDragCancel = { trimDragPx = 0f }
                        ) { change, dragAmount ->
                            change.consume()
                            trimDragPx += dragAmount
                        }
                    }
            )
        }
    }
}
