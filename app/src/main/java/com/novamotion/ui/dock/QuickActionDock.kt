package com.novamotion.ui.dock

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novamotion.ui.components.GlassmorphicCard
import com.novamotion.ui.components.iosSpringClick
import com.novamotion.ui.theme.*

/**
 * Floating Dynamic Island Capsule Dock for NovaMotion Studio.
 * Crafted with Apple iOS Liquid Glass aesthetics, specular highlight rim,
 * responsive spring physics, and tactile haptic feedback.
 */
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
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassmorphicCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(27.dp),
            backgroundColor = IosGlassSurface,
            borderBrush = IosGlassBorder,
            elevation = 12.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp)
            ) {
                // ── 1. Cupertino Timecode Pill & Frame Stepper ────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x33000000))
                        .border(0.5.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onStepFrame(-1)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NavigateBefore,
                            contentDescription = "Previous Frame",
                            tint = IosLabelSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    val seconds = (currentPlayheadMs / 1000) % 60
                    val frames = ((currentPlayheadMs % 1000) * 60 / 1000)
                    Text(
                        text = String.format("%02d:%02d", seconds, frames),
                        color = IosLabelPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onStepFrame(1)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NavigateNext,
                            contentDescription = "Next Frame",
                            tint = IosLabelSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // ── 2. Central Transport: Spring Play/Pause & Dynamic Keyframe Diamond
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Central Circular Play / Pause Button with Spring physics
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(IosPurple, IosIndigo)
                                )
                            )
                            .border(1.dp, Brush.verticalGradient(listOf(Color(0x80FFFFFF), Color(0x26FFFFFF))), CircleShape)
                            .iosSpringClick {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onTogglePlay()
                            }
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Keyframe Diamond Pill (◇ / ◆) with Neon Cyan Glow
                    val diamondBgColor by animateColorAsState(
                        targetValue = if (isOnKeyframe) IosCyan.copy(alpha = 0.22f) else Color(0x33000000),
                        animationSpec = tween(200),
                        label = "diamondBg"
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(diamondBgColor)
                            .border(
                                width = 1.dp,
                                brush = if (isOnKeyframe) IosActiveGlowBorder else IosGlassBorder,
                                shape = RoundedCornerShape(17.dp)
                            )
                            .iosSpringClick {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onToggleKeyframe()
                            }
                            .padding(horizontal = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isOnKeyframe) "◆" else "◇",
                                color = if (isOnKeyframe) IosCyan else IosLabelSecondary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isOnKeyframe) "Key" else "+Key",
                                color = if (isOnKeyframe) IosCyan else IosLabelSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // ── 3. Quick Actions: Split, Curve Graph, VFX, Undo ──────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Cut / Split Button
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onCutClip()
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCut,
                            contentDescription = "Cut",
                            tint = IosLabelSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // Curve Graph Toggle with active frosted glass pill
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleCurveGraph()
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .background(
                                if (showCurveGraph) IosIndigo.copy(alpha = 0.35f) else Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                            .border(
                                width = if (showCurveGraph) 1.dp else 0.dp,
                                brush = if (showCurveGraph) IosGlassBorder else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)),
                                shape = RoundedCornerShape(10.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = "Curve Graph",
                            tint = if (showCurveGraph) IosCyan else IosLabelSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // Effects Browser
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onOpenEffects()
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoFixHigh,
                            contentDescription = "Effects",
                            tint = IosLabelSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // Undo
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onUndo()
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Undo,
                            contentDescription = "Undo",
                            tint = IosLabelTertiary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
    }
}
