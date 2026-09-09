package com.novamotion.ui.project

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
import com.novamotion.core.project.AspectRatioPreset
import com.novamotion.ui.theme.*

@Composable
fun NewProjectDialog(
    onDismiss: () -> Unit,
    onCreateProject: (title: String, preset: AspectRatioPreset, fps: Int) -> Unit
) {
    var title by remember { mutableStateOf("New Motion Project") }
    var selectedPreset by remember { mutableStateOf(AspectRatioPreset.REELS_9_16) }
    var selectedFps by remember { mutableStateOf(60) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = StudioSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StudioBorder, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "New Project",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Project Title") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = ElectricIndigo,
                        unfocusedBorderColor = StudioBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Aspect Ratio Selector
                Text(text = "Aspect Ratio", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AspectRatioPreset.values().forEach { preset ->
                        val isSelected = preset == selectedPreset
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isSelected) ElectricIndigo.copy(alpha = 0.2f) else StudioSurfaceVariant,
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.dp,
                                    color = if (isSelected) ElectricIndigo else StudioBorder,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedPreset = preset }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = preset.title,
                                color = if (isSelected) NeonCyan else TextPrimary,
                                fontSize = 14.sp,
                                modifier = Modifier.width(50.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = preset.description, color = TextPrimary, fontSize = 12.sp)
                                Text(
                                    text = "${preset.width}x${preset.height}",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Frame Rate (FPS)
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
                                    if (isSelected) ElectricIndigo else StudioSurfaceVariant,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedFps = fps }
                                .padding(vertical = 8.dp)
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
                        onClick = { onCreateProject(title, selectedPreset, selectedFps) },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo)
                    ) {
                        Text(text = "Create Project")
                    }
                }
            }
        }
    }
}
