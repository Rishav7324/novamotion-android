package com.novamotion.ui.templates

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novamotion.core.templates.ProjectTemplate
import com.novamotion.core.templates.TemplateLibrary
import com.novamotion.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateBrowserSheet(
    onDismiss: () -> Unit,
    onSelectTemplate: (ProjectTemplate) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = StudioSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = StudioBorder) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Preset Motion Templates",
                color = TextPrimary,
                fontSize = 17.sp,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "1-Tap trending animations and effects",
                color = TextMuted,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxHeight(0.6f)
            ) {
                items(TemplateLibrary.templates) { template ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(StudioSurfaceVariant, RoundedCornerShape(10.dp))
                            .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
                            .clickable { onSelectTemplate(template) }
                            .padding(14.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .background(ElectricIndigo.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = ElectricIndigo)
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = template.title, color = TextPrimary, fontSize = 14.sp)
                            Text(text = template.description, color = TextMuted, fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
