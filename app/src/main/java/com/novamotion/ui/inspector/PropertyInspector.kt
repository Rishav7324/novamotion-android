package com.novamotion.ui.inspector

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novamotion.core.model.Layer
import com.novamotion.ui.theme.*

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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioSurface)
            .border(1.dp, StudioBorder)
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Layer Header
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "${selectedLayer.name} (${selectedLayer.type.label})",
                color = TextPrimary,
                fontSize = 14.sp
            )

            // Add Effect Button
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

        // Transform Controls Row with Jog Wheel
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                PropertySlider(
                    name = "Scale",
                    value = 1.0f,
                    range = 0.1f..3.0f,
                    onValueChange = { onValueChange("scale", it) }
                )
                PropertySlider(
                    name = "Rotation",
                    value = 0f,
                    range = -180f..180f,
                    onValueChange = { onValueChange("rotation", it) }
                )
                PropertySlider(
                    name = "Opacity",
                    value = 1.0f,
                    range = 0f..1f,
                    onValueChange = { onValueChange("opacity", it) }
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Precision Jog Wheel
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                JogWheel(onStep = { delta -> onValueChange("jog", delta) })
                Text(text = "Precision", color = TextMuted, fontSize = 10.sp)
            }
        }

        // Active Effects Stack
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
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = name, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(55.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = NeonCyan,
                activeTrackColor = ElectricIndigo,
                inactiveTrackColor = StudioSurfaceVariant
            ),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = String.format("%.1f", value),
            color = TextMuted,
            fontSize = 11.sp,
            modifier = Modifier.width(32.dp)
        )
    }
}
