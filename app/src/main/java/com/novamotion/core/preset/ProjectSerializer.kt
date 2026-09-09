package com.novamotion.core.preset

import com.novamotion.core.model.*
import org.json.JSONArray
import org.json.JSONObject

object ProjectSerializer {

    fun serialize(project: Project): String {
        val json = JSONObject()
        json.put("id", project.id)
        json.put("title", project.title)
        json.put("width", project.width)
        json.put("height", project.height)
        json.put("fps", project.fps)
        json.put("durationMs", project.durationMs)

        val layersArray = JSONArray()
        for (layer in project.layers) {
            val layerJson = JSONObject()
            layerJson.put("id", layer.id)
            layerJson.put("name", layer.name)
            layerJson.put("type", layer.type.name)
            layerJson.put("startTimeMs", layer.startTimeMs)
            layerJson.put("durationMs", layer.durationMs)
            layerJson.put("mediaUri", layer.mediaUri ?: "")
            layerJson.put("textContent", layer.textContent)

            // Keyframes for posX
            val keyframesArray = JSONArray()
            for (k in layer.transform.posX.keyframes) {
                val kJson = JSONObject()
                kJson.put("timeMs", k.timeMs)
                kJson.put("value", k.value.toDouble())
                kJson.put("curve_x1", k.curve.x1.toDouble())
                kJson.put("curve_y1", k.curve.y1.toDouble())
                kJson.put("curve_x2", k.curve.x2.toDouble())
                kJson.put("curve_y2", k.curve.y2.toDouble())
                keyframesArray.put(kJson)
            }
            layerJson.put("keyframes_posX", keyframesArray)
            layersArray.put(layerJson)
        }
        json.put("layers", layersArray)

        return json.toString(2)
    }

    fun deserialize(jsonStr: String): Project {
        val json = JSONObject(jsonStr)
        val id = json.optString("id", java.util.UUID.randomUUID().toString())
        val title = json.optString("title", "Imported Project")
        val width = json.optInt("width", 1080)
        val height = json.optInt("height", 1920)
        val fps = json.optInt("fps", 60)
        val durationMs = json.optLong("durationMs", 10000L)

        val layers = mutableListOf<Layer>()
        val layersArray = json.optJSONArray("layers")
        if (layersArray != null) {
            for (i in 0 until layersArray.length()) {
                val layerJson = layersArray.getJSONObject(i)
                val layerId = layerJson.optString("id", java.util.UUID.randomUUID().toString())
                val name = layerJson.optString("name", "Layer $i")
                val type = LayerType.valueOf(layerJson.optString("type", "SHAPE"))
                val startMs = layerJson.optLong("startTimeMs", 0L)
                val durMs = layerJson.optLong("durationMs", 5000L)
                val mediaUri = layerJson.optString("mediaUri").takeIf { it.isNotEmpty() }
                val textContent = layerJson.optString("textContent", "NovaMotion")

                val kArray = layerJson.optJSONArray("keyframes_posX")
                val keyframes = mutableListOf<Keyframe<Float>>()
                if (kArray != null) {
                    for (j in 0 until kArray.length()) {
                        val kObj = kArray.getJSONObject(j)
                        keyframes.add(
                            Keyframe(
                                timeMs = kObj.getLong("timeMs"),
                                value = kObj.getDouble("value").toFloat(),
                                curve = BezierControlPoints(
                                    x1 = kObj.getDouble("curve_x1").toFloat(),
                                    y1 = kObj.getDouble("curve_y1").toFloat(),
                                    x2 = kObj.getDouble("curve_x2").toFloat(),
                                    y2 = kObj.getDouble("curve_y2").toFloat()
                                )
                            )
                        )
                    }
                }

                layers.add(
                    Layer(
                        id = layerId,
                        name = name,
                        type = type,
                        startTimeMs = startMs,
                        durationMs = durMs,
                        mediaUri = mediaUri,
                        textContent = textContent,
                        transform = LayerTransform(
                            posX = AnimatableProperty(0f, keyframes)
                        )
                    )
                )
            }
        }

        return Project(
            id = id,
            title = title,
            width = width,
            height = height,
            fps = fps,
            durationMs = durationMs,
            layers = layers
        )
    }
}
