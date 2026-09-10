package com.novamotion.ui.text

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novamotion.core.text.KineticTextPreset
import com.novamotion.core.text.KineticTextStyle
import com.novamotion.ui.components.GlassmorphicCard
import com.novamotion.ui.theme.*

/**
 * Apple iOS Cupertino Typography & Kinetic Text Inspector.
 */
@Composable
fun TextInspector(
    textStyle: KineticTextStyle,
    onTextStyleChanged: (KineticTextStyle) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(IosSecondaryBackground)
            .padding(14.dp)
    ) {
        Text(
            text = "KINETIC TYPOGRAPHY",
            color = IosLabelSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Text Content Card
        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            backgroundColor = IosGlassSurface,
            borderBrush = IosGlassBorder,
            elevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                OutlinedTextField(
                    value = textStyle.text,
                    onValueChange = { onTextStyleChanged(textStyle.copy(text = it)) },
                    label = { Text("Text Content", color = IosLabelSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = IosLabelPrimary,
                        unfocusedTextColor = IosLabelPrimary,
                        focusedBorderColor = IosOrange,
                        unfocusedBorderColor = Color(0x33FFFFFF)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Font Size Slider with Monospace badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Size",
                        color = IosLabelSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.width(44.dp)
                    )
                    Slider(
                        value = textStyle.fontSize,
                        onValueChange = { onTextStyleChanged(textStyle.copy(fontSize = it)) },
                        valueRange = 24f..120f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = IosOrange,
                            inactiveTrackColor = Color(0x33FFFFFF)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .width(46.dp)
                            .height(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x33000000))
                            .border(0.5.dp, Color(0x1AFFFFFF), RoundedCornerShape(6.dp))
                    ) {
                        Text(
                            text = "${textStyle.fontSize.toInt()}sp",
                            color = IosLabelPrimary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Kinetic Animator Preset Horizontal Row
        Text(
            text = "ANIMATION PRESETS",
            color = IosLabelSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KineticTextPreset.values().forEach { preset ->
                val isSelected = textStyle.animator == preset
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) IosOrange.copy(alpha = 0.25f) else Color(0x331C1C1E)
                        )
                        .border(
                            width = if (isSelected) 1.dp else 0.5.dp,
                            color = if (isSelected) IosOrange else Color(0x26FFFFFF),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onTextStyleChanged(textStyle.copy(animator = preset)) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = preset.displayName,
                        color = if (isSelected) IosOrange else IosLabelSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
