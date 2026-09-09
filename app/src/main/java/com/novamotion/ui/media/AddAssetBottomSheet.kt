package com.novamotion.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novamotion.core.model.LayerType
import com.novamotion.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAssetBottomSheet(
    onDismiss: () -> Unit,
    onSelectOption: (LayerType) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = StudioSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = StudioBorder) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Add Layer to Timeline",
                color = TextPrimary,
                fontSize = 16.sp,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AssetOptionRow(
                    icon = Icons.Default.VideoLibrary,
                    title = "Media (Video / Image)",
                    subtitle = "Import clips from device storage",
                    iconColor = PurpleVideo,
                    onClick = { onSelectOption(LayerType.VIDEO) }
                )

                AssetOptionRow(
                    icon = Icons.Default.TextFields,
                    title = "Kinetic Text",
                    subtitle = "Animated typography with custom fonts",
                    iconColor = AmberText,
                    onClick = { onSelectOption(LayerType.TEXT) }
                )

                AssetOptionRow(
                    icon = Icons.Default.Category,
                    title = "Vector Shape",
                    subtitle = "Parametric shapes, masks, and paths",
                    iconColor = NeonCyan,
                    onClick = { onSelectOption(LayerType.SHAPE) }
                )

                AssetOptionRow(
                    icon = Icons.Default.MusicNote,
                    title = "Audio / Sound FX",
                    subtitle = "Music track with waveform peaks",
                    iconColor = EmeraldAudio,
                    onClick = { onSelectOption(LayerType.AUDIO) }
                )

                AssetOptionRow(
                    icon = Icons.Default.AutoFixHigh,
                    title = "Adjustment Layer",
                    subtitle = "Apply global color grade and VFX to all layers below",
                    iconColor = OrangeAdjustment,
                    onClick = { onSelectOption(LayerType.ADJUSTMENT) }
                )

                AssetOptionRow(
                    icon = Icons.Default.CenterFocusStrong,
                    title = "Null Controller",
                    subtitle = "Parent multiple layers to a single controller",
                    iconColor = TextSecondary,
                    onClick = { onSelectOption(LayerType.NULL_OBJECT) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AssetOptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(StudioSurfaceVariant, RoundedCornerShape(10.dp))
            .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .background(iconColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column {
            Text(text = title, color = TextPrimary, fontSize = 14.sp)
            Text(text = subtitle, color = TextMuted, fontSize = 11.sp)
        }
    }
}
