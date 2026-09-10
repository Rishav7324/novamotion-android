package com.novamotion.ui.inspector

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novamotion.core.model.Layer
import com.novamotion.core.model.evaluate
import com.novamotion.ui.components.GlassmorphicCard
import com.novamotion.ui.components.iosSpringClick
import com.novamotion.ui.theme.*

/**
 * Apple iOS Cupertino Pro Property Inspector for NovaMotion.
 * Features Control Center style thick gradient sliders, monospace numeric boxes,
 * tactile Jog Wheel nudge, and Liquid Glass frosted cards.
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
                .background(IosSecondaryBackground)
                .padding(24.dp)
        ) {
            Text(
                text = "Select a layer on the timeline to edit properties",
                color = IosLabelTertiary,
                fontSize = 13.sp
            )
        }
        return
    }

    // Evaluated transform values at current playhead
    val scaleX   = selectedLayer.transform.scaleX.evaluate(currentPlayheadMs)
    val rotation = selectedLayer.transform.rotation.evaluate(currentPlayheadMs)
    val opacity  = selectedLayer.transform.opacity.evaluate(currentPlayheadMs)
    val posX     = selectedLayer.transform.posX.evaluate(currentPlayheadMs)
    val posY     = selectedLayer.transform.posY.evaluate(currentPlayheadMs)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(IosSecondaryBackground)
            .padding(8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // ── 1. Layer Header Glass Pill ──────────────────────────────────────
        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            backgroundColor = IosGlassSurface,
            borderBrush = IosGlassBorder,
            elevation = 3.dp
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Column {
                    Text(
                        text = selectedLayer.name,
                        color = IosLabelPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${selectedLayer.type.label} • ${selectedLayer.durationMs / 1000.0f}s",
                        color = IosLabelSecondary,
                        fontSize = 10.sp
                    )
                }

                // Add VFX Action Pill
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(IosPurple, IosIndigo)
                            )
                        )
                        .border(0.75.dp, Brush.verticalGradient(listOf(Color.White, Color(0x33FFFFFF))), RoundedCornerShape(10.dp))
                        .iosSpringClick { onAddEffectClick() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoFixHigh,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Add VFX",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ── 2. Transform Section: Control Center Sliders & Jog Wheel ─────────
        Text(
            text = "TRANSFORM CONTROLS",
            color = IosLabelSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(start = 4.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))

        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            backgroundColor = IosGlassSurface,
            borderBrush = IosGlassBorder,
            elevation = 4.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    IosControlCenterSlider(
                        name = "Pos X",
                        value = posX,
                        range = -1080f..1080f,
                        unit = "px",
                        onValueChange = { onValueChange("posX", it) }
                    )
                    IosControlCenterSlider(
                        name = "Pos Y",
                        value = posY,
                        range = -1920f..1920f,
                        unit = "px",
                        onValueChange = { onValueChange("posY", it) }
                    )
                    IosControlCenterSlider(
                        name = "Scale",
                        value = scaleX,
                        range = 0.05f..5.0f,
                        unit = "x",
                        onValueChange = { onValueChange("scale", it) }
                    )
                    IosControlCenterSlider(
                        name = "Rotate",
                        value = rotation,
                        range = -360f..360f,
                        unit = "°",
                        onValueChange = { onValueChange("rotation", it) }
                    )
                    IosControlCenterSlider(
                        name = "Opacity",
                        value = opacity.coerceIn(0f, 1f),
                        range = 0f..1f,
                        unit = "%",
                        multiplier = 100f,
                        onValueChange = { onValueChange("opacity", it) }
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Apple Watch Jog Wheel Hub
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    JogWheel(onStep = { delta -> onValueChange("jog", delta) })
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "X Nudge",
                        color = IosLabelSecondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // ── 3. Keyframe Diamond Status ──────────────────────────────────────
        val kfCount = selectedLayer.transform.posX.keyframes.size
        if (kfCount > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 6.dp)
            ) {
                Text(text = "◆", color = IosCyan, fontSize = 11.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$kfCount keyframe${if (kfCount != 1) "s" else ""} on Pos X",
                    color = IosCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // ── 4. Applied Effects Stack ─────────────────────────────────────────
        if (selectedLayer.effects.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "APPLIED EFFECTS (${selectedLayer.effects.size})",
                color = IosLabelSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(start = 4.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))

            selectedLayer.effects.forEach { effect ->
                GlassmorphicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    shape = RoundedCornerShape(10.dp),
                    backgroundColor = IosTertiaryBackground,
                    borderBrush = if (effect.isEnabled) IosActiveGlowBorder else IosGlassBorder,
                    elevation = 3.dp
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = effect.type.displayName,
                                color = if (effect.isEnabled) IosCyan else IosLabelSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (effect.isEnabled) IosCyan.copy(alpha = 0.2f) else Color(0x33000000))
                                    .border(0.5.dp, if (effect.isEnabled) IosCyan else Color(0x26FFFFFF), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = if (effect.isEnabled) "ACTIVE" else "OFF",
                                    color = if (effect.isEnabled) IosCyan else IosLabelTertiary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Effect parameters
                        effect.parameters.values.forEach { param ->
                            Spacer(modifier = Modifier.height(3.dp))
                            IosControlCenterSlider(
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

/**
 * Apple iOS Control Center style thick gradient slider with monospace numeric box.
 */
@Composable
private fun IosControlCenterSlider(
    name: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    unit: String = "",
    multiplier: Float = 1.0f,
    onValueChange: (Float) -> Unit
) {
    var sliderValue by remember(value) { mutableFloatStateOf(value) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = name,
            color = IosLabelSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(44.dp)
        )

        Slider(
            value = sliderValue,
            onValueChange = {
                sliderValue = it
                onValueChange(it)
            },
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = IosIndigo,
                inactiveTrackColor = Color(0x33FFFFFF)
            ),
            modifier = Modifier.weight(1f).height(20.dp)
        )

        // Monospace numeric input display pill
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(40.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Color(0x33000000))
                .border(0.5.dp, Color(0x1AFFFFFF), RoundedCornerShape(5.dp))
        ) {
            val displayNum = sliderValue * multiplier
            Text(
                text = String.format("%.1f", displayNum) + unit,
                color = IosLabelPrimary,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
