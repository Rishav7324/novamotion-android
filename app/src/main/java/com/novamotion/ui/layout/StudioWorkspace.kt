package com.novamotion.ui.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novamotion.core.model.*
import com.novamotion.ui.canvas.CanvasViewport
import com.novamotion.ui.curve.BezierGraphEditor
import com.novamotion.ui.dock.QuickActionDock
import com.novamotion.ui.export.ExportDialog
import com.novamotion.ui.inspector.PropertyInspector
import com.novamotion.ui.theme.*
import com.novamotion.ui.timeline.MagneticTimeline
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioWorkspace() {
    // Demo Sample Project with Video, Shape, and Text layers
    var project by remember {
        mutableStateOf(
            Project(
                title = "Cyberpunk Motion Intro",
                width = 1080,
                height = 1920,
                fps = 60,
                durationMs = 8000L,
                layers = listOf(
                    Layer(
                        name = "Background Video",
                        type = LayerType.VIDEO,
                        startTimeMs = 0L,
                        durationMs = 8000L
                    ),
                    Layer(
                        name = "Neon Cyber Glow",
                        type = LayerType.SHAPE,
                        startTimeMs = 500L,
                        durationMs = 6000L,
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
                        durationMs = 5000L,
                        textContent = "NOVAMOTION ULTRA"
                    ),
                    Layer(
                        name = "Cyber Synth Beat",
                        type = LayerType.AUDIO,
                        startTimeMs = 0L,
                        durationMs = 8000L
                    )
                )
            )
        )
    }

    var currentPlayheadMs by remember { mutableLongStateOf(1200L) }
    var isPlaying by remember { mutableStateOf(false) }
    var selectedLayerId by remember { mutableStateOf<String?>(project.layers.getOrNull(1)?.id) }
    var showCurveGraph by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

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
                        Text(
                            text = project.title,
                            color = TextPrimary,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "${project.width}x${project.height} • ${project.fps} FPS • ${(project.durationMs / 1000)}s",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                },
                actions = {
                    // Export Button
                    Button(
                        onClick = { showExportDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Export",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Export", fontSize = 12.sp)
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextSecondary)
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
                    // Add/Remove keyframe at playhead
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
                    project = project.copy(
                        layers = project.layers.map { if (it.id == updatedLayer.id) updatedLayer else it }
                    )
                },
                onCutClip = {
                    // Cut/Split selected layer at playhead
                },
                onUndo = {},
                onRedo = {},
                onToggleCurveGraph = { showCurveGraph = !showCurveGraph },
                onOpenEffects = {},
                modifier = Modifier.fillMaxWidth()
            )

            // ZONE 3: Context-Aware Lower Deck (~45%)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.54f)
            ) {
                if (showCurveGraph) {
                    // Mode B: Split Curve Graph Editor
                    BezierGraphEditor(
                        curve = selectedLayer?.transform?.posX?.keyframes?.firstOrNull()?.curve ?: BezierControlPoints(),
                        onCurveChanged = {},
                        onClose = { showCurveGraph = false },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Mode A: Multi-Track Magnetic Timeline + Property Drawer
                    Column(modifier = Modifier.fillMaxSize()) {
                        MagneticTimeline(
                            project = project,
                            currentPlayheadMs = currentPlayheadMs,
                            selectedLayerId = selectedLayerId,
                            onSelectLayer = { selectedLayerId = it },
                            onSeek = { currentPlayheadMs = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )

                        PropertyInspector(
                            selectedLayer = selectedLayer,
                            currentPlayheadMs = currentPlayheadMs,
                            onValueChange = { _, _ -> },
                            onAddEffectClick = {},
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                        )
                    }
                }
            }
        }

        if (showExportDialog) {
            ExportDialog(
                onDismiss = { showExportDialog = false },
                onStartExport = { _, _, _ ->
                    // Launch hardware export
                }
            )
        }
    }
}
