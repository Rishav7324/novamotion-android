package com.novamotion.ui.media

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
    onSelectLayerType: (LayerType) -> Unit,
    onMediaSelected: (Uri, LayerType) -> Unit
) {
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onMediaSelected(uri, LayerType.VIDEO)
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onMediaSelected(uri, LayerType.IMAGE)
        }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onMediaSelected(uri, LayerType.AUDIO)
        }
    }

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
                    title = "Device Video",
                    subtitle = "Pick MP4/MKV video clip from gallery",
                    iconColor = PurpleVideo,
                    onClick = {
                        videoPickerLauncher.launch("video/*")
                    }
                )

                AssetOptionRow(
                    icon = Icons.Default.PhotoLibrary,
                    title = "Device Photo",
                    subtitle = "Pick PNG/JPEG image from gallery",
                    iconColor = NeonCyan,
                    onClick = {
                        imagePickerLauncher.launch("image/*")
                    }
                )

                AssetOptionRow(
                    icon = Icons.Default.Audiotrack,
                    title = "Audio / Music",
                    subtitle = "Pick MP3/WAV soundtrack from device",
                    iconColor = GreenAudio,
                    onClick = {
                        audioPickerLauncher.launch("audio/*")
                    }
                )

                AssetOptionRow(
                    icon = Icons.Default.TextFields,
                    title = "Kinetic Text",
                    subtitle = "Animated typography with custom fonts & colors",
                    iconColor = AmberText,
                    onClick = { onSelectLayerType(LayerType.TEXT) }
                )

                AssetOptionRow(
                    icon = Icons.Default.Category,
                    title = "Vector Shape",
                    subtitle = "Parametric Stars, Polygons, Rectangles & Circles",
                    iconColor = CyanShape,
                    onClick = { onSelectLayerType(LayerType.SHAPE) }
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
            .clickable(onClick = onClick)
            .background(StudioSurfaceVariant, RoundedCornerShape(10.dp))
            .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
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

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = TextPrimary, fontSize = 13.sp, style = MaterialTheme.typography.titleSmall)
            Text(text = subtitle, color = TextMuted, fontSize = 11.sp)
        }

        Icon(Icons.Default.Add, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
    }
}
