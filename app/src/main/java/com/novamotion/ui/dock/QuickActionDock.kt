package com.novamotion.ui.dock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novamotion.ui.theme.*

@Composable
fun QuickActionDock(
    currentPlayheadMs: Long,
    isPlaying: Boolean,
    isOnKeyframe: Boolean,
    showCurveGraph: Boolean,
    onTogglePlay: () -> Unit,
    onStepFrame: (Int) -> Unit,
    onToggleKeyframe: () -> Unit,
    onCutClip: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleCurveGraph: () -> Unit,
    onOpenEffects: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = StudioSurface,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .border(width = 1.dp, color = StudioBorder)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {
            // Timecode Display & Step buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onStepFrame(-1) }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous Frame",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                val seconds = (currentPlayheadMs / 1000) % 60
                val frames = ((currentPlayheadMs % 1000) * 60 / 1000)
                Text(
                    text = String.format("%02d:%02d", seconds, frames),
                    color = TextPrimary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                IconButton(onClick = { onStepFrame(1) }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Frame",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Central Transport: Play/Pause & Instant Keyframe Diamond
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Play / Pause Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(ElectricIndigo)
                        .clickable { onTogglePlay() }
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Instant Keyframe Diamond Button (◇)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isOnKeyframe) NeonCyan.copy(alpha = 0.2f) else StudioSurfaceVariant)
                        .border(
                            width = 1.dp,
                            color = if (isOnKeyframe) NeonCyan else StudioBorder,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onToggleKeyframe() }
                        .padding(horizontal = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "◇",
                            color = if (isOnKeyframe) NeonCyan else TextSecondary,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isOnKeyframe) "Key" else "+Key",
                            color = if (isOnKeyframe) NeonCyan else TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Quick Actions: Cut, Curve Graph, Effects, Undo
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Cut / Split
                IconButton(onClick = onCutClip, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.Default.ContentCut,
                        contentDescription = "Cut",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Curve Graph Toggle
                IconButton(
                    onClick = onToggleCurveGraph,
                    modifier = Modifier
                        .size(34.dp)
                        .background(
                            if (showCurveGraph) ElectricIndigo.copy(alpha = 0.2f) else Color.Transparent,
                            RoundedCornerShape(6.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.ShowChart,
                        contentDescription = "Curve Graph",
                        tint = if (showCurveGraph) ElectricIndigo else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Effects
                IconButton(onClick = onOpenEffects, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = "Effects",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Undo
                IconButton(onClick = onUndo, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.Default.Undo,
                        contentDescription = "Undo",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
