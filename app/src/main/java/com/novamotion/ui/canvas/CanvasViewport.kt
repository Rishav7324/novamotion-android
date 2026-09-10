package com.novamotion.ui.canvas

import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.GridOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.novamotion.core.model.Project
import com.novamotion.core.model.evaluate
import com.novamotion.core.render.GLViewportRenderer
import com.novamotion.ui.components.GlassmorphicCard
import com.novamotion.ui.theme.*

/**
 * Zone 1: Canvas Viewport for NovaMotion Studio.
 * Real-time OpenGL ES 3.2 rendering over pure OLED Black background with
 * floating iOS Liquid Glass HUD and precision interactive transform gizmo.
 */
@Composable
fun CanvasViewport(
    project: Project,
    currentPlayheadMs: Long,
    selectedLayerId: String?,
    onTransformChange: (layerId: String, dx: Float, dy: Float, scale: Float, rotate: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var renderer by remember { mutableStateOf<GLViewportRenderer?>(null) }
    var showGridGuides by remember { mutableStateOf(false) }

    val selectedLayer = project.layers.find { it.id == selectedLayerId }

    // Evaluated transform values for visual bounding box gizmo
    val layerX = selectedLayer?.transform?.posX?.evaluate(currentPlayheadMs) ?: 0f
    val layerY = selectedLayer?.transform?.posY?.evaluate(currentPlayheadMs) ?: 0f
    val layerScaleX = selectedLayer?.transform?.scaleX?.evaluate(currentPlayheadMs) ?: 1f
    val layerRot = selectedLayer?.transform?.rotation?.evaluate(currentPlayheadMs) ?: 0f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(IosSystemBackground)
            .clipToBounds()
    ) {
        // ── 1. OpenGL ES 3.2 Surface View ────────────────────────────────
        AndroidView(
            factory = { context ->
                android.opengl.GLSurfaceView(context).apply {
                    setEGLContextClientVersion(3)
                    val r = GLViewportRenderer(context).apply {
                        this.currentProject = project
                        this.currentPlayheadMs = currentPlayheadMs
                    }
                    renderer = r
                    setRenderer(r)
                    renderMode = android.opengl.GLSurfaceView.RENDERMODE_WHEN_DIRTY
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { view ->
                renderer?.currentProject = project
                renderer?.currentPlayheadMs = currentPlayheadMs
                view.requestRender()
            },
            modifier = Modifier.fillMaxSize()
        )

        // ── 2. Safe Areas / Rule-of-Thirds Grid Overlay ──────────────────
        AnimatedVisibility(
            visible = showGridGuides,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val gridColor = Color(0x33FFFFFF)
                val safeColor = Color(0x6664D2FF)

                // 3x3 Rule of Thirds
                drawLine(gridColor, Offset(w / 3f, 0f), Offset(w / 3f, h), strokeWidth = 1f)
                drawLine(gridColor, Offset(2 * w / 3f, 0f), Offset(2 * w / 3f, h), strokeWidth = 1f)
                drawLine(gridColor, Offset(0f, h / 3f), Offset(w, h / 3f), strokeWidth = 1f)
                drawLine(gridColor, Offset(0f, 2 * h / 3f), Offset(w, 2 * h / 3f), strokeWidth = 1f)

                // 90% Safe Area Title Rect
                val padX = w * 0.05f
                val padY = h * 0.05f
                drawRect(
                    color = safeColor,
                    topLeft = Offset(padX, padY),
                    size = androidx.compose.ui.geometry.Size(w - 2 * padX, h - 2 * padY),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
                )
            }
        }

        // ── 3. Floating iOS Liquid Glass HUD Capsule ─────────────────────
        GlassmorphicCard(
            shape = RoundedCornerShape(14.dp),
            backgroundColor = IosGlassSurface,
            borderBrush = IosGlassBorder,
            elevation = 6.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                // Resolution & FPS badge
                Text(
                    text = "${project.width}×${project.height} • ${project.fps} FPS",
                    color = IosLabelSecondary,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(12.dp)
                        .background(Color(0x33FFFFFF))
                )

                // Grid Guide Toggle
                IconButton(
                    onClick = { showGridGuides = !showGridGuides },
                    modifier = Modifier.size(22.dp)
                ) {
                    Icon(
                        imageVector = if (showGridGuides) Icons.Default.GridOn else Icons.Default.GridOff,
                        contentDescription = "Toggle Grid Guides",
                        tint = if (showGridGuides) IosCyan else IosLabelSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                }

                // Aspect Fit Button
                IconButton(
                    onClick = {
                        // Reset zoom/pan or fit canvas
                        renderer?.let { /* triggered via state */ }
                    },
                    modifier = Modifier.size(22.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FitScreen,
                        contentDescription = "Fit to Screen",
                        tint = IosLabelSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        // ── 4. Interactive Touch Transform Gizmo Layer ───────────────────
        if (selectedLayerId != null && selectedLayer != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(selectedLayerId) {
                        detectTransformGestures { _, pan, zoom, rotation ->
                            onTransformChange(selectedLayerId, pan.x * 2.2f, pan.y * 2.2f, zoom, rotation)
                        }
                    }
            ) {
                val boxWidth = (160f * layerScaleX).coerceIn(40f, 440f).dp
                val boxHeight = (160f * layerScaleX).coerceIn(40f, 440f).dp

                Box(
                    modifier = Modifier
                        .size(boxWidth, boxHeight)
                        .align(Alignment.Center)
                        .graphicsLayer {
                            translationX = layerX * 0.4f
                            translationY = layerY * 0.4f
                            rotationZ = layerRot
                        }
                        .border(1.dp, IosActiveGlowBorder, RoundedCornerShape(6.dp))
                ) {
                    // Center Pivot Crosshair
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(12.dp).align(Alignment.Center)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val c = Offset(size.width / 2, size.height / 2)
                            drawLine(IosCyan, Offset(c.x - 4.dp.toPx(), c.y), Offset(c.x + 4.dp.toPx(), c.y), strokeWidth = 1.2f)
                            drawLine(IosCyan, Offset(c.x, c.y - 4.dp.toPx()), Offset(c.x, c.y + 4.dp.toPx()), strokeWidth = 1.2f)
                            drawCircle(Color.White, radius = 1.5.dp.toPx(), center = c)
                        }
                    }

                    // 4 Circular Glass Scale Pins
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color.White).border(1.dp, IosIndigo, CircleShape).align(Alignment.TopStart))
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color.White).border(1.dp, IosIndigo, CircleShape).align(Alignment.TopEnd))
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color.White).border(1.dp, IosIndigo, CircleShape).align(Alignment.BottomStart))
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color.White).border(1.dp, IosIndigo, CircleShape).align(Alignment.BottomEnd))

                    // Top Rotation Stalk Handle
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.align(Alignment.TopCenter).offset(y = (-24).dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(IosIndigo)
                                .border(1.dp, Color.White, CircleShape)
                        ) {
                            Text(text = "↻", color = Color.White, fontSize = 9.sp)
                        }
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(8.dp)
                                .background(IosIndigo)
                        )
                    }
                }
            }
        }
    }
}
