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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novamotion.core.text.KineticTextPreset
import com.novamotion.core.text.KineticTextStyle
import com.novamotion.ui.theme.*

@Composable
fun TextInspector(
    textStyle: KineticTextStyle,
    onTextStyleChanged: (KineticTextStyle) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioSurface)
            .padding(12.dp)
    ) {
        Text(
            text = "Kinetic Typography & Animation",
            color = TextPrimary,
            fontSize = 13.sp,
            style = MaterialTheme.typography.titleSmall
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Text Input
        OutlinedTextField(
            value = textStyle.text,
            onValueChange = { onTextStyleChanged(textStyle.copy(text = it)) },
            label = { Text("Text Content") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = AmberText,
                unfocusedBorderColor = StudioBorder
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Kinetic Animator Preset Selector Bar
        Text(text = "Kinetic Animator", color = TextSecondary, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(6.dp))
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
                        .background(
                            if (isSelected) AmberText.copy(alpha = 0.25f) else StudioSurfaceVariant,
                            RoundedCornerShape(6.dp)
                        )
                        .border(
                            width = if (isSelected) 1.dp else 0.dp,
                            color = if (isSelected) AmberText else StudioBorder,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable { onTextStyleChanged(textStyle.copy(animator = preset)) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = preset.displayName,
                        color = if (isSelected) AmberText else TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Font Size Slider
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Size", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(60.dp))
            Slider(
                value = textStyle.fontSize,
                onValueChange = { onTextStyleChanged(textStyle.copy(fontSize = it)) },
                valueRange = 24f..120f,
                colors = SliderDefaults.colors(thumbColor = AmberText, activeTrackColor = AmberText),
                modifier = Modifier.weight(1f)
            )
            Text(text = "${textStyle.fontSize.toInt()}sp", color = TextMuted, fontSize = 11.sp)
        }
    }
}
