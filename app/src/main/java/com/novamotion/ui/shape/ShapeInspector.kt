package com.novamotion.ui.shape

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novamotion.core.shape.MaskMode
import com.novamotion.core.shape.ShapeType
import com.novamotion.core.shape.VectorShapeData
import com.novamotion.ui.theme.*

@Composable
fun ShapeInspector(
    shapeData: VectorShapeData,
    onShapeDataChanged: (VectorShapeData) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioSurface)
            .padding(12.dp)
    ) {
        Text(
            text = "Vector Shape & Mask Properties",
            color = TextPrimary,
            fontSize = 13.sp,
            style = MaterialTheme.typography.titleSmall
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Shape Type Selector Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf(ShapeType.RECTANGLE, ShapeType.CIRCLE, ShapeType.STAR, ShapeType.POLYGON).forEach { type ->
                val isSelected = shapeData.type == type
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isSelected) ElectricIndigo else StudioSurfaceVariant,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { onShapeDataChanged(shapeData.copy(type = type)) }
                        .padding(vertical = 8.dp)
                ) {
                    Text(text = type.displayName.split(" ")[0], color = TextPrimary, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Sliders for Corner Radius / Star Points
        if (shapeData.type == ShapeType.RECTANGLE) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Radius", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(60.dp))
                Slider(
                    value = shapeData.cornerRadius,
                    onValueChange = { onShapeDataChanged(shapeData.copy(cornerRadius = it)) },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = ElectricIndigo),
                    modifier = Modifier.weight(1f)
                )
                Text(text = "${shapeData.cornerRadius.toInt()}px", color = TextMuted, fontSize = 11.sp)
            }
        } else if (shapeData.type == ShapeType.STAR) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Points", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(60.dp))
                Slider(
                    value = shapeData.pointsCount.toFloat(),
                    onValueChange = { onShapeDataChanged(shapeData.copy(pointsCount = it.toInt())) },
                    valueRange = 3f..12f,
                    steps = 8,
                    colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = ElectricIndigo),
                    modifier = Modifier.weight(1f)
                )
                Text(text = "${shapeData.pointsCount}", color = TextMuted, fontSize = 11.sp)
            }
        }

        // Masking Mode Selector
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Masking Mode", color = TextSecondary, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf(MaskMode.NONE, MaskMode.ALPHA_MATTE, MaskMode.ALPHA_INVERT, MaskMode.LUMA_MATTE).forEach { mode ->
                val isSelected = shapeData.maskMode == mode
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isSelected) NeonCyan.copy(alpha = 0.25f) else StudioSurfaceVariant,
                            RoundedCornerShape(6.dp)
                        )
                        .border(
                            width = if (isSelected) 1.dp else 0.dp,
                            color = if (isSelected) NeonCyan else StudioBorder,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable { onShapeDataChanged(shapeData.copy(maskMode = mode)) }
                        .padding(vertical = 6.dp)
                ) {
                    Text(
                        text = mode.label.split(" ")[0],
                        color = if (isSelected) NeonCyan else TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
