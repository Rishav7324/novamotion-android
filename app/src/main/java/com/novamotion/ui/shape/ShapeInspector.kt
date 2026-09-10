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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novamotion.core.shape.MaskMode
import com.novamotion.core.shape.ShapeType
import com.novamotion.core.shape.VectorShapeData
import com.novamotion.ui.components.GlassmorphicCard
import com.novamotion.ui.theme.*

/**
 * Apple iOS Cupertino Vector Shape & Mask Inspector.
 */
@Composable
fun ShapeInspector(
    shapeData: VectorShapeData,
    onShapeDataChanged: (VectorShapeData) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(IosSecondaryBackground)
            .padding(14.dp)
    ) {
        Text(
            text = "VECTOR SHAPE & MASKS",
            color = IosLabelSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Shape Type Selector Cards
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf(ShapeType.RECTANGLE, ShapeType.CIRCLE, ShapeType.STAR, ShapeType.POLYGON).forEach { type ->
                val isSelected = shapeData.type == type
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) IosIndigo else Color(0x331C1C1E)
                        )
                        .border(
                            width = if (isSelected) 1.dp else 0.5.dp,
                            color = if (isSelected) Color.White else Color(0x26FFFFFF),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onShapeDataChanged(shapeData.copy(type = type)) }
                        .padding(vertical = 10.dp)
                ) {
                    Text(
                        text = type.displayName.split(" ")[0],
                        color = if (isSelected) Color.White else IosLabelSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Shape Parameters Card
        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            backgroundColor = IosGlassSurface,
            borderBrush = IosGlassBorder,
            elevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (shapeData.type == ShapeType.RECTANGLE) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Corner Radius",
                            color = IosLabelSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.width(90.dp)
                        )
                        Slider(
                            value = shapeData.cornerRadius,
                            onValueChange = { onShapeDataChanged(shapeData.copy(cornerRadius = it)) },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = IosIndigo,
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
                                text = "${shapeData.cornerRadius.toInt()}px",
                                color = IosLabelPrimary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                } else if (shapeData.type == ShapeType.STAR) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Star Points",
                            color = IosLabelSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.width(90.dp)
                        )
                        Slider(
                            value = shapeData.pointsCount.toFloat(),
                            onValueChange = { onShapeDataChanged(shapeData.copy(pointsCount = it.toInt())) },
                            valueRange = 3f..12f,
                            steps = 8,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = IosIndigo,
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
                                text = "${shapeData.pointsCount}",
                                color = IosLabelPrimary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Masking Modes
                Text(
                    text = "MASKING MODES",
                    color = IosLabelSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
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
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) IosCyan.copy(alpha = 0.25f) else Color(0x33000000)
                                )
                                .border(
                                    width = if (isSelected) 1.dp else 0.5.dp,
                                    color = if (isSelected) IosCyan else Color(0x26FFFFFF),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onShapeDataChanged(shapeData.copy(maskMode = mode)) }
                                .padding(vertical = 6.dp)
                        ) {
                            Text(
                                text = mode.label.split(" ")[0],
                                color = if (isSelected) IosCyan else IosLabelSecondary,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}
