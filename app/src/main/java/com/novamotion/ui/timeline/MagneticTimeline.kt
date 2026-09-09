package com.novamotion.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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

@Composable
fun MagneticTimeline(
    project: Project,
    currentPlayheadMs: Long,
    selectedLayerId: String?,
    onSelectLayer: (String) -> Unit,
    onSeek: (Long) -> Unit,
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
            // Time Ruler
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .background(StudioSurfaceVariant)
                    .border(width = 0.5.dp, color = StudioBorder)
            ) {
                Row(modifier = Modifier.padding(start = 12.dp)) {
                    val stepSec = 1
                    val maxSec = (project.durationMs / 1000).toInt()
                    for (sec in 0..maxSec step stepSec) {
                        Text(
                            text = String.format("%02d:00", sec),
                            color = TextMuted,
                            fontSize = 10.sp,
                            modifier = Modifier.width((stepSec * 1000 * pxPerMs).dp)
                        )
                    }
                }
            }

            // Layer Track Lanes
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
                        onClick = { onSelectLayer(layer.id) }
                    )
                }
            }
        }

        // Red Playhead Line
        val playheadOffsetDp = (currentPlayheadMs * pxPerMs).dp - scrollState.value.dp
        Box(
            modifier = Modifier
                .offset(x = playheadOffsetDp)
                .width(2.dp)
                .fillMaxHeight()
                .background(PlayheadRed)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newTime = (currentPlayheadMs + (dragAmount.x / pxPerMs)).toLong()
                        onSeek(newTime.coerceIn(0L, project.durationMs))
                    }
                }
        ) {
            // Playhead Cap/Handle
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
    onClick: () -> Unit
) {
    val trackColor = when (layer.type) {
        LayerType.VIDEO -> PurpleVideo
        LayerType.IMAGE -> PurpleVideo.copy(alpha = 0.8f)
        LayerType.TEXT -> AmberText
        LayerType.SHAPE -> NeonCyan
        LayerType.AUDIO -> EmeraldAudio
        LayerType.ADJUSTMENT -> OrangeAdjustment
        LayerType.NULL_OBJECT -> TextSecondary
    }

    val startOffsetDp = (layer.startTimeMs * pxPerMs).dp
    val clipWidthDp = (layer.durationMs * pxPerMs).dp.coerceAtLeast(30.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
    ) {
        Box(
            modifier = Modifier
                .offset(x = startOffsetDp)
                .width(clipWidthDp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(6.dp))
                .background(trackColor.copy(alpha = if (isSelected) 0.35f else 0.2f))
                .border(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) trackColor else trackColor.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(6.dp)
                )
                .clickable { onClick() }
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = layer.name,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    maxLines = 1
                )

                // Keyframe Indicators on clip
                if (layer.transform.posX.hasKeyframes()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (k in layer.transform.posX.keyframes) {
                            Text(text = "◇", color = NeonCyan, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}
