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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.novamotion.core.model.Project
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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioBackground)
            .clipToBounds()
    ) {
        // OpenGL Surface View
        AndroidView(
            factory = { context ->
                android.opengl.GLSurfaceView(context).apply {
                    setEGLContextClientVersion(3)
                    val r = GLViewportRenderer().apply {
                        this.currentProject = project
                        this.currentPlayheadMs = currentPlayheadMs
                    }
                    renderer = r
                    setRenderer(r)
                    renderMode = android.opengl.GLSurfaceView.RENDERMODE_CONTINUOUSLY
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = {
                renderer?.currentProject = project
                renderer?.currentPlayheadMs = currentPlayheadMs
            },
            modifier = Modifier.fillMaxSize()
        )

        // Viewport Overlay HUD (Safe Guides & Aspect Ratio)
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

        // Touch Transform Gizmo Layer
        if (selectedLayerId != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(selectedLayerId) {
                        detectTransformGestures { _, pan, zoom, rotation ->
                            onTransformChange(selectedLayerId, pan.x, pan.y, zoom, rotation)
                        }
                    }
            ) {
                // Interactive Bounding Box visual indicator
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .align(Alignment.Center)
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
