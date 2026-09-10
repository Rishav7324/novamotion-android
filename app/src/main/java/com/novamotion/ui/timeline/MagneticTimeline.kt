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
    onLayerTrimHead: ((layerId: String, newStartMs: Long, newDurationMs: Long) -> Unit)? = null,
    pxPerMs: Float = 0.1f,
    snapEnabled: Boolean = true,
    showWaveforms: Boolean = true,
    onZoomChange: ((Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
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
                    .height(20.dp)
                    .background(Color(0x66141416))
                    .border(width = 0.5.dp, brush = IosGlassBorder, shape = RectangleShape)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 8.dp)
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
                                    .height(7.dp)
                                    .background(Color(0x33FFFFFF))
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = label,
                                color = IosLabelTertiary,
                                fontSize = 8.sp,
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
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
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
                        },
                        onLayerTrimHead = { newStartMs, newDurationMs ->
                            onLayerTrimHead?.invoke(layer.id, newStartMs, newDurationMs)
                        }
                    )
                }
            }
        }

        // ── 3. Apple Neon Red Magnetic Playhead Needle (inside scroll, accounts for scroll offset) ───────
        val playheadOffsetPx = currentPlayheadMs * pxPerMs
        val scrollOffsetPx = scrollState.value.toFloat()
        val playheadVisibleX = (playheadOffsetPx - scrollOffsetPx).dp
        Box(
            modifier = Modifier
                .offset(x = playheadVisibleX)
                .width(2.dp)
                .fillMaxHeight()
                .background(
                    Brush.verticalGradient(
                        listOf(IosRed, IosRed.copy(alpha = 0.85f))
                    )
                )
                .pointerInput(currentPlayheadMs, project.durationMs, snapEnabled, pxPerMs) {
                    var accMs = currentPlayheadMs
                    detectDragGestures(
                        onDragStart = { accMs = currentPlayheadMs },
                        onDragEnd = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                    ) { change, dragAmount ->
                        change.consume()
                        val deltaMs = (dragAmount.x / pxPerMs).toLong()
                        accMs = (accMs + deltaMs).coerceIn(0L, project.durationMs)
                        var snapped = accMs
                        if (snapEnabled) {
                            val snapWindowMs = (8.dp.toPx() / pxPerMs).toLong()
                            for (layer in project.layers) {
                                if (kotlin.math.abs(accMs - layer.startTimeMs) < snapWindowMs) snapped = layer.startTimeMs
                                if (kotlin.math.abs(accMs - layer.endTimeMs) < snapWindowMs) snapped = layer.endTimeMs
                            }
                        }
                        onSeek(snapped)
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
    onLayerTrimmed: (newDurationMs: Long) -> Unit,
    onLayerTrimHead: (newStartMs: Long, newDurationMs: Long) -> Unit
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
            .height(34.dp)
    ) {
        // ── Clip Body (iOS Liquid Glass Squircle Card) ───────────────────────
        Box(
            modifier = Modifier
                .offset(x = startOffsetDp + dragAccumPx.dp)
                .width(clipWidthDp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(7.dp))
                .background(
                    if (isSelected) trackAccent.copy(alpha = 0.35f) else Color(0x331C1C1E)
                )
                .border(
                    width = if (isSelected) 1.dp else 0.75.dp,
                    brush = if (isSelected) IosActiveGlowBorder else Brush.verticalGradient(
                        listOf(trackAccent.copy(alpha = 0.7f), Color(0x1AFFFFFF))
                    ),
                    shape = RoundedCornerShape(7.dp)
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
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            // Audio track visual placeholder — real waveform via WaveformExtractor when available
            if (showWaveforms && layer.type == LayerType.AUDIO) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "AUDIO",
                        color = IosGreen.copy(alpha = 0.5f),
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Layer name + type icon (+ HOLD indicator)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = layer.name,
                        color = IosLabelPrimary,
                        fontSize = 10.sp,
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
                            .padding(horizontal = 3.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "◆$kfCount",
                            color = IosCyan,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ── Left Edge Head-Trim Handle + Right Edge Tail Handle ────────────────
        if (isSelected) {
            // Left (head) handle: trims in-point, moves start forward/backward
            var headDragPx by remember(layer.id) { mutableFloatStateOf(0f) }
            Box(
                modifier = Modifier
                    .offset(x = startOffsetDp + headDragPx.dp, y = 3.dp)
                    .width(9.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp))
                    .background(Color.White.copy(alpha = 0.92f))
                    .border(1.dp, trackAccent, RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp))
                    .pointerInput(layer.id) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                val deltaMs = (headDragPx / pxPerMs).toLong()
                                headDragPx = 0f
                                val newStart = (layer.startTimeMs + deltaMs).coerceIn(0L, layer.endTimeMs - 500L)
                                val newDuration = (layer.durationMs - (newStart - layer.startTimeMs)).coerceAtLeast(500L)
                                onLayerTrimHead(newStart, newDuration)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDragCancel = { headDragPx = 0f }
                        ) { change, dragAmount ->
                            change.consume()
                            headDragPx += dragAmount
                        }
                    }
            )
            // Right (tail) handle
            var tailDragPx by remember(layer.id) { mutableFloatStateOf(0f) }
            Box(
                modifier = Modifier
                    .offset(
                        x = startOffsetDp + clipWidthDp - 9.dp + tailDragPx.dp,
                        y = 3.dp
                    )
                    .width(9.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp))
                    .background(Color.White)
                    .border(1.dp, trackAccent, RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp))
                    .pointerInput(layer.id) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                val deltaMs = (tailDragPx / pxPerMs).toLong()
                                tailDragPx = 0f
                                val newDuration = (layer.durationMs + deltaMs).coerceAtLeast(500L)
                                onLayerTrimmed(newDuration)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDragCancel = { tailDragPx = 0f }
                        ) { change, dragAmount ->
                            change.consume()
                            tailDragPx += dragAmount
                        }
                    }
            )
        }
    }
}
