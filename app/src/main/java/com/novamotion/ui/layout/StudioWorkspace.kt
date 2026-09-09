package com.novamotion.ui.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novamotion.core.effects.EffectCatalog
import com.novamotion.core.model.*
import com.novamotion.core.project.ProjectManager
import com.novamotion.core.shape.VectorShapeData
import com.novamotion.core.text.KineticTextStyle
import com.novamotion.ui.canvas.CanvasViewport
import com.novamotion.ui.curve.BezierGraphEditor
import com.novamotion.ui.dock.QuickActionDock
import com.novamotion.ui.effects.EffectsBrowserSheet
import com.novamotion.ui.export.ExportDialog
import com.novamotion.ui.inspector.PropertyInspector
import com.novamotion.ui.media.AddAssetBottomSheet
import com.novamotion.ui.project.NewProjectDialog
import com.novamotion.ui.shape.ShapeInspector
import com.novamotion.ui.templates.TemplateBrowserSheet
import com.novamotion.ui.text.TextInspector
import com.novamotion.ui.timeline.MagneticTimeline
import com.novamotion.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioWorkspace() {
    var project by remember {
        mutableStateOf(
            ProjectManager.createProject(title = "Cyberpunk Motion Intro")
        )
    }

    // Default layers
    LaunchedEffect(Unit) {
        if (project.layers.isEmpty()) {
            val sampleLayers = listOf(
                Layer(name = "Background Footage", type = LayerType.VIDEO, startTimeMs = 0L, durationMs = 8000L),
                Layer(
                    name = "Neon Cyber Glow",
                    type = LayerType.SHAPE,
                    startTimeMs = 500L,
                    durationMs = 6500L,
                    transform = LayerTransform(
                        posX = AnimatableProperty(
                            defaultValue = 0f,
                            keyframes = listOf(
                                Keyframe(timeMs = 500L, value = -200f),
                                Keyframe(timeMs = 2500L, value = 200f)
                            )
                        )
                    )
                ),
                Layer(
                    name = "NovaMotion Kinetic Title",
                    type = LayerType.TEXT,
                    startTimeMs = 1000L,
                    durationMs = 5500L,
                    textContent = "NOVAMOTION ULTRA"
                ),
                Layer(name = "Cyber Synth Beat", type = LayerType.AUDIO, startTimeMs = 0L, durationMs = 8000L)
            )
            val updated = project.copy(layers = sampleLayers)
            project = updated
            ProjectManager.updateActiveProject(updated, recordHistory = false)
        }
    }

    var currentPlayheadMs by remember { mutableLongStateOf(1200L) }
    var isPlaying by remember { mutableStateOf(false) }
    var selectedLayerId by remember { mutableStateOf<String?>(project.layers.getOrNull(1)?.id) }

    // Dialog & Sheet states
    var showCurveGraph by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showNewProjectDialog by remember { mutableStateOf(false) }
    var showTemplatesSheet by remember { mutableStateOf(false) }
    var showAddLayerSheet by remember { mutableStateOf(false) }
    var showEffectsSheet by remember { mutableStateOf(false) }

    // Auto-advance playhead when playing
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(16L) // ~60 FPS
            currentPlayheadMs = (currentPlayheadMs + 16L) % project.durationMs
        }
    }

    val selectedLayer = project.layers.find { it.id == selectedLayerId }
    val isOnKeyframe = selectedLayer?.transform?.posX?.keyframes?.any { it.timeMs == currentPlayheadMs } == true

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = project.title, color = TextPrimary, fontSize = 15.sp)
                        Text(
                            text = "${project.width}x${project.height} • ${project.fps} FPS • ${(project.durationMs / 1000)}s",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                },
                actions = {
                    // New Project
                    IconButton(onClick = { showNewProjectDialog = true }) {
                        Icon(Icons.Default.AddBox, contentDescription = "New Project", tint = TextSecondary)
                    }

                    // Templates
                    IconButton(onClick = { showTemplatesSheet = true }) {
                        Icon(Icons.Default.Dashboard, contentDescription = "Templates", tint = NeonCyan)
                    }

                    // Export Button
                    Button(
                        onClick = { showExportDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Export", fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = StudioSurface)
            )
        },
        containerColor = StudioBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ZONE 1: Canvas Viewport (Top ~45%)
            CanvasViewport(
                project = project,
                currentPlayheadMs = currentPlayheadMs,
                selectedLayerId = selectedLayerId,
                onTransformChange = { _, _, _, _, _ -> },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.46f)
            )

            // ZONE 2: 1-Tap Quick Action Dock (Center ~56dp)
            QuickActionDock(
                currentPlayheadMs = currentPlayheadMs,
                isPlaying = isPlaying,
                isOnKeyframe = isOnKeyframe,
                showCurveGraph = showCurveGraph,
                onTogglePlay = { isPlaying = !isPlaying },
                onStepFrame = { step ->
                    val frameTime = 1000L / project.fps
                    currentPlayheadMs = (currentPlayheadMs + step * frameTime).coerceIn(0L, project.durationMs)
                },
                onToggleKeyframe = {
                    val layer = selectedLayer ?: return@QuickActionDock
                    val existing = layer.transform.posX.keyframes.find { it.timeMs == currentPlayheadMs }
                    val updatedKeyframes = if (existing != null) {
                        layer.transform.posX.keyframes.filter { it.timeMs != currentPlayheadMs }
                    } else {
                        layer.transform.posX.keyframes + Keyframe(timeMs = currentPlayheadMs, value = 0f)
                    }
                    val updatedLayer = layer.copy(
                        transform = layer.transform.copy(posX = layer.transform.posX.copy(keyframes = updatedKeyframes))
                    )
                    val updatedProj = project.copy(
                        layers = project.layers.map { if (it.id == updatedLayer.id) updatedLayer else it }
                    )
                    project = updatedProj
                    ProjectManager.updateActiveProject(updatedProj)
                },
                onCutClip = {
                    val layer = selectedLayer ?: return@QuickActionDock
                    if (currentPlayheadMs > layer.startTimeMs && currentPlayheadMs < layer.endTimeMs) {
                        val part1 = layer.copy(durationMs = currentPlayheadMs - layer.startTimeMs)
                        val part2 = layer.copy(
                            id = java.util.UUID.randomUUID().toString(),
                            name = "${layer.name} (Split)",
                            startTimeMs = currentPlayheadMs,
                            durationMs = layer.endTimeMs - currentPlayheadMs
                        )
                        val updatedProj = project.copy(
                            layers = project.layers.flatMap { if (it.id == layer.id) listOf(part1, part2) else listOf(it) }
                        )
                        project = updatedProj
                        ProjectManager.updateActiveProject(updatedProj)
                    }
                },
                onUndo = {
                    if (ProjectManager.undo()) {
                        ProjectManager.activeProject.value?.let { project = it }
                    }
                },
                onRedo = {
                    if (ProjectManager.redo()) {
                        ProjectManager.activeProject.value?.let { project = it }
                    }
                },
                onToggleCurveGraph = { showCurveGraph = !showCurveGraph },
                onOpenEffects = { showEffectsSheet = true },
                modifier = Modifier.fillMaxWidth()
            )

            // ZONE 3: Context-Aware Lower Deck (~45%)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.54f)
            ) {
                if (showCurveGraph) {
                    BezierGraphEditor(
                        curve = selectedLayer?.transform?.posX?.keyframes?.firstOrNull()?.curve ?: BezierControlPoints(),
                        onCurveChanged = {},
                        onClose = { showCurveGraph = false },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Timeline with Add Layer button
                        Box(modifier = Modifier.weight(1f)) {
                            MagneticTimeline(
                                project = project,
                                currentPlayheadMs = currentPlayheadMs,
                                selectedLayerId = selectedLayerId,
                                onSelectLayer = { selectedLayerId = it },
                                onSeek = { currentPlayheadMs = it },
                                modifier = Modifier.fillMaxSize()
                            )

                            // Floating "+ Layer" Button
                            FloatingActionButton(
                                onClick = { showAddLayerSheet = true },
                                containerColor = ElectricIndigo,
                                contentColor = TextPrimary,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(12.dp)
                                    .size(44.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Layer")
                            }
                        }

                        // Contextual Inspector Deck
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                        ) {
                            when (selectedLayer?.type) {
                                LayerType.SHAPE -> {
                                    ShapeInspector(
                                        shapeData = VectorShapeData(),
                                        onShapeDataChanged = {},
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                LayerType.TEXT -> {
                                    TextInspector(
                                        textStyle = KineticTextStyle(text = selectedLayer.textContent),
                                        onTextStyleChanged = { newStyle ->
                                            val updated = selectedLayer.copy(textContent = newStyle.text)
                                            val updatedProj = project.copy(
                                                layers = project.layers.map { if (it.id == updated.id) updated else it }
                                            )
                                            project = updatedProj
                                            ProjectManager.updateActiveProject(updatedProj, recordHistory = false)
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                else -> {
                                    PropertyInspector(
                                        selectedLayer = selectedLayer,
                                        currentPlayheadMs = currentPlayheadMs,
                                        onValueChange = { _, _ -> },
                                        onAddEffectClick = { showEffectsSheet = true },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dialogs & Sheets
        if (showNewProjectDialog) {
            NewProjectDialog(
                onDismiss = { showNewProjectDialog = false },
                onCreateProject = { title, preset, fps ->
                    project = ProjectManager.createProject(title = title, aspectRatio = preset, fps = fps)
                    selectedLayerId = null
                    showNewProjectDialog = false
                }
            )
        }

        if (showTemplatesSheet) {
            TemplateBrowserSheet(
                onDismiss = { showTemplatesSheet = false },
                onSelectTemplate = { tpl ->
                    project = tpl.project
                    ProjectManager.setActiveProject(tpl.project)
                    selectedLayerId = tpl.project.layers.firstOrNull()?.id
                    showTemplatesSheet = false
                }
            )
        }

        if (showAddLayerSheet) {
            AddAssetBottomSheet(
                onDismiss = { showAddLayerSheet = false },
                onSelectOption = { layerType ->
                    val newLayer = Layer(
                        name = "New ${layerType.label}",
                        type = layerType,
                        startTimeMs = currentPlayheadMs,
                        durationMs = 4000L
                    )
                    val updatedProj = project.copy(layers = project.layers + newLayer)
                    project = updatedProj
                    selectedLayerId = newLayer.id
                    ProjectManager.updateActiveProject(updatedProj)
                    showAddLayerSheet = false
                }
            )
        }

        if (showEffectsSheet) {
            EffectsBrowserSheet(
                onDismiss = { showEffectsSheet = false },
                onSelectEffect = { effectType ->
                    val layer = selectedLayer ?: return@EffectsBrowserSheet
                    val newEffect = EffectCatalog.createEffect(effectType)
                    val updatedLayer = layer.copy(effects = layer.effects + newEffect)
                    val updatedProj = project.copy(
                        layers = project.layers.map { if (it.id == updatedLayer.id) updatedLayer else it }
                    )
                    project = updatedProj
                    ProjectManager.updateActiveProject(updatedProj)
                    showEffectsSheet = false
                }
            )
        }

        if (showExportDialog) {
            ExportDialog(
                onDismiss = { showExportDialog = false },
                onStartExport = { _, _, _ ->
                    // Launch offline hardware export
                }
            )
        }
    }
}
