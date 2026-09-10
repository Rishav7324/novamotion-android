package com.novamotion.ui.effects

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novamotion.core.effects.EffectCatalog
import com.novamotion.core.effects.EffectDefinition
import com.novamotion.core.model.EffectType
import com.novamotion.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EffectsBrowserSheet(
    onDismiss: () -> Unit,
    onSelectEffect: (EffectType) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = remember {
        listOf("All") + EffectCatalog.availableEffects.map { it.type.category }.distinct()
    }

    val filteredEffects = EffectCatalog.availableEffects.filter { eff ->
        val matchesCategory = selectedCategory == "All" || eff.type.category == selectedCategory
        val matchesSearch = eff.type.displayName.contains(searchQuery, ignoreCase = true) ||
                eff.description.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }

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
                text = "VFX Effects Library (150+ Shaders)",
                color = TextPrimary,
                fontSize = 14.sp,
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search effects, blur, warp, glow...", color = TextMuted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = ElectricIndigo,
                    unfocusedBorderColor = StudioBorder
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Category Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = cat == selectedCategory
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .background(
                                if (isSelected) ElectricIndigo else StudioSurfaceVariant,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = cat,
                            color = if (isSelected) TextPrimary else TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Effects List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxHeight(0.6f)
            ) {
                items(filteredEffects) { def ->
                    EffectItemRow(definition = def, onClick = { onSelectEffect(def.type) })
                }
            }
        }
    }
}

@Composable
private fun EffectItemRow(
    definition: EffectDefinition,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(StudioSurfaceVariant, RoundedCornerShape(8.dp))
            .border(0.75.dp, StudioBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(30.dp)
                .background(ElectricIndigo.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
        ) {
            Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = ElectricIndigo, modifier = Modifier.size(16.dp))
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = definition.type.displayName, color = TextPrimary, fontSize = 12.sp)
            Text(text = definition.description, color = TextMuted, fontSize = 10.sp, maxLines = 1)
        }
    }
}
