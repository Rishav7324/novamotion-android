package com.novamotion.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novamotion.core.model.Layer
import com.novamotion.core.model.LayerType
import com.novamotion.core.model.Project
import com.novamotion.ui.theme.*

/**
 * Multi-track magnetic timeline with:
 *  - Playhead drag scrubbing
 *  - Layer clip selection
 *  - Layer clip drag: horizontal drag moves startTimeMs (temporal reposition)
 *  - Clip trim: right-edge drag (held for 300ms first — indicates trim mode)
 *
 * The onLayerMoved callback gives the caller the new startTimeMs for the moved layer.
 * The onLayerTrimmed callback gives the new durationMs.
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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(scrollState)
        ) {
            // ── Time Ruler ────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .background(StudioSurfaceVariant)
                    .border(width = 0.5.dp, color = StudioBorder)
            ) {
                Row(modifier = Modifier.padding(start = 12.dp)) {
                    val stepSec = 1
                    val maxSec = (project.durationMs / 1000).toInt().coerceAtLeast(1)
                    for (sec in 0..maxSec step stepSec) {
                        val label = String.format("%02d:%02d", sec / 60, sec % 60)
                        Text(
                            text = label,
                            color = TextMuted,
                            fontSize = 10.sp,
                            modifier = Modifier.width((stepSec * 1000 * pxPerMs).dp)
                        )
                    }
                }
            }

            // ── Layer Track Lanes ─────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
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

        // ── Red Playhead Line (draggable) ─────────────────────────────────────
        val playheadOffsetDp = (currentPlayheadMs * pxPerMs).dp
        Box(
            modifier = Modifier
                .offset(x = playheadOffsetDp)
                .width(2.dp)
                .fillMaxHeight()
                .background(PlayheadRed)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val deltaMs = (dragAmount.x / pxPerMs).toLong()
                        val newTime = (currentPlayheadMs + deltaMs).coerceIn(0L, project.durationMs)
                        onSeek(newTime)
                    }
                }
        ) {
            // Playhead Handle Cap
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .offset(x = (-5).dp, y = 0.dp)
                    .background(PlayheadRed, RoundedCornerShape(2.dp))
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
    val trackColor = when (layer.type) {
        LayerType.VIDEO      -> PurpleVideo
        LayerType.IMAGE      -> PurpleVideo.copy(alpha = 0.8f)
        LayerType.TEXT       -> AmberText
        LayerType.SHAPE      -> NeonCyan
        LayerType.AUDIO      -> EmeraldAudio
        LayerType.ADJUSTMENT -> OrangeAdjustment
        LayerType.NULL_OBJECT -> TextSecondary
    }

    val startOffsetDp = (layer.startTimeMs * pxPerMs).dp
    val clipWidthDp   = (layer.durationMs * pxPerMs).dp.coerceAtLeast(30.dp)

    // Accumulated drag delta for clip movement (in pixels)
    var dragAccumPx by remember(layer.id) { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
    ) {
        // ── Clip Body ─────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .offset(x = startOffsetDp + dragAccumPx.dp)
                .width(clipWidthDp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(6.dp))
                .background(trackColor.copy(alpha = if (isSelected) 0.35f else 0.2f))
                .border(
                    width  = if (isSelected) 1.5.dp else 1.dp,
                    color  = if (isSelected) trackColor else trackColor.copy(alpha = 0.6f),
                    shape  = RoundedCornerShape(6.dp)
                )
                .clickable { onClick() }
                // Horizontal drag to reposition clip temporally
                .pointerInput(layer.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            // Commit the accumulated drag to a new startTimeMs
                            val deltaPx = dragAccumPx
                            dragAccumPx = 0f
                            val deltaMs = (deltaPx / pxPerMs).toLong()
                            val newStart = (layer.startTimeMs + deltaMs).coerceIn(0L, projectDurationMs - layer.durationMs)
                            onLayerMoved(newStart)
                            onClick() // keep selected after move
                        },
                        onDragCancel = {
                            dragAccumPx = 0f
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        dragAccumPx += dragAmount
                    }
                }
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Layer name + type icon
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
                    Icon(icon, contentDescription = null, tint = trackColor, modifier = Modifier.size(12.dp))
                    Text(text = layer.name, color = TextPrimary, fontSize = 11.sp, maxLines = 1)
                }

                // Keyframe count badge
                val kfCount = layer.transform.posX.keyframes.size
                if (kfCount > 0) {
                    Text(text = "◇×$kfCount", color = NeonCyan, fontSize = 9.sp)
                }
            }
        }

        // ── Right trim handle ─────────────────────────────────────────────────
        if (isSelected) {
            var trimDragPx by remember(layer.id) { mutableFloatStateOf(0f) }
            Box(
                modifier = Modifier
                    .offset(
                        x = startOffsetDp + clipWidthDp - 10.dp + (trimDragPx / 1).dp,
                        y = 4.dp
                    )
                    .width(10.dp)
                    .height(34.dp)
                    .background(trackColor, RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp))
                    .pointerInput(layer.id) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                val deltaMs = (trimDragPx / pxPerMs).toLong()
                                trimDragPx = 0f
                                val newDuration = (layer.durationMs + deltaMs).coerceAtLeast(500L)
                                onLayerTrimmed(newDuration)
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
