package com.novamotion.ui.canvas

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.novamotion.core.model.Project
import com.novamotion.core.model.evaluate
import com.novamotion.core.render.GLViewportRenderer
import com.novamotion.ui.theme.*

@Composable
fun CanvasViewport(
    project: Project,
    currentPlayheadMs: Long,
    selectedLayerId: String?,
    onTransformChange: (layerId: String, dx: Float, dy: Float, scale: Float, rotate: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var renderer by remember { mutableStateOf<GLViewportRenderer?>(null) }
    val selectedLayer = project.layers.find { it.id == selectedLayerId }

    // Evaluated transform values for visual bounding box gizmo
    val layerX = selectedLayer?.transform?.posX?.evaluate(currentPlayheadMs) ?: 0f
    val layerY = selectedLayer?.transform?.posY?.evaluate(currentPlayheadMs) ?: 0f
    val layerScaleX = selectedLayer?.transform?.scaleX?.evaluate(currentPlayheadMs) ?: 1f
    val layerRot = selectedLayer?.transform?.rotation?.evaluate(currentPlayheadMs) ?: 0f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioBackground)
            .clipToBounds()
    ) {
        // OpenGL ES 3.0 Surface View — RENDERMODE_WHEN_DIRTY saves GPU power at idle
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
                    // Only render when requestRender() is called — avoids wasted frames
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
                // Request a new frame whenever project state or playhead changes
                view.requestRender()
            },
            modifier = Modifier.fillMaxSize()
        )

        // Viewport Overlay HUD (resolution + FPS badge)
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .background(StudioSurface.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                    .border(1.dp, StudioBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${project.width}x${project.height} (${project.fps} FPS)",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Row {
                IconButton(onClick = {}, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.FitScreen,
                        contentDescription = "Fit to Screen",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(onClick = {}, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.AspectRatio,
                        contentDescription = "Aspect Ratio",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Interactive Touch Transform Gizmo Layer
        if (selectedLayerId != null && selectedLayer != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(selectedLayerId) {
                        detectTransformGestures { _, pan, zoom, rotation ->
                            // Scale pan delta based on density ratio
                            onTransformChange(selectedLayerId, pan.x * 2.2f, pan.y * 2.2f, zoom, rotation)
                        }
                    }
            ) {
                // Interactive Bounding Box positioned & scaled dynamically
                val boxWidth = (140f * layerScaleX).coerceIn(40f, 400f).dp
                val boxHeight = (140f * layerScaleX).coerceIn(40f, 400f).dp

                Box(
                    modifier = Modifier
                        .size(boxWidth, boxHeight)
                        .align(Alignment.Center)
                        .graphicsLayer {
                            translationX = layerX * 0.4f
                            translationY = layerY * 0.4f
                            rotationZ = layerRot
                        }
                        .border(1.5.dp, ElectricIndigo, RoundedCornerShape(4.dp))
                ) {
                    // Corner scale handles
                    Box(modifier = Modifier.size(10.dp).background(NeonCyan).align(Alignment.TopStart))
                    Box(modifier = Modifier.size(10.dp).background(NeonCyan).align(Alignment.TopEnd))
                    Box(modifier = Modifier.size(10.dp).background(NeonCyan).align(Alignment.BottomStart))
                    Box(modifier = Modifier.size(10.dp).background(NeonCyan).align(Alignment.BottomEnd))
                }
            }
        }
    }
}
