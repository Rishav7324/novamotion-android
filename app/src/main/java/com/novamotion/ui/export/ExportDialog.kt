package com.novamotion.ui.export

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.novamotion.ui.theme.*

@Composable
fun ExportDialog(
    isExporting: Boolean,
    progress: Float,
    exportResultPath: String?,
    onDismiss: () -> Unit,
    onStartExport: (width: Int, height: Int, fps: Int, bitrateMbps: Int) -> Unit
) {
    var selectedRes by remember { mutableStateOf("1080p (FHD)") }
    var selectedFps by remember { mutableStateOf(60) }

    Dialog(onDismissRequest = { if (!isExporting) onDismiss() }) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = StudioSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StudioBorder, RoundedCornerShape(16.dp))
                .padding(2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Hardware Export (MediaCodec)",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Frame-by-Frame GPU Render to MP4",
                    color = TextMuted,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (exportResultPath != null) {
                    // Export Success State
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = NeonCyan,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Export Completed!",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = exportResultPath,
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "Done")
                        }
                    }
                } else if (isExporting) {
                    // Export In-Progress State
                    Column(modifier = Modifier.padding(vertical = 10.dp)) {
                        LinearProgressIndicator(
                            progress = { progress },
                            color = NeonCyan,
                            trackColor = StudioSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().height(8.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Encoding MP4 Video...",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                color = NeonCyan,
                                fontSize = 12.sp,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                } else {
                    // Export Configuration State
                    Text(text = "Resolution", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("720p (HD)", "1080p (FHD)", "4K UHD").forEach { res ->
                            val isSelected = res == selectedRes
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) ElectricIndigo else StudioSurfaceVariant,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedRes = res }
                                    .padding(vertical = 10.dp)
                            ) {
                                Text(text = res, color = TextPrimary, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = "Frame Rate", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(24, 30, 60).forEach { fps ->
                            val isSelected = fps == selectedFps
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) NeonCyan.copy(alpha = 0.3f) else StudioSurfaceVariant,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = if (isSelected) 1.dp else 0.dp,
                                        color = if (isSelected) NeonCyan else StudioBorder,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedFps = fps }
                                    .padding(vertical = 10.dp)
                            ) {
                                Text(text = "$fps FPS", color = TextPrimary, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(text = "Cancel", color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val (w, h) = when (selectedRes) {
                                    "720p (HD)" -> Pair(720, 1280)
                                    "4K UHD" -> Pair(2160, 3840)
                                    else -> Pair(1080, 1920)
                                }
                                onStartExport(w, h, selectedFps, 25)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "Start Render")
                        }
                    }
                }
            }
        }
    }
}
