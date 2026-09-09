package com.novamotion.ui.nodegraph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novamotion.core.nodes.CompositorNode
import com.novamotion.core.nodes.NodeConnection
import com.novamotion.core.nodes.NodeGraph
import com.novamotion.core.nodes.NodePin
import com.novamotion.core.nodes.NodeType
import com.novamotion.ui.theme.*

@Composable
fun NodeGraphCanvas(
    modifier: Modifier = Modifier
) {
    // Sample default VFX node graph
    val nodeGraph = remember {
        NodeGraph().apply {
            val src = CompositorNode(id = "node_src", name = "Video Source", type = NodeType.SOURCE, posX = 60f, posY = 80f,
                outputPins = listOf(NodePin(id = "p_src_out", name = "RGBA Out", isOutput = true)))
            val blur = CompositorNode(id = "node_fx", name = "Motion Blur", type = NodeType.SHADER_EFFECT, posX = 260f, posY = 80f,
                inputPins = listOf(NodePin(id = "p_fx_in", name = "RGBA In", isOutput = false)),
                outputPins = listOf(NodePin(id = "p_fx_out", name = "Out", isOutput = true)))
            val out = CompositorNode(id = "node_out", name = "Master Display", type = NodeType.OUTPUT, posX = 460f, posY = 80f,
                inputPins = listOf(NodePin(id = "p_out_in", name = "Input", isOutput = false)))

            addNode(src)
            addNode(blur)
            addNode(out)
            connect("node_src", "p_src_out", "node_fx", "p_fx_in")
            connect("node_fx", "p_fx_out", "node_out", "p_out_in")
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBackground)
    ) {
        // Render Curved Connecting Cables
        Canvas(modifier = Modifier.fillMaxSize()) {
            for (conn in nodeGraph.connections) {
                val from = nodeGraph.nodes.find { it.id == conn.fromNodeId } ?: continue
                val to = nodeGraph.nodes.find { it.id == conn.toNodeId } ?: continue

                val start = Offset((from.posX + 150f).dp.toPx(), (from.posY + 35f).dp.toPx())
                val end = Offset(to.posX.dp.toPx(), (to.posY + 35f).dp.toPx())

                val control1 = Offset(start.x + 60.dp.toPx(), start.y)
                val control2 = Offset(end.x - 60.dp.toPx(), end.y)

                val path = Path().apply {
                    moveTo(start.x, start.y)
                    cubicTo(control1.x, control1.y, control2.x, control2.y, end.x, end.y)
                }

                drawPath(path, color = NeonCyan, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
            }
        }

        // Render Node Cards
        for (node in nodeGraph.nodes) {
            NodeCard(node = node)
        }
    }
}

@Composable
private fun NodeCard(node: CompositorNode) {
    var pos by remember { mutableStateOf(Offset(node.posX, node.posY)) }

    val headerColor = when (node.type) {
        NodeType.SOURCE -> PurpleVideo
        NodeType.SHADER_EFFECT -> ElectricIndigo
        NodeType.MASK -> NeonCyan
        NodeType.TRANSFORM -> AmberText
        NodeType.MERGE -> OrangeAdjustment
        NodeType.OUTPUT -> EmeraldAudio
    }

    Box(
        modifier = Modifier
            .offset(x = pos.x.dp, y = pos.y.dp)
            .width(150.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(StudioSurface)
            .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    pos += Offset(dragAmount.x / 2.5f, dragAmount.y / 2.5f)
                }
            }
            .padding(8.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(headerColor, CircleShape))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = node.name, color = TextPrimary, fontSize = 12.sp, style = MaterialTheme.typography.titleSmall)
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(text = node.type.label, color = TextMuted, fontSize = 9.sp)
        }
    }
}
