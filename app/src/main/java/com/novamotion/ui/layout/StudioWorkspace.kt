package com.novamotion.ui.layout

import android.os.Environment
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
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
import com.novamotion.ui.components.CupertinoSegmentedControl
import com.novamotion.ui.components.GlassmorphicCard
import com.novamotion.ui.components.iosSpringClick
import com.novamotion.ui.curve.BezierGraphEditor
import com.novamotion.ui.dock.QuickActionDock
import com.novamotion.ui.editor.EditorViewModel
import com.novamotion.ui.editor.EditorViewModelFactory
import com.novamotion.ui.effects.EffectsBrowserSheet
import com.novamotion.ui.export.ExportDialog
import com.novamotion.ui.inspector.PropertyInspector
import com.novamotion.ui.media.AddAssetBottomSheet
import com.novamotion.ui.media.AssetPickerHelper
import com.novamotion.ui.preset.XmlPresetDialog
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
        val proj = initialProject ?: ProjectManager.createProject(title = "Untitled Project")
        viewModel.loadProject(proj)
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
    val pxPerMs by viewModel.pxPerMs.collectAsState()
    val snapEnabled by viewModel.snapEnabled.collectAsState()
    val showWaveforms by viewModel.showWaveforms.collectAsState()

    var showNewProjectDialog by remember { mutableStateOf(false) }
    var showXmlPresetDialog by remember { mutableStateOf(false) }
    var selectedDeckTab by remember { mutableIntStateOf(0) } // 0: Timeline, 1: Inspector, 2: Curves

    // Sync curve graph toggle from dock with deck tab
    LaunchedEffect(showCurveGraph) {
        selectedDeckTab = if (showCurveGraph) 2 else 0
    }

    val currentProject = project ?: return
    val selectedLayer = currentProject.layers.find { it.id == selectedLayerId }
    val isOnKeyframe = selectedLayer?.transform?.posX?.keyframes?.any { it.timeMs == currentPlayheadMs } == true

    // ─── Audio synchronisation (real MediaPlayer) ───────────────────────────
    val audioEngine = remember { AudioPlaybackEngine(context) }
    DisposableEffect(Unit) { onDispose { audioEngine.release() } }

    val audioLayerUri = currentProject.layers.find { it.type == LayerType.AUDIO && it.mediaUri != null }?.mediaUri
    LaunchedEffect(audioLayerUri) {
        if (audioLayerUri != null) {
            audioEngine.loadAudio(audioLayerUri)
        } else {
            audioEngine.release()
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            audioEngine.play(currentPlayheadMs)
        } else {
            audioEngine.pause()
        }
    }

    LaunchedEffect(isPlaying, currentPlayheadMs) {
        if (isPlaying) {
            audioEngine.correctDriftIfNeeded(currentPlayheadMs)
        }
    }

    // ─── iOS Liquid Glass Studio Scaffold ────────────────────────────────────
    Scaffold(
        topBar = {
            // Apple iOS Frosted Glass Navigation Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
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
                            .padding(horizontal = 8.dp)
                    ) {
                        // Back to Home Button
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x33000000))
                                    .border(0.5.dp, Color(0x26FFFFFF), CircleShape)
                                    .iosSpringClick { onBackToHome() }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Home",
                                    tint = IosLabelPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Project Title & Specs
                            Column {
                                Text(
                                    text = currentProject.title,
                                    color = IosLabelPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${currentProject.width}×${currentProject.height} • ${currentProject.fps} FPS",
                                    color = IosLabelSecondary,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Actions: Undo, Redo, Add Layer, Export Pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // Undo
                            IconButton(
                                onClick = { viewModel.undo() },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    Icons.Default.Undo,
                                    contentDescription = "Undo",
                                    tint = if (ProjectManager.canUndo()) IosLabelPrimary else IosLabelTertiary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }

                            // Redo
                            IconButton(
                                onClick = { viewModel.redo() },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    Icons.Default.Redo,
                                    contentDescription = "Redo",
                                    tint = if (ProjectManager.canRedo()) IosLabelPrimary else IosLabelTertiary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }

                            // Add Layer
                            IconButton(
                                onClick = { viewModel.toggleAddLayerSheet(true) },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    Icons.Default.AddCircleOutline,
                                    contentDescription = "Add Layer",
                                    tint = IosCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            // XML Presets (Alight Motion 2-Way Import & Export)
                            IconButton(
                                onClick = { showXmlPresetDialog = true },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    Icons.Default.Code,
                                    contentDescription = "XML Presets",
                                    tint = IosMint,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            // Apple Pill "Export" CTA Button
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(IosPurple, IosIndigo)
                                        )
                                    )
                                    .border(0.75.dp, Brush.verticalGradient(listOf(Color.White, Color(0x33FFFFFF))), RoundedCornerShape(12.dp))
                                    .iosSpringClick { viewModel.toggleExportDialog(true) }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.FileDownload,
                                        contentDescription = "Export",
                                        tint = Color.White,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Export",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = IosSystemBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── ZONE 1: Seamless OLED Canvas Viewport (~48%) ─────────────────
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
                    .weight(0.48f)
            )

            // ── ZONE 2: Floating Dynamic Glass Island Dock (42dp) ─────────────
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
                    viewModel.splitLayerAtPlayhead()
                },
                onUndo = { viewModel.undo() },
                onRedo = { viewModel.redo() },
                onToggleCurveGraph = { viewModel.toggleCurveGraph() },
                onOpenEffects = { viewModel.toggleEffectsSheet(true) }
            )

            // ── ZONE 3: Cupertino Modular Lower Deck (~52%) ───────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.52f)
                    .background(IosSecondaryBackground)
            ) {
                // Cupertino Sliding Segmented Control Bar
                CupertinoSegmentedControl(
                    items = listOf("☵ Timeline", "🎛 Inspector", "∿ Curves"),
                    selectedIndex = selectedDeckTab,
                    onSelectIndex = { tab ->
                        selectedDeckTab = tab
                        if (tab == 2 && !showCurveGraph) viewModel.toggleCurveGraph()
                        if (tab != 2 && showCurveGraph) viewModel.toggleCurveGraph()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )

                // Animated Modular Content Deck — spring slide + fade (M3 Expressive)
                AnimatedContent(
                    targetState = selectedDeckTab,
                    transitionSpec = {
                        (fadeIn(animationSpec = androidx.compose.animation.core.tween(150)) + androidx.compose.animation.slideInVertically(
                            animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.85f, stiffness = 400f),
                            initialOffsetY = { it / 6 }
                        )) togetherWith (fadeOut(animationSpec = androidx.compose.animation.core.tween(120)) + androidx.compose.animation.slideOutVertically(
                            animationSpec = androidx.compose.animation.core.tween(120),
                            targetOffsetY = { -it / 8 }
                        ))
                    },
                    label = "modularDeckSwap",
                    modifier = Modifier.fillMaxSize()
                ) { targetTab ->
                    when (targetTab) {
                        0 -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Timeline mini-toolbar: zoom + snap + waveform toggles
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0x14141416))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    IconButton(onClick = { viewModel.zoomBy(0.85f) }, modifier = Modifier.size(22.dp)) {
                                        Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = IosLabelSecondary, modifier = Modifier.size(12.dp))
                                    }
                                    IconButton(onClick = { viewModel.zoomBy(1.18f) }, modifier = Modifier.size(22.dp)) {
                                        Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = IosLabelSecondary, modifier = Modifier.size(12.dp))
                                    }
                                    Box(modifier = Modifier.width(1.dp).height(14.dp).background(Color(0x33FFFFFF)))
                                    IconButton(onClick = { viewModel.toggleSnap() }, modifier = Modifier.size(22.dp)) {
                                        Icon(Icons.Default.AutoFixHigh, contentDescription = "Snap", tint = if (snapEnabled) IosCyan else IosLabelTertiary, modifier = Modifier.size(12.dp))
                                    }
                                    IconButton(onClick = { viewModel.setShowWaveforms(!showWaveforms) }, modifier = Modifier.size(22.dp)) {
                                        Icon(Icons.Default.GraphicEq, contentDescription = "Waveforms", tint = if (showWaveforms) IosCyan else IosLabelTertiary, modifier = Modifier.size(12.dp))
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(text = "${(pxPerMs*1000).toInt()} px/s", color = IosLabelTertiary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                }
                                MagneticTimeline(
                                    project = currentProject,
                                    currentPlayheadMs = currentPlayheadMs,
                                    selectedLayerId = selectedLayerId,
                                    onSeek = { ms ->
                                        viewModel.seekTo(ms)
                                        audioEngine.seekTo(ms)
                                    },
                                    onSelectLayer = { id ->
                                        viewModel.selectLayer(id)
                                    },
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
                                    onLayerTrimHead = { layerId, newStartMs, newDurationMs ->
                                        val layer = currentProject.layers.find { it.id == layerId } ?: return@MagneticTimeline
                                        val trimmed = com.novamotion.core.timeline.TimelineOperations.trimLayerHead(layer, newStartMs)
                                        val updatedProj = currentProject.copy(
                                            layers = currentProject.layers.map { if (it.id == layerId) trimmed else it }
                                        )
                                        viewModel.updateProject(updatedProj)
                                    },
                                    pxPerMs = pxPerMs,
                                    snapEnabled = snapEnabled,
                                    showWaveforms = showWaveforms,
                                    onZoomChange = { viewModel.setPxPerMs(it) },
                                    modifier = Modifier.weight(1f).fillMaxWidth()
                                )
                            }
                        }
                        1 -> {
                            // Full-height Contextual Layer Inspector
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
                        2 -> {
                            // Bézier Speed & Value Timing Graph
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
                                onClose = { selectedDeckTab = 0 },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        // ─── Modal Dialogs & Sheets ──────────────────────────────────────────

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
                        // Start foreground service to prevent kill during long encode
                        try { com.novamotion.core.export.ExportForegroundService.start(context) } catch (_: Exception) {}
                        // Request notification permission on Android 13+ (best-effort)
                        if (android.os.Build.VERSION.SDK_INT >= 33) {
                            try {
                                val perm = android.Manifest.permission.POST_NOTIFICATIONS
                                if (androidx.core.content.ContextCompat.checkSelfPermission(context, perm) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                    // Cannot request from composable without Activity; service will run without notification update
                                }
                            } catch (_: Exception) {}
                        }
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
                        try { com.novamotion.core.export.ExportForegroundService.stop(context) } catch (_: Exception) {}
                        viewModel.setExporting(false)
                        if (res.isSuccess) {
                            val displayName = "NovaMotion_${System.currentTimeMillis()}.mp4"
                            val galleryUri = MediaStoreExporter.saveToGallery(
                                context = context,
                                sourceFile = targetFile,
                                displayName = displayName
                            )
                            viewModel.setExportResult(galleryUri?.toString() ?: targetFile.absolutePath)
                        } else {
                            viewModel.setExportResult(null)
                        }
                    }
                }
            )
        }

        if (showXmlPresetDialog) {
            XmlPresetDialog(
                currentProject = currentProject,
                onDismiss = { showXmlPresetDialog = false },
                onProjectImported = { imported ->
                    viewModel.loadProject(imported)
                }
            )
        }
    }
}
