package com.novamotion.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novamotion.R
import com.novamotion.core.model.AspectRatioPreset
import com.novamotion.core.model.Project
import com.novamotion.core.project.ProjectManager
import com.novamotion.ui.project.NewProjectDialog
import com.novamotion.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenProject: (Project) -> Unit,
    onOpenSettings: () -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedPresetForCreate by remember { mutableStateOf<AspectRatioPreset?>(null) }
    var activeTab by remember { mutableStateOf(0) } // 0: Projects, 1: Templates

    val recentProjects = remember {
        mutableStateListOf(
            ProjectManager.createProject("Cyberpunk Motion Intro", AspectRatioPreset.NINE_SIXTEEN),
            ProjectManager.createProject("Velocity Beat Drop", AspectRatioPreset.NINE_SIXTEEN),
            ProjectManager.createProject("Cinematic YouTube Vlog", AspectRatioPreset.SIXTEEN_NINE)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "NovaMotion Logo",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "NovaMotion Studio",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Pro Motion Graphics & VFX",
                                color = NeonCyan,
                                fontSize = 10.sp
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = StudioSurface)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = ElectricIndigo,
                contentColor = TextPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "New Project")
            }
        },
        containerColor = StudioBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Aspect Ratio Quick Starter Carousel
            Text(
                text = "Quick Project Creation",
                color = TextSecondary,
                fontSize = 12.sp,
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                listOf(
                    Triple(AspectRatioPreset.NINE_SIXTEEN, "9:16", "Reels/TikTok"),
                    Triple(AspectRatioPreset.SIXTEEN_NINE, "16:9", "YouTube/Cinema"),
                    Triple(AspectRatioPreset.ONE_ONE, "1:1", "Square Post"),
                    Triple(AspectRatioPreset.FOUR_FIVE, "4:5", "Portrait Post"),
                    Triple(AspectRatioPreset.TWENTY_ONE_NINE, "21:9", "Ultrawide")
                ).forEach { (preset, ratio, label) ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .width(110.dp)
                            .clickable {
                                selectedPresetForCreate = preset
                                showCreateDialog = true
                            }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(text = ratio, color = NeonCyan, fontSize = 16.sp, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = label, color = TextMuted, fontSize = 10.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Tabs: Projects / Templates
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = StudioSurface,
                contentColor = NeonCyan,
                divider = {}
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("Recent Projects (${recentProjects.size})") }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("Starter Templates") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (activeTab == 0) {
                // Projects Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(recentProjects) { proj ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenProject(proj) }
                                .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(90.dp)
                                        .background(StudioBackground, RoundedCornerShape(8.dp))
                                ) {
                                    Icon(
                                        Icons.Default.MovieCreation,
                                        contentDescription = null,
                                        tint = ElectricIndigo,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = proj.title,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    style = MaterialTheme.typography.titleSmall
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "${proj.width}x${proj.height}",
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        text = "${proj.fps} FPS",
                                        color = NeonCyan,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Starter Templates
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    listOf(
                        "Cyberpunk Neon Kinetic Title" to "Vibrant glowing text animation with chromatic split",
                        "Velocity AMV Beat Ramp" to "Optical flow speed ramp with mirror motion tile",
                        "Hollywood Cinematic Film Intro" to "3D Kodak Portra LUT color grade with film vignette",
                        "Audio Reactive Bass Spectrum" to "Real-time spring physics spectrum visualizer"
                    ).forEach { (title, desc) ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val tplProj = ProjectManager.createProject(title, AspectRatioPreset.NINE_SIXTEEN)
                                    onOpenProject(tplProj)
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = NeonCyan)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = title, color = TextPrimary, fontSize = 13.sp)
                                    Text(text = desc, color = TextMuted, fontSize = 11.sp)
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
                            }
                        }
                    }
                }
            }
        }

        // New Project Creation Dialog
        if (showCreateDialog) {
            NewProjectDialog(
                onDismiss = { showCreateDialog = false },
                onCreateProject = { title, preset, fps ->
                    val newProj = ProjectManager.createProject(title = title, aspectRatio = selectedPresetForCreate ?: preset, fps = fps)
                    recentProjects.add(0, newProj)
                    showCreateDialog = false
                    onOpenProject(newProj)
                }
            )
        }
    }
}
