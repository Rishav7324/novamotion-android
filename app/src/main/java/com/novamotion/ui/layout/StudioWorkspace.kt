package com.novamotion.ui.layout

import android.os.Environment
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novamotion.core.audio.AudioPlaybackEngine
import com.novamotion.core.effects.EffectCatalog
import com.novamotion.core.export.ExportConfiguration
import com.novamotion.core.export.HardwareVideoEncoder
import com.novamotion.core.export.MediaStoreExporter
import com.novamotion.core.model.*
import com.novamotion.core.project.ProjectManager
import com.novamotion.core.shape.ShapeType
import com.novamotion.core.shape.VectorShapeData
import com.novamotion.core.text.KineticTextStyle
import com.novamotion.ui.canvas.CanvasViewport
import com.novamotion.ui.curve.BezierGraphEditor
import com.novamotion.ui.dock.QuickActionDock
import com.novamotion.ui.editor.EditorViewModel
import com.novamotion.ui.editor.EditorViewModelFactory
import com.novamotion.ui.effects.EffectsBrowserSheet
import com.novamotion.ui.export.ExportDialog
import com.novamotion.ui.inspector.PropertyInspector
import com.novamotion.ui.media.AddAssetBottomSheet
import com.novamotion.ui.media.AssetPickerHelper
import com.novamotion.ui.project.NewProjectDialog
import com.novamotion.ui.shape.ShapeInspector
import com.novamotion.ui.templates.TemplateBrowserSheet
import com.novamotion.ui.text.TextInspector
import com.novamotion.ui.timeline.MagneticTimeline
import com.novamotion.ui.theme.*
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
    val activity = context as? androidx.savedstate.SavedStateRegistryOwner

    // ─── ViewModel (survives configuration changes) ─────────────────────────
    val viewModel: EditorViewModel = if (activity != null) {
        viewModel(factory = EditorViewModelFactory(activity, context))
    } else {
        viewModel()
    }

    // ─── Load initial project into ViewModel once ───────────────────────────
    LaunchedEffect(initialProject?.id) {
        val proj = initialProject ?: ProjectManager.createProject(title = "Cyberpunk Motion Intro")
        viewModel.loadProject(proj)

        // Seed sample layers if empty
        if (proj.layers.isEmpty()) {
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
            val seeded = proj.copy(layers = sampleLayers)
            viewModel.updateProject(seeded, recordHistory = false)
        }
    }

    // ─── Collect state from ViewModel ───────────────────────────────────────
    val project by viewModel.project.collectAsState()
    val currentPlayheadMs by viewModel.playheadMs.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val selectedLayerId by viewModel.selectedLayerId.collectAsState()
    val showCurveGraph by viewModel.showCurveGraph.collectAsState()
    val showExportDialog by viewModel.showExportDialog.collectAsState()
    val showAddLayerSheet by viewModel.showAddLayerSheet.collectAsState()
    val showEffectsSheet by viewModel.showEffectsSheet.collectAsState()
    val showTemplatesSheet by viewModel.showTemplatesSheet.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    val exportProgress by viewModel.exportProgress.collectAsState()
    val exportResultPath by viewModel.exportResultPath.collectAsState()

    var showNewProjectDialog by remember { mutableStateOf(false) }

    val currentProject = project ?: return

    val selectedLayer = currentProject.layers.find { it.id == selectedLayerId }
    val isOnKeyframe = selectedLayer?.transform?.posX?.keyframes?.any { it.timeMs == currentPlayheadMs } == true

    // ─── Audio synchronisation (real MediaPlayer) ───────────────────────────
    val audioEngine = remember { AudioPlaybackEngine(context) }
    DisposableEffect(Unit) { onDispose { audioEngine.release() } }

    // Sync audio source loading when audio layers change
    val audioLayerUri = currentProject.layers.find { it.type == LayerType.AUDIO && it.mediaUri != null }?.mediaUri
    LaunchedEffect(audioLayerUri) {
        if (audioLayerUri != null) {
            audioEngine.loadAudio(audioLayerUri)
        } else {
            audioEngine.release()
        }
    }

    // Trigger play or pause only when playback state toggles
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            audioEngine.play(currentPlayheadMs)
        } else {
            audioEngine.pause()
        }
    }

    // Smooth drift correction during playback (only seeks if drift > 200ms)
    LaunchedEffect(isPlaying, currentPlayheadMs) {
        if (isPlaying) {
            audioEngine.correctDriftIfNeeded(currentPlayheadMs)
        }
    }

    // ─── UI ─────────────────────────────────────────────────────────────────
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
                        Text(text = currentProject.title, color = TextPrimary, fontSize = 15.sp, maxLines = 1)
                        Text(
                            text = "${currentProject.width}x${currentProject.height} • ${currentProject.fps} FPS • ${currentProject.durationMs / 1000}s",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                },
                actions = {
                    // Undo
                    IconButton(onClick = { viewModel.undo() }) {
                        Icon(
                            Icons.Default.Undo,
                            contentDescription = "Undo",
                            tint = if (ProjectManager.canUndo()) TextPrimary else TextMuted
                        )
                    }
                    // Redo
                    IconButton(onClick = { viewModel.redo() }) {
                        Icon(
                            Icons.Default.Redo,
                            contentDescription = "Redo",
                            tint = if (ProjectManager.canRedo()) TextPrimary else TextMuted
                        )
                    }
                    // Add Layer
                    IconButton(onClick = { viewModel.toggleAddLayerSheet(true) }) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = "Add Layer", tint = NeonCyan)
                    }
                    // Export
                    Button(
                        onClick = { viewModel.toggleExportDialog(true) },
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
                project = currentProject,
                currentPlayheadMs = currentPlayheadMs,
                selectedLayerId = selectedLayerId,
                onTransformChange = { layerId, dx, dy, zoom, dRot ->
                    val layer = currentProject.layers.find { it.id == layerId } ?: return@CanvasViewport
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
                    val updatedProject = currentProject.copy(
                        layers = currentProject.layers.map { if (it.id == layerId) updatedLayer else it }
                    )
                    viewModel.updateProject(updatedProject, recordHistory = false)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.46f)
            )

            // ZONE 2: Quick Action Dock (Center ~56dp)
            QuickActionDock(
                currentPlayheadMs = currentPlayheadMs,
                isPlaying = isPlaying,
                isOnKeyframe = isOnKeyframe,
                showCurveGraph = showCurveGraph,
                onTogglePlay = { viewModel.togglePlayback() },
                onStepFrame = { step -> viewModel.stepFrame(step >= 0) },
                onToggleKeyframe = {
                    val layer = selectedLayer ?: return@QuickActionDock
                    val existing = layer.transform.posX.keyframes.find { it.timeMs == currentPlayheadMs }
                    val updatedKeyframes = if (existing != null) {
                        layer.transform.posX.keyframes.filter { it.timeMs != currentPlayheadMs }
                    } else {
                        layer.transform.posX.keyframes + Keyframe(timeMs = currentPlayheadMs, value = 0f)
                    }
                    val updatedLayer = layer.copy(
                        transform = layer.transform.copy(
                            posX = layer.transform.posX.copy(keyframes = updatedKeyframes)
                        )
                    )
                    val updatedProj = currentProject.copy(
                        layers = currentProject.layers.map { if (it.id == updatedLayer.id) updatedLayer else it }
                    )
                    viewModel.updateProject(updatedProj)
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
                        val updatedLayers = currentProject.layers.flatMap {
                            if (it.id == layer.id) listOf(part1, part2) else listOf(it)
                        }
                        val updatedProj = currentProject.copy(layers = updatedLayers)
                        viewModel.updateProject(updatedProj)
                        viewModel.selectLayer(part2.id)
                    }
                },
                onUndo = { viewModel.undo() },
                onRedo = { viewModel.redo() },
                onToggleCurveGraph = { viewModel.toggleCurveGraph() },
                onOpenEffects = { viewModel.toggleEffectsSheet(true) }
            )

            // ZONE 3: Timeline + Inspector (Bottom ~54%)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.54f)
            ) {
                if (showCurveGraph) {
                    BezierGraphEditor(
                        curve = selectedLayer?.transform?.posX?.keyframes?.firstOrNull()?.curve ?: BezierControlPoints(),
                        onCurveChanged = { newCurve ->
                            val layer = selectedLayer ?: return@BezierGraphEditor
                            val updatedKeyframes = layer.transform.posX.keyframes.map {
                                if (it.timeMs == currentPlayheadMs) it.copy(curve = newCurve) else it
                            }
                            val updatedLayer = layer.copy(
                                transform = layer.transform.copy(posX = layer.transform.posX.copy(keyframes = updatedKeyframes))
                            )
                            val updatedProj = currentProject.copy(
                                layers = currentProject.layers.map { if (it.id == updatedLayer.id) updatedLayer else it }
                            )
                            viewModel.updateProject(updatedProj, recordHistory = false)
                        },
                        onClose = { viewModel.toggleCurveGraph() },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        MagneticTimeline(
                            project = currentProject,
                            currentPlayheadMs = currentPlayheadMs,
                            selectedLayerId = selectedLayerId,
                            onSeek = { ms ->
                                viewModel.seekTo(ms)
                                audioEngine.seekTo(ms)
                            },
                            onSelectLayer = { id -> viewModel.selectLayer(id) },
                            onLayerMoved = { layerId, newStartMs ->
                                val layer = currentProject.layers.find { it.id == layerId } ?: return@MagneticTimeline
                                val updatedLayer = layer.copy(startTimeMs = newStartMs)
                                val updatedProj = currentProject.copy(
                                    layers = currentProject.layers.map { if (it.id == layerId) updatedLayer else it }
                                )
                                viewModel.updateProject(updatedProj)
                            },
                            onLayerTrimmed = { layerId, newDurationMs ->
                                val layer = currentProject.layers.find { it.id == layerId } ?: return@MagneticTimeline
                                val updatedLayer = layer.copy(durationMs = newDurationMs)
                                val updatedProj = currentProject.copy(
                                    layers = currentProject.layers.map { if (it.id == layerId) updatedLayer else it }
                                )
                                viewModel.updateProject(updatedProj)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.55f)
                        )

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
                                            val updatedProj = currentProject.copy(
                                                layers = currentProject.layers.map { if (it.id == updated.id) updated else it }
                                            )
                                            viewModel.updateProject(updatedProj)
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
                                            val updatedProj = currentProject.copy(
                                                layers = currentProject.layers.map { if (it.id == updated.id) updated else it }
                                            )
                                            viewModel.updateProject(updatedProj)
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
                                                "posX" -> layer.transform.copy(posX = AnimatableProperty(newVal))
                                                "posY" -> layer.transform.copy(posY = AnimatableProperty(newVal))
                                                "scale" -> layer.transform.copy(
                                                    scaleX = AnimatableProperty(newVal),
                                                    scaleY = AnimatableProperty(newVal)
                                                )
                                                "rotation" -> layer.transform.copy(rotation = AnimatableProperty(newVal))
                                                "opacity" -> layer.transform.copy(opacity = AnimatableProperty(newVal))
                                                "jog" -> {
                                                    val curX = layer.transform.posX.defaultValue
                                                    layer.transform.copy(posX = AnimatableProperty(curX + newVal * 5f))
                                                }
                                                else -> layer.transform
                                            }
                                            val updatedLayer = layer.copy(transform = updatedTransform)
                                            val updatedProj = currentProject.copy(
                                                layers = currentProject.layers.map { if (it.id == updatedLayer.id) updatedLayer else it }
                                            )
                                            viewModel.updateProject(updatedProj, recordHistory = false)
                                        },
                                        onAddEffectClick = { viewModel.toggleEffectsSheet(true) },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ─── Dialogs & Sheets ────────────────────────────────────────────────

        if (showNewProjectDialog) {
            NewProjectDialog(
                onDismiss = { showNewProjectDialog = false },
                onCreateProject = { title, preset, fps ->
                    val newProj = ProjectManager.createProject(title = title, aspectRatio = preset, fps = fps)
                    viewModel.loadProject(newProj)
                    showNewProjectDialog = false
                }
            )
        }

        if (showTemplatesSheet) {
            TemplateBrowserSheet(
                onDismiss = { viewModel.toggleTemplatesSheet(false) },
                onSelectTemplate = { tpl ->
                    viewModel.loadProject(tpl.project)
                    viewModel.toggleTemplatesSheet(false)
                }
            )
        }

        if (showAddLayerSheet) {
            AddAssetBottomSheet(
                onDismiss = { viewModel.toggleAddLayerSheet(false) },
                onSelectLayerType = { layerType ->
                    val newLayer = Layer(
                        name = if (layerType == LayerType.TEXT) "Kinetic Title" else "Vector Shape",
                        type = layerType,
                        startTimeMs = currentPlayheadMs,
                        durationMs = 4000L,
                        textContent = if (layerType == LayerType.TEXT) "EDIT TEXT" else "",
                        shapeType = if (layerType == LayerType.SHAPE) "STAR" else "RECTANGLE"
                    )
                    viewModel.addLayer(newLayer)
                    viewModel.toggleAddLayerSheet(false)
                },
                onMediaSelected = { uri, layerType ->
                    val newLayer = AssetPickerHelper.createLayerFromMediaUri(
                        context = context,
                        uri = uri,
                        type = layerType,
                        startTimeMs = currentPlayheadMs
                    )
                    viewModel.addLayer(newLayer)
                    viewModel.toggleAddLayerSheet(false)
                }
            )
        }

        if (showEffectsSheet) {
            EffectsBrowserSheet(
                onDismiss = { viewModel.toggleEffectsSheet(false) },
                onSelectEffect = { effectType ->
                    val layer = selectedLayer ?: return@EffectsBrowserSheet
                    val newEffect = EffectCatalog.createEffect(effectType)
                    val updatedLayer = layer.copy(effects = layer.effects + newEffect)
                    val updatedProj = currentProject.copy(
                        layers = currentProject.layers.map { if (it.id == updatedLayer.id) updatedLayer else it }
                    )
                    viewModel.updateProject(updatedProj)
                    viewModel.toggleEffectsSheet(false)
                }
            )
        }

        if (showExportDialog) {
            ExportDialog(
                isExporting = isExporting,
                progress = exportProgress,
                exportResultPath = exportResultPath,
                onDismiss = {
                    viewModel.toggleExportDialog(false)
                    viewModel.setExportResult(null)
                    viewModel.setExporting(false)
                },
                onStartExport = { w, h, fps, bitrate ->
                    viewModel.setExporting(true)
                    viewModel.setExportProgress(0f)
                    viewModel.setExportResult(null)
                    scope.launch {
                        val moviesDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
                            ?: context.filesDir
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
                            project = currentProject,
                            config = cfg,
                            onProgress = { p -> viewModel.setExportProgress(p) }
                        )
                        viewModel.setExporting(false)
                        if (res.isSuccess) {
                            // Save to system Gallery (visible in Photos/Gallery apps)
                            val displayName = "NovaMotion_${System.currentTimeMillis()}.mp4"
                            val galleryUri = MediaStoreExporter.saveToGallery(
                                context = context,
                                sourceFile = targetFile,
                                displayName = displayName
                            )
                            // Report gallery URI if saved, else fallback to file path
                            viewModel.setExportResult(galleryUri?.toString() ?: targetFile.absolutePath)
                        }
                    }
                }
            )
        }
    }
}
