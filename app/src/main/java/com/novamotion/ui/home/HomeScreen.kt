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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novamotion.R
import com.novamotion.core.model.Project
import com.novamotion.core.project.AspectRatioPreset
import com.novamotion.core.project.ProjectManager
import com.novamotion.core.project.ProjectPersistenceManager
import com.novamotion.ui.components.CupertinoSegmentedControl
import com.novamotion.ui.components.GlassmorphicCard
import com.novamotion.ui.components.iosSpringClick
import com.novamotion.ui.preset.XmlPresetDialog
import com.novamotion.ui.project.NewProjectDialog
import com.novamotion.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenProject: (Project) -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showXmlImportDialog by remember { mutableStateOf(false) }
    var selectedPresetForCreate by remember { mutableStateOf<AspectRatioPreset?>(null) }
    var activeTab by remember { mutableIntStateOf(0) } // 0: Projects, 1: Templates
    var isLoadingProjects by remember { mutableStateOf(true) }

    // Load saved projects from disk on first launch
    val recentProjects = remember { mutableStateListOf<Project>() }
    LaunchedEffect(Unit) {
        isLoadingProjects = true
        val savedProject = ProjectPersistenceManager.loadLastProject(context)
        if (savedProject != null) {
            recentProjects.add(0, savedProject)
        }
        val allIds = ProjectPersistenceManager.listSavedProjectIds(context)
        for (id in allIds) {
            if (savedProject?.id != id) {
                val proj = ProjectPersistenceManager.loadProject(context, id)
                if (proj != null) recentProjects.add(proj)
            }
        }
        isLoadingProjects = false
    }

    Scaffold(
        topBar = {
            // Apple Frosted Glass Top Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                GlassmorphicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    shape = RoundedCornerShape(21.dp),
                    backgroundColor = IosGlassSurface,
                    borderBrush = IosGlassBorder,
                    elevation = 4.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.app_logo),
                                contentDescription = "NovaMotion Logo",
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "NovaMotion Studio",
                                    color = IosLabelPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Pro Motion Graphics & VFX",
                                    color = IosCyan,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Import XML Preset Button
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x33000000))
                                    .border(0.5.dp, Color(0x26FFFFFF), CircleShape)
                                    .iosSpringClick { showXmlImportDialog = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Code,
                                    contentDescription = "Import XML Preset",
                                    tint = IosMint,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            // Settings Icon Button
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x33000000))
                                    .border(0.5.dp, Color(0x26FFFFFF), CircleShape)
                                    .iosSpringClick { onOpenSettings() }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = IosLabelSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            // Apple Pill Floating Action Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(IosPurple, IosIndigo)
                        )
                    )
                    .border(1.dp, Brush.verticalGradient(listOf(Color.White, Color(0x33FFFFFF))), RoundedCornerShape(16.dp))
                    .iosSpringClick { showCreateDialog = true }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "New Project",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        containerColor = IosSystemBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── Quick Aspect Ratio Starter Carousel ─────────────────────────
            Text(
                text = "START NEW CANVAS",
                color = IosLabelSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                listOf(
                    Triple(AspectRatioPreset.REELS_9_16, "9:16", "Reels/TikTok"),
                    Triple(AspectRatioPreset.CINEMA_16_9, "16:9", "YouTube/Cinema"),
                    Triple(AspectRatioPreset.SQUARE_1_1, "1:1", "Square Post"),
                    Triple(AspectRatioPreset.FEED_4_5, "4:5", "Portrait"),
                    Triple(AspectRatioPreset.ULTRAWIDE_21_9, "21:9", "Ultrawide")
                ).forEach { (preset, ratio, label) ->
                    GlassmorphicCard(
                        modifier = Modifier
                            .width(100.dp)
                            .iosSpringClick {
                                selectedPresetForCreate = preset
                                showCreateDialog = true
                            },
                        shape = RoundedCornerShape(12.dp),
                        backgroundColor = IosGlassSurface,
                        borderBrush = IosGlassBorder,
                        elevation = 3.dp
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Text(
                                text = ratio,
                                color = IosCyan,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = label,
                                color = IosLabelSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── Cupertino Segmented Navigation Bar ───────────────────────────
            CupertinoSegmentedControl(
                items = listOf("Recent Projects (${recentProjects.size})", "Starter Templates"),
                selectedIndex = activeTab,
                onSelectIndex = { activeTab = it }
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (activeTab == 0) {
                if (isLoadingProjects) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        CircularProgressIndicator(color = IosIndigo)
                    }
                } else {
                    // Projects Grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(recentProjects) { proj ->
                            ProjectCard(
                                project = proj,
                                onClick = { onOpenProject(proj) },
                                onDelete = {
                                    scope.launch {
                                        ProjectPersistenceManager.deleteProject(context, proj.id)
                                        recentProjects.remove(proj)
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                // Starter Templates List
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    listOf(
                        Triple("Cyberpunk Neon Kinetic Title", "Vibrant glowing text animation with chromatic split", AspectRatioPreset.REELS_9_16),
                        Triple("Velocity AMV Beat Ramp", "Optical flow speed ramp with mirror motion tile", AspectRatioPreset.REELS_9_16),
                        Triple("Hollywood Cinematic Film Intro", "3D Kodak Portra LUT color grade with film vignette", AspectRatioPreset.CINEMA_16_9),
                        Triple("Audio Reactive Bass Spectrum", "Real-time spring physics spectrum visualizer", AspectRatioPreset.SQUARE_1_1)
                    ).forEach { (title, desc, preset) ->
                        GlassmorphicCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .iosSpringClick {
                                    val tplProj = ProjectManager.createProject(title, preset)
                                    recentProjects.add(0, tplProj)
                                    onOpenProject(tplProj)
                                },
                            shape = RoundedCornerShape(16.dp),
                            backgroundColor = IosGlassSurface,
                            borderBrush = IosGlassBorder,
                            elevation = 4.dp
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(IosIndigo.copy(alpha = 0.2f))
                                        .border(0.75.dp, IosIndigo, CircleShape)
                                ) {
                                    Icon(
                                        Icons.Default.AutoFixHigh,
                                        contentDescription = null,
                                        tint = IosCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        color = IosLabelPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = desc,
                                        color = IosLabelSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = IosLabelTertiary)
                            }
                        }
                    }
                }
            }
        }

        if (showCreateDialog) {
            NewProjectDialog(
                onDismiss = { showCreateDialog = false },
                onCreateProject = { title, preset, fps ->
                    val newProj = ProjectManager.createProject(
                        title = title,
                        aspectRatio = selectedPresetForCreate ?: preset,
                        fps = fps
                    )
                    recentProjects.add(0, newProj)
                    showCreateDialog = false
                    selectedPresetForCreate = null
                    onOpenProject(newProj)
                }
            )
        }

        if (showXmlImportDialog) {
            val placeholder = remember { ProjectManager.createProject("Community Preset") }
            XmlPresetDialog(
                currentProject = placeholder,
                onDismiss = { showXmlImportDialog = false },
                onProjectImported = { imported ->
                    recentProjects.add(0, imported)
                    onOpenProject(imported)
                }
            )
        }
    }
}

@Composable
private fun ProjectCard(
    project: Project,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    GlassmorphicCard(
        modifier = Modifier
            .fillMaxWidth()
            .iosSpringClick { onClick() },
        shape = RoundedCornerShape(16.dp),
        backgroundColor = IosGlassSurface,
        borderBrush = IosGlassBorder,
        elevation = 6.dp
    ) {
            Column(modifier = Modifier.padding(8.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(IosSystemBackground)
                    .border(0.5.dp, Color(0x1AFFFFFF), RoundedCornerShape(8.dp))
            ) {
                Icon(
                    Icons.Default.MovieCreation,
                    contentDescription = null,
                    tint = IosIndigo,
                    modifier = Modifier.size(26.dp)
                )
                // Delete button
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier
                        .size(22.dp)
                        .align(Alignment.TopEnd)
                        .padding(1.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Delete",
                        tint = IosLabelTertiary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = project.title,
                color = IosLabelPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${project.width}×${project.height}",
                    color = IosLabelSecondary,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "${project.fps} FPS",
                    color = IosCyan,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = "${project.layers.size} layer${if (project.layers.size != 1) "s" else ""}",
                color = IosLabelTertiary,
                fontSize = 9.sp
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Project?", color = IosLabelPrimary) },
            text = { Text("\"${project.title}\" will be permanently deleted.", color = IosLabelSecondary) },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) {
                    Text("Delete", color = IosRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = IosLabelSecondary)
                }
            },
            containerColor = IosTertiaryBackground,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
