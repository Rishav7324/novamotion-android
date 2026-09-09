package com.novamotion.ui.layout

import android.net.Uri
import android.os.Environment
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novamotion.core.audio.AudioPlaybackEngine
import com.novamotion.core.effects.EffectCatalog
import com.novamotion.core.export.ExportConfiguration
import com.novamotion.core.export.HardwareVideoEncoder
import com.novamotion.core.model.*
import com.novamotion.core.project.ProjectManager
import com.novamotion.ui.canvas.CanvasViewport
import com.novamotion.ui.curve.BezierGraphEditor
import com.novamotion.ui.dock.QuickActionDock
import com.novamotion.ui.effects.EffectsBrowserSheet
import com.novamotion.ui.export.ExportDialog
import com.novamotion.ui.inspector.PropertyInspector
import com.novamotion.ui.media.AddAssetBottomSheet
import com.novamotion.ui.media.AssetPickerHelper
import com.novamotion.ui.project.NewProjectDialog
import com.novamotion.ui.shape.ShapeInspector
import com.novamotion.ui.templates.TemplateBrowserSheet
import com.novamotion.core.text.KineticTextStyle
import com.novamotion.core.shape.VectorShapeData
import com.novamotion.core.shape.ShapeType
import com.novamotion.ui.text.TextInspector
import com.novamotion.ui.timeline.MagneticTimeline
import com.novamotion.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioWorkspace(
    initialProject: Project? = null,
    onBackToHome: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var project by remember {
        mutableStateOf(
            initialProject ?: ProjectManager.createProject(title = "Cyberpunk Motion Intro")
        )
    }

    // Default sample layers if empty
    LaunchedEffect(Unit) {
        if (project.layers.isEmpty()) {
            val sampleLayers = listOf(
                Layer(
                    name = "NovaMotion Title",
                    type = LayerType.TEXT,
                    startTimeMs = 0L,
                    durationMs = 6000L,
                    textContent = "NOVAMOTION PRO",
                    textColor = 0xFFFFFFFF,
                    transform = LayerTransform(
                        posX = AnimatableProperty(
                            defaultValue = 0f,
                            keyframes = listOf(
                                Keyframe(timeMs = 0L, value = -300f),
                                Keyframe(timeMs = 1500L, value = 0f)
                            )
                        ),
                        scaleX = AnimatableProperty(1.2f),
                        scaleY = AnimatableProperty(1.2f)
                    )
                ),
                Layer(
                    name = "Neon Cyber Star",
                    type = LayerType.SHAPE,
                    startTimeMs = 500L,
                    durationMs = 5500L,
                    shapeType = "STAR",
                    fillColor = 0xFF6366F1,
                    transform = LayerTransform(
                        posY = AnimatableProperty(
                            defaultValue = 150f,
                            keyframes = listOf(
                                Keyframe(timeMs = 500L, value = 300f),
                                Keyframe(timeMs = 2000L, value = 150f)
                            )
                        ),
                        rotation = AnimatableProperty(
                            defaultValue = 0f,
                            keyframes = listOf(
                                Keyframe(timeMs = 500L, value = 0f),
                                Keyframe(timeMs = 5000L, value = 360f)
                            )
                        )
                    )
                )
            )
            val updated = project.copy(layers = sampleLayers)
            project = updated
            ProjectManager.updateActiveProject(updated, recordHistory = false)
        }
    }

    var currentPlayheadMs by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
    var selectedLayerId by remember { mutableStateOf<String?>(project.layers.firstOrNull()?.id) }

    // Audio Playback Engine
    val audioEngine = remember { AudioPlaybackEngine(context) }
    DisposableEffect(Unit) {
        onDispose {
            audioEngine.release()
        }
    }

    // Synchronize audio with playback state
    LaunchedEffect(isPlaying) {
        val audioLayer = project.layers.find { it.type == LayerType.AUDIO && it.mediaUri != null }
        if (audioLayer != null && audioLayer.mediaUri != null) {
            audioEngine.loadAudio(audioLayer.mediaUri)
        }

        if (isPlaying) {
            audioEngine.play(currentPlayheadMs)
            while (isPlaying) {
                delay(16L) // ~60 FPS
                currentPlayheadMs = (currentPlayheadMs + 16L) % project.durationMs
            }
        } else {
            audioEngine.pause()
        }
    }

    // Dialog & Sheet states
    var showCurveGraph by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showNewProjectDialog by remember { mutableStateOf(false) }
    var showTemplatesSheet by remember { mutableStateOf(false) }
    var showAddLayerSheet by remember { mutableStateOf(false) }
    var showEffectsSheet by remember { mutableStateOf(false) }

    // Real Export states
    var isExporting by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableFloatStateOf(0f) }
    var exportResultPath by remember { mutableStateOf<String?>(null) }

    val selectedLayer = project.layers.find { it.id == selectedLayerId }
    val isOnKeyframe = selectedLayer?.transform?.posX?.keyframes?.any { it.timeMs == currentPlayheadMs } == true

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBackToHome) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Home", tint = TextPrimary)
                    }
                },
                title = {
                    Column {
                        Text(text = project.title, color = TextPrimary, fontSize = 15.sp, maxLines = 1)
                        Text(
                            text = "${project.width}x${project.height} • ${project.fps} FPS • ${(project.durationMs / 1000)}s",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                },
                actions = {
                    // Undo
                    IconButton(
                        onClick = {
                            if (ProjectManager.undo()) {
                                ProjectManager.activeProject.value?.let { project = it }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = "Undo", tint = if (ProjectManager.canUndo()) TextPrimary else TextMuted)
                    }

                    // Redo
                    IconButton(
                        onClick = {
                            if (ProjectManager.redo()) {
                                ProjectManager.activeProject.value?.let { project = it }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Redo, contentDescription = "Redo", tint = if (ProjectManager.canRedo()) TextPrimary else TextMuted)
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
            // ZONE 1: Canvas Viewport (Top ~45%) with Interactive Touch Gizmo
            CanvasViewport(
                project = project,
                currentPlayheadMs = currentPlayheadMs,
                selectedLayerId = selectedLayerId,
                onTransformChange = { layerId, dx, dy, zoom, dRot ->
                    val layer = project.layers.find { it.id == layerId } ?: return@CanvasViewport
                    val curX = layer.transform.posX.evaluate(currentPlayheadMs)
                    val curY = layer.transform.posY.evaluate(currentPlayheadMs)
                    val curScaleX = layer.transform.scaleX.evaluate(currentPlayheadMs)
                    val curScaleY = layer.transform.scaleY.evaluate(currentPlayheadMs)
                    val curRot = layer.transform.rotation.evaluate(currentPlayheadMs)

                    val newX = curX + dx
                    val newY = curY + dy
                    val newScaleX = (curScaleX * zoom).coerceIn(0.05f, 10f)
                    val newScaleY = (curScaleY * zoom).coerceIn(0.05f, 10f)
                    val newRot = curRot + dRot

                    fun updateProperty(prop: AnimatableProperty<Float>, newVal: Float): AnimatableProperty<Float> {
                        return if (prop.keyframes.isNotEmpty()) {
                            val idx = prop.keyframes.indexOfFirst { it.timeMs == currentPlayheadMs }
                            val newKfs = if (idx >= 0) {
                                prop.keyframes.mapIndexed { i, kf -> if (i == idx) kf.copy(value = newVal) else kf }
                            } else {
                                (prop.keyframes + Keyframe(timeMs = currentPlayheadMs, value = newVal)).sortedBy { it.timeMs }
                            }
                            prop.copy(keyframes = newKfs)
                        } else {
                            prop.copy(defaultValue = newVal)
                        }
                    }

                    val updatedTransform = layer.transform.copy(
                        posX = updateProperty(layer.transform.posX, newX),
                        posY = updateProperty(layer.transform.posY, newY),
                        scaleX = updateProperty(layer.transform.scaleX, newScaleX),
                        scaleY = updateProperty(layer.transform.scaleY, newScaleY),
                        rotation = updateProperty(layer.transform.rotation, newRot)
                    )

                    val updatedLayer = layer.copy(transform = updatedTransform)
                    val updatedProject = project.copy(
                        layers = project.layers.map { if (it.id == layerId) updatedLayer else it }
                    )
                    project = updatedProject
                    ProjectManager.updateActiveProject(updatedProject, recordHistory = false)
                },
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
                    audioEngine.seekTo(currentPlayheadMs)
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
                        val updatedLayers = project.layers.flatMap {
                            if (it.id == layer.id) listOf(part1, part2) else listOf(it)
                        }
                        val updatedProj = project.copy(layers = updatedLayers)
                        project = updatedProj
                        selectedLayerId = part2.id
                        ProjectManager.updateActiveProject(updatedProj)
                    }
                },
                onDuplicateClip = {
                    val layer = selectedLayer ?: return@QuickActionDock
                    val duplicate = layer.copy(
                        id = java.util.UUID.randomUUID().toString(),
                        name = "${layer.name} Copy",
                        startTimeMs = layer.startTimeMs + 200L
                    )
                    val updatedProj = project.copy(layers = project.layers + duplicate)
                    project = updatedProj
                    selectedLayerId = duplicate.id
                    ProjectManager.updateActiveProject(updatedProj)
                },
                onToggleCurveGraph = { showCurveGraph = !showCurveGraph },
                onAddLayerClick = { showAddLayerSheet = true }
            )

            // ZONE 3: Magnetic Multi-Track Timeline & Context Inspector (Bottom ~54%)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.54f)
            ) {
                if (showCurveGraph) {
                    // Split Bezier Curve Graph Editor
                    BezierGraphEditor(
                        controlPoints = selectedLayer?.transform?.posX?.keyframes?.firstOrNull()?.curve ?: BezierControlPoints(),
                        onPointsChange = { newCurve ->
                            val layer = selectedLayer ?: return@BezierGraphEditor
                            val updatedKeyframes = layer.transform.posX.keyframes.map {
                                if (it.timeMs == currentPlayheadMs) it.copy(curve = newCurve) else it
                            }
                            val updatedLayer = layer.copy(
                                transform = layer.transform.copy(posX = layer.transform.posX.copy(keyframes = updatedKeyframes))
                            )
                            val updatedProj = project.copy(
                                layers = project.layers.map { if (it.id == updatedLayer.id) updatedLayer else it }
                            )
                            project = updatedProj
                            ProjectManager.updateActiveProject(updatedProj, recordHistory = false)
                        },
                        onClose = { showCurveGraph = false },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Multi-Track Magnetic Timeline
                        MagneticTimeline(
                            project = project,
                            currentPlayheadMs = currentPlayheadMs,
                            selectedLayerId = selectedLayerId,
                            onSeek = { ms ->
                                currentPlayheadMs = ms
                                audioEngine.seekTo(ms)
                            },
                            onSelectLayer = { id -> selectedLayerId = id },
                            onLayerMove = { id, newStartMs ->
                                val updatedLayers = project.layers.map {
                                    if (it.id == id) it.copy(startTimeMs = newStartMs) else it
                                }
                                val updatedProj = project.copy(layers = updatedLayers)
                                project = updatedProj
                                ProjectManager.updateActiveProject(updatedProj)
                            },
                            onLayerTrim = { id, newStartMs, newDurationMs ->
                                val updatedLayers = project.layers.map {
                                    if (it.id == id) it.copy(startTimeMs = newStartMs, durationMs = newDurationMs) else it
                                }
                                val updatedProj = project.copy(layers = updatedLayers)
                                project = updatedProj
                                ProjectManager.updateActiveProject(updatedProj)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.55f)
                        )

                        // Contextual Layer Property & VFX Inspector Drawer
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.45f)
                        ) {
                            when (selectedLayer?.type) {
                                LayerType.TEXT -> {
                                    val currentStyle = KineticTextStyle(
                                        text = selectedLayer.textContent,
                                        fillColor = selectedLayer.textColor
                                    )
                                    TextInspector(
                                        textStyle = currentStyle,
                                        onTextStyleChanged = { newStyle ->
                                            val updated = selectedLayer.copy(
                                                textContent = newStyle.text,
                                                textColor = newStyle.fillColor
                                            )
                                            val updatedProj = project.copy(layers = project.layers.map { if (it.id == updated.id) updated else it })
                                            project = updatedProj
                                            ProjectManager.updateActiveProject(updatedProj)
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                LayerType.SHAPE -> {
                                    val currentShape = VectorShapeData(
                                        type = try { ShapeType.valueOf(selectedLayer.shapeType) } catch (e: Exception) { ShapeType.RECTANGLE },
                                        primaryColor = selectedLayer.fillColor
                                    )
                                    ShapeInspector(
                                        shapeData = currentShape,
                                        onShapeDataChanged = { newShape ->
                                            val updated = selectedLayer.copy(
                                                shapeType = newShape.type.name,
                                                fillColor = newShape.primaryColor
                                            )
                                            val updatedProj = project.copy(layers = project.layers.map { if (it.id == updated.id) updated else it })
                                            project = updatedProj
                                            ProjectManager.updateActiveProject(updatedProj)
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                else -> {
                                    PropertyInspector(
                                        selectedLayer = selectedLayer,
                                        currentPlayheadMs = currentPlayheadMs,
                                        onValueChange = { prop, newVal ->
                                            val layer = selectedLayer ?: return@PropertyInspector
                                            val updatedTransform = when (prop) {
                                                "scale" -> layer.transform.copy(scaleX = AnimatableProperty(newVal), scaleY = AnimatableProperty(newVal))
                                                "rotation" -> layer.transform.copy(rotation = AnimatableProperty(newVal))
                                                "opacity" -> layer.transform.copy(opacity = AnimatableProperty(newVal))
                                                "jog" -> {
                                                    val curX = layer.transform.posX.defaultValue
                                                    layer.transform.copy(posX = AnimatableProperty(curX + newVal * 5f))
                                                }
                                                else -> layer.transform
                                            }
                                            val updatedLayer = layer.copy(transform = updatedTransform)
                                            val updatedProj = project.copy(layers = project.layers.map { if (it.id == updatedLayer.id) updatedLayer else it })
                                            project = updatedProj
                                            ProjectManager.updateActiveProject(updatedProj, recordHistory = false)
                                        },
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
                onSelectLayerType = { layerType ->
                    val newLayer = Layer(
                        name = if (layerType == LayerType.TEXT) "Kinetic Title" else "Vector Shape",
                        type = layerType,
                        startTimeMs = currentPlayheadMs,
                        durationMs = 4000L,
                        textContent = if (layerType == LayerType.TEXT) "EDIT TEXT" else "",
                        shapeType = if (layerType == LayerType.SHAPE) "STAR" else "RECTANGLE"
                    )
                    val updatedProj = project.copy(layers = project.layers + newLayer)
                    project = updatedProj
                    selectedLayerId = newLayer.id
                    ProjectManager.updateActiveProject(updatedProj)
                    showAddLayerSheet = false
                },
                onMediaSelected = { uri, layerType ->
                    val newLayer = AssetPickerHelper.createLayerFromMediaUri(
                        context = context,
                        uri = uri,
                        type = layerType,
                        startTimeMs = currentPlayheadMs
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
                isExporting = isExporting,
                progress = exportProgress,
                exportResultPath = exportResultPath,
                onDismiss = {
                    showExportDialog = false
                    exportResultPath = null
                    isExporting = false
                },
                onStartExport = { w, h, fps, bitrate ->
                    isExporting = true
                    exportProgress = 0f
                    exportResultPath = null
                    scope.launch {
                        val moviesDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
                        val targetFile = File(moviesDir, "NovaMotion_${System.currentTimeMillis()}.mp4")
                        val cfg = ExportConfiguration(
                            width = w,
                            height = h,
                            fps = fps,
                            bitrateMbps = bitrate,
                            outputFile = targetFile
                        )
                        val res = HardwareVideoEncoder.encodeProject(
                            context = context,
                            project = project,
                            config = cfg,
                            onProgress = { p -> exportProgress = p }
                        )
                        isExporting = false
                        if (res.isSuccess) {
                            exportResultPath = targetFile.absolutePath
                        }
                    }
                }
            )
        }
    }
}
