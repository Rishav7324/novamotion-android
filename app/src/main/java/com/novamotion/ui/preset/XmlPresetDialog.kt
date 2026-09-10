package com.novamotion.ui.preset

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.novamotion.core.model.Project
import com.novamotion.core.preset.AlightMotionXmlParser
import com.novamotion.ui.components.CupertinoSegmentedControl
import com.novamotion.ui.components.GlassmorphicCard
import com.novamotion.ui.components.iosSpringClick
import com.novamotion.ui.theme.*

/**
 * Apple iOS 18 Liquid Glass Alight Motion XML Preset Dialog.
 * Enables 1-tap community XML preset import and export.
 */
@Composable
fun XmlPresetDialog(
    currentProject: Project,
    onDismiss: () -> Unit,
    onProjectImported: (Project) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0 = Export, 1 = Import
    var importText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val exportedXml = remember(currentProject) {
        try {
            AlightMotionXmlParser.exportToAlightMotionXml(currentProject)
        } catch (e: Exception) {
            "<!-- Failed to generate XML: ${e.message} -->"
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        GlassmorphicCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            shape = RoundedCornerShape(24.dp),
            backgroundColor = IosGlassSurface,
            borderBrush = IosGlassBorder,
            elevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Dialog Title & Close
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "Alight Motion XML",
                            color = IosLabelPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Community Preset Interchange",
                            color = IosLabelSecondary,
                            fontSize = 11.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = IosLabelSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Segmented Tab Control: [ Export Preset ] | [ Import Preset ]
                CupertinoSegmentedControl(
                    items = listOf("Export Preset", "Import Preset"),
                    selectedIndex = selectedTab,
                    onItemSelected = {
                        selectedTab = it
                        errorMessage = null
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (selectedTab == 0) {
                    // ── 1. Export Preset ─────────────────────────────────────
                    Text(
                        text = "XML Preview (${currentProject.layers.size} layers):",
                        color = IosLabelTertiary,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x800A0B0E))
                            .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        val scrollState = rememberScrollState()
                        Text(
                            text = exportedXml,
                            color = IosCyan,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.verticalScroll(scrollState)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Copy XML Button
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0x33FFFFFF))
                                .border(0.5.dp, Color(0x40FFFFFF), RoundedCornerShape(20.dp))
                                .iosSpringClick {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Alight Motion XML", exportedXml)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "XML Preset copied to clipboard!", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = IosLabelPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Copy XML",
                                    color = IosLabelPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Share XML Button
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    Brush.linearGradient(listOf(IosPurple, IosIndigo))
                                )
                                .border(0.5.dp, Color(0x80FFFFFF), RoundedCornerShape(20.dp))
                                .iosSpringClick {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, exportedXml)
                                        putExtra(Intent.EXTRA_TITLE, "${currentProject.title} Preset.xml")
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share Preset XML"))
                                }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Share Preset",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                } else {
                    // ── 2. Import Preset ─────────────────────────────────────
                    Text(
                        text = "Paste Alight Motion XML Preset:",
                        color = IosLabelTertiary,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = importText,
                        onValueChange = {
                            importText = it
                            errorMessage = null
                        },
                        placeholder = {
                            Text(
                                "Paste <scene ...> XML preset here",
                                color = IosLabelTertiary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        textStyle = LocalTextStyle.current.copy(
                            color = IosLabelPrimary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IosCyan,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedContainerColor = Color(0x800A0B0E),
                            unfocusedContainerColor = Color(0x800A0B0E)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    )

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = IosRed,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Load Preset CTA Button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .clip(RoundedCornerShape(21.dp))
                            .background(
                                if (importText.isNotBlank()) {
                                    Brush.linearGradient(listOf(IosCyan, IosIndigo))
                                } else {
                                    Brush.linearGradient(listOf(Color(0x33FFFFFF), Color(0x22FFFFFF)))
                                }
                            )
                            .border(0.5.dp, Color(0x80FFFFFF), RoundedCornerShape(21.dp))
                            .iosSpringClick {
                                if (importText.isBlank()) return@iosSpringClick
                                val result = AlightMotionXmlParser.importFromAlightMotionXml(importText)
                                result.onSuccess { importedProj ->
                                    Toast.makeText(context, "Preset imported successfully!", Toast.LENGTH_SHORT).show()
                                    onProjectImported(importedProj)
                                    onDismiss()
                                }.onFailure { error ->
                                    errorMessage = "Invalid XML: ${error.message}"
                                }
                            }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.DownloadDone,
                                contentDescription = "Import",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Load & Open Preset",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
