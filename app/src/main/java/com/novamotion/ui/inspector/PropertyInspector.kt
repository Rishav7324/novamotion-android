package com.novamotion.ui.inspector

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novamotion.core.model.Layer
import com.novamotion.core.model.evaluate
import com.novamotion.ui.theme.*

/**
 * Production PropertyInspector.
 *
 * Previous version had hardcoded slider values (value = 1.0f / 0f).
 * This version reads the actual evaluated transform values at currentPlayheadMs
 * and displays them correctly.
 */
@Composable
fun PropertyInspector(
    selectedLayer: Layer?,
    currentPlayheadMs: Long,
    onValueChange: (property: String, newValue: Float) -> Unit,
    onAddEffectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selectedLayer == null) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .fillMaxWidth()
                .background(StudioSurface)
                .padding(24.dp)
        ) {
            Text(text = "Select a layer to edit properties", color = TextMuted, fontSize = 13.sp)
        }
        return
    }

    // Evaluate actual transform values at the current playhead position
    val scaleX   = selectedLayer.transform.scaleX.evaluate(currentPlayheadMs)
    val rotation = selectedLayer.transform.rotation.evaluate(currentPlayheadMs)
    val opacity  = selectedLayer.transform.opacity.evaluate(currentPlayheadMs)
    val posX     = selectedLayer.transform.posX.evaluate(currentPlayheadMs)
    val posY     = selectedLayer.transform.posY.evaluate(currentPlayheadMs)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioSurface)
            .border(1.dp, StudioBorder)
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // ── Layer Header ──────────────────────────────────────────────────────
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = selectedLayer.name,
                    color = TextPrimary,
                    fontSize = 14.sp
                )
                Text(
                    text = "${selectedLayer.type.label} • ${selectedLayer.durationMs / 1000.0f}s",
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
            Button(
                onClick = onAddEffectClick,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Add VFX", fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = StudioBorder.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(12.dp))

        // ── Transform Section ─────────────────────────────────────────────────
        Text(text = "Transform", color = TextSecondary, fontSize = 11.sp, style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Position X
                PropertySlider(
                    name = "Pos X",
                    value = posX,
                    range = -1080f..1080f,
                    onValueChange = { newVal ->
                        // Inline position edit via a separate "posX" property key
                        onValueChange("posX", newVal)
                    }
                )
                // Position Y
                PropertySlider(
                    name = "Pos Y",
                    value = posY,
                    range = -1920f..1920f,
                    onValueChange = { onValueChange("posY", it) }
                )
                // Scale (uniform)
                PropertySlider(
                    name = "Scale",
                    value = scaleX,
                    range = 0.05f..5.0f,
                    onValueChange = { onValueChange("scale", it) }
                )
                // Rotation
                PropertySlider(
                    name = "Rotation",
                    value = rotation,
                    range = -360f..360f,
                    onValueChange = { onValueChange("rotation", it) }
                )
                // Opacity
                PropertySlider(
                    name = "Opacity",
                    value = opacity.coerceIn(0f, 1f),
                    range = 0f..1f,
                    onValueChange = { onValueChange("opacity", it) }
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Precision Jog Wheel
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                JogWheel(onStep = { delta -> onValueChange("jog", delta) })
                Text(text = "X Nudge", color = TextMuted, fontSize = 10.sp)
            }
        }

        // ── Keyframe Info ─────────────────────────────────────────────────────
        val kfCount = selectedLayer.transform.posX.keyframes.size
        if (kfCount > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "◇ $kfCount keyframe${if (kfCount != 1) "s" else ""} on Pos X", color = NeonCyan, fontSize = 11.sp)
            }
        }

        // ── Applied Effects Stack ─────────────────────────────────────────────
        if (selectedLayer.effects.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = StudioBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Applied Effects (${selectedLayer.effects.size})",
                color = TextSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            selectedLayer.effects.forEach { effect ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = effect.type.displayName,
                                color = if (effect.isEnabled) NeonCyan else TextMuted,
                                fontSize = 12.sp,
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = if (effect.isEnabled) "ON" else "OFF",
                                color = if (effect.isEnabled) NeonCyan else TextMuted,
                                fontSize = 10.sp
                            )
                        }

                        // Effect parameters
                        effect.parameters.values.forEach { param ->
                            Spacer(modifier = Modifier.height(4.dp))
                            PropertySlider(
                                name = param.name,
                                value = param.value,
                                range = param.min..param.max,
                                onValueChange = { newVal ->
                                    onValueChange("${effect.id}_${param.key}", newVal)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PropertySlider(
    name: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    // Keep a local slider state to avoid recomposition jank on fast drags
    var sliderValue by remember(value) { mutableFloatStateOf(value) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = name, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(55.dp))
        Slider(
            value = sliderValue,
            onValueChange = {
                sliderValue = it
                onValueChange(it)
            },
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = NeonCyan,
                activeTrackColor = ElectricIndigo,
                inactiveTrackColor = StudioSurfaceVariant
            ),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = String.format("%.1f", sliderValue),
            color = TextMuted,
            fontSize = 10.sp,
            modifier = Modifier.width(36.dp)
        )
    }
}
