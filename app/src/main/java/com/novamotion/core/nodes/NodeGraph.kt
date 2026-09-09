package com.novamotion.core.nodes

enum class NodeType(val label: String) {
    SOURCE("Media Source"),
    SHADER_EFFECT("VFX Shader"),
    MASK("Alpha Mask"),
    TRANSFORM("3D Transform"),
    MERGE("Merge & Blend"),
    OUTPUT("Final Output")
}

data class NodePin(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val isOutput: Boolean
)

data class CompositorNode(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val type: NodeType,
    val posX: Float = 0f,
    val posY: Float = 0f,
    val inputPins: List<NodePin> = emptyList(),
    val outputPins: List<NodePin> = emptyList()
)

data class NodeConnection(
    val fromNodeId: String,
    val fromPinId: String,
    val toNodeId: String,
    val toPinId: String
)

class NodeGraph {
    val nodes = mutableListOf<CompositorNode>()
    val connections = mutableListOf<NodeConnection>()

    fun addNode(node: CompositorNode) {
        nodes.add(node)
    }

    fun connect(fromNodeId: String, fromPinId: String, toNodeId: String, toPinId: String) {
        // Prevent duplicate connections to the same input pin
        connections.removeAll { it.toNodeId == toNodeId && it.toPinId == toPinId }
        connections.add(NodeConnection(fromNodeId, fromPinId, toNodeId, toPinId))
    }

    /**
     * Topological Sort (Kahn's Algorithm) to determine the exact GPU render order.
     */
    fun computeExecutionOrder(): List<CompositorNode> {
        val inDegree = mutableMapOf<String, Int>()
        val adjList = mutableMapOf<String, MutableList<String>>()

        for (node in nodes) {
            inDegree[node.id] = 0
            adjList[node.id] = mutableListOf()
        }

        for (conn in connections) {
            adjList[conn.fromNodeId]?.add(conn.toNodeId)
            inDegree[conn.toNodeId] = (inDegree[conn.toNodeId] ?: 0) + 1
        }

        val queue = ArrayDeque<String>()
        for ((nodeId, deg) in inDegree) {
            if (deg == 0) queue.add(nodeId)
        }

        val order = mutableListOf<CompositorNode>()
        while (queue.isNotEmpty()) {
            val currId = queue.removeFirst()
            nodes.find { it.id == currId }?.let { order.add(it) }

            adjList[currId]?.forEach { neighborId ->
                val newDeg = (inDegree[neighborId] ?: 1) - 1
                inDegree[neighborId] = newDeg
                if (newDeg == 0) queue.add(neighborId)
            }
        }

        return order
    }
}
