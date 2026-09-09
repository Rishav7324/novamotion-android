package com.novamotion.ui.export

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
    onDismiss: () -> Unit,
    onStartExport: (resolution: String, fps: Int, bitrateMbps: Int) -> Unit
) {
    var selectedRes by remember { mutableStateOf("1080p (FHD)") }
    var selectedFps by remember { mutableStateOf(60) }
    var isExporting by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0.45f) }

    Dialog(onDismissRequest = onDismiss) {
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
                    text = "Ultra-Fast GPU Blit to H.264/HEVC",
                    color = TextMuted,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Resolution Selector
                Text(text = "Resolution", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("1080p (FHD)", "2K", "4K Ultra").forEach { res ->
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
                            Text(text = res, color = TextPrimary, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Frame Rate Selector
                Text(text = "Frame Rate", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(30, 60, 120).forEach { fps ->
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

                if (isExporting) {
                    Column {
                        LinearProgressIndicator(
                            progress = { progress },
                            color = NeonCyan,
                            trackColor = StudioSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Rendering frames: ${(progress * 100).toInt()}%",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                } else {
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
                                isExporting = true
                                onStartExport(selectedRes, selectedFps, 25)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo)
                        ) {
                            Text(text = "Start Export")
                        }
                    }
                }
            }
        }
    }
}
