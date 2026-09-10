package com.novamotion.core.preset

import com.novamotion.core.model.*
import org.json.JSONArray
import org.json.JSONObject

/**
 * Production-grade JSON serializer for NovaMotion projects.
 *
 * Previous implementation only serialized posX keyframes, silently dropping
 * all other animated properties. This version serializes the complete
 * LayerTransform (posX, posY, posZ, scaleX, scaleY, rotation, opacity, pivot).
 */
object ProjectSerializer {

    fun serialize(project: Project): String {
        val json = JSONObject()
        json.put("id", project.id)
        json.put("title", project.title)
        json.put("width", project.width)
        json.put("height", project.height)
        json.put("fps", project.fps)
        json.put("durationMs", project.durationMs)
        json.put("backgroundColor", project.backgroundColor.toString())

        val layersArray = JSONArray()
        for (layer in project.layers) {
            val layerJson = JSONObject()
            layerJson.put("id", layer.id)
            layerJson.put("name", layer.name)
            layerJson.put("type", layer.type.name)
            layerJson.put("startTimeMs", layer.startTimeMs)
            layerJson.put("durationMs", layer.durationMs)
            layerJson.put("isVisible", layer.isVisible)
            layerJson.put("isLocked", layer.isLocked)
            layerJson.put("parentLayerId", layer.parentLayerId ?: "")
            layerJson.put("mediaUri", layer.mediaUri ?: "")
            layerJson.put("textContent", layer.textContent)
            layerJson.put("textColor", layer.textColor.toString())
            layerJson.put("kineticPreset", layer.kineticPreset)
            layerJson.put("fontSize", layer.fontSize.toDouble())
            layerJson.put("letterSpacing", layer.letterSpacing.toDouble())
            layerJson.put("shadowRadius", layer.shadowRadius.toDouble())
            layerJson.put("shapeType", layer.shapeType)
            layerJson.put("fillColor", layer.fillColor.toString())

            // Serialize complete transform
            val transformJson = JSONObject()
            transformJson.put("pivotX", layer.transform.pivotX.toDouble())
            transformJson.put("pivotY", layer.transform.pivotY.toDouble())

            val properties = mapOf(
                "posX"     to layer.transform.posX,
                "posY"     to layer.transform.posY,
                "posZ"     to layer.transform.posZ,
                "scaleX"   to layer.transform.scaleX,
                "scaleY"   to layer.transform.scaleY,
                "rotation" to layer.transform.rotation,
                "opacity"  to layer.transform.opacity
            )

            for ((propName, prop) in properties) {
                val propJson = JSONObject()
                propJson.put("defaultValue", prop.defaultValue.toDouble())
                val kfArray = JSONArray()
                for (kf in prop.keyframes) {
                    val kfJson = JSONObject()
                    kfJson.put("id", kf.id)
                    kfJson.put("timeMs", kf.timeMs)
                    kfJson.put("value", kf.value.toDouble())
                    kfJson.put("curve_x1", kf.curve.x1.toDouble())
                    kfJson.put("curve_y1", kf.curve.y1.toDouble())
                    kfJson.put("curve_x2", kf.curve.x2.toDouble())
                    kfJson.put("curve_y2", kf.curve.y2.toDouble())
                    kfArray.put(kfJson)
                }
                propJson.put("keyframes", kfArray)
                transformJson.put(propName, propJson)
            }
            layerJson.put("transform", transformJson)

            // Effects
            val effectsArray = JSONArray()
            for (effect in layer.effects) {
                val effectJson = JSONObject()
                effectJson.put("id", effect.id)
                effectJson.put("type", effect.type.name)
                effectJson.put("isEnabled", effect.isEnabled)
                val paramsJson = JSONObject()
                for ((key, param) in effect.parameters) {
                    val paramJson = JSONObject()
                    paramJson.put("key", param.key)
                    paramJson.put("name", param.name)
                    paramJson.put("value", param.value.toDouble())
                    paramJson.put("min", param.min.toDouble())
                    paramJson.put("max", param.max.toDouble())
                    paramsJson.put(key, paramJson)
                }
                effectJson.put("parameters", paramsJson)
                effectsArray.put(effectJson)
            }
            layerJson.put("effects", effectsArray)

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
        val backgroundColor = json.optString("backgroundColor", "0xFF0A0B0EL").toLongOrNull() ?: 0xFF0A0B0EL

        val layers = mutableListOf<Layer>()
        val layersArray = json.optJSONArray("layers")
        if (layersArray != null) {
            for (i in 0 until layersArray.length()) {
                val layerJson = layersArray.getJSONObject(i)
                val layerId = layerJson.optString("id", java.util.UUID.randomUUID().toString())
                val name = layerJson.optString("name", "Layer $i")
                val type = try { LayerType.valueOf(layerJson.optString("type", "SHAPE")) } catch (e: Exception) { LayerType.SHAPE }
                val startMs = layerJson.optLong("startTimeMs", 0L)
                val durMs = layerJson.optLong("durationMs", 5000L)
                val isVisible = layerJson.optBoolean("isVisible", true)
                val isLocked = layerJson.optBoolean("isLocked", false)
                val parentLayerId = layerJson.optString("parentLayerId").takeIf { it.isNotEmpty() }
                val mediaUri = layerJson.optString("mediaUri").takeIf { it.isNotEmpty() }
                val textContent = layerJson.optString("textContent", "NovaMotion")
                val textColor = layerJson.optString("textColor", "0xFFFFFFFF").toLongOrNull() ?: 0xFFFFFFFF
                val kineticPreset = layerJson.optString("kineticPreset", "SPRING_POP")
                val fontSize = layerJson.optDouble("fontSize", 64.0).toFloat()
                val letterSpacing = layerJson.optDouble("letterSpacing", 0.05).toFloat()
                val shadowRadius = layerJson.optDouble("shadowRadius", 8.0).toFloat()
                val shapeType = layerJson.optString("shapeType", "RECTANGLE")
                val fillColor = layerJson.optString("fillColor", "0xFF6366F1").toLongOrNull() ?: 0xFF6366F1

                val transform = deserializeTransform(layerJson.optJSONObject("transform"))
                val effects = deserializeEffects(layerJson.optJSONArray("effects"))

                layers.add(Layer(
                    id = layerId,
                    name = name,
                    type = type,
                    startTimeMs = startMs,
                    durationMs = durMs,
                    isVisible = isVisible,
                    isLocked = isLocked,
                    parentLayerId = parentLayerId,
                    mediaUri = mediaUri,
                    textContent = textContent,
                    textColor = textColor,
                    kineticPreset = kineticPreset,
                    fontSize = fontSize,
                    letterSpacing = letterSpacing,
                    shadowRadius = shadowRadius,
                    shapeType = shapeType,
                    fillColor = fillColor,
                    transform = transform,
                    effects = effects
                ))
            }
        }

        return Project(
            id = id,
            title = title,
            width = width,
            height = height,
            fps = fps,
            durationMs = durationMs,
            backgroundColor = backgroundColor,
            layers = layers
        )
    }

    private fun deserializeTransform(json: JSONObject?): LayerTransform {
        if (json == null) return LayerTransform()
        val pivotX = json.optDouble("pivotX", 0.5).toFloat()
        val pivotY = json.optDouble("pivotY", 0.5).toFloat()

        return LayerTransform(
            posX     = deserializeProperty(json.optJSONObject("posX"), 0f),
            posY     = deserializeProperty(json.optJSONObject("posY"), 0f),
            posZ     = deserializeProperty(json.optJSONObject("posZ"), 0f),
            scaleX   = deserializeProperty(json.optJSONObject("scaleX"), 1f),
            scaleY   = deserializeProperty(json.optJSONObject("scaleY"), 1f),
            rotation = deserializeProperty(json.optJSONObject("rotation"), 0f),
            opacity  = deserializeProperty(json.optJSONObject("opacity"), 1f),
            pivotX   = pivotX,
            pivotY   = pivotY
        )
    }

    private fun deserializeProperty(json: JSONObject?, default: Float): AnimatableProperty<Float> {
        if (json == null) return AnimatableProperty(default)
        val defaultValue = json.optDouble("defaultValue", default.toDouble()).toFloat()
        val kfArray = json.optJSONArray("keyframes")
        val keyframes = mutableListOf<Keyframe<Float>>()
        if (kfArray != null) {
            for (i in 0 until kfArray.length()) {
                val kfJson = kfArray.getJSONObject(i)
                keyframes.add(Keyframe(
                    id = kfJson.optString("id", java.util.UUID.randomUUID().toString()),
                    timeMs = kfJson.getLong("timeMs"),
                    value = kfJson.getDouble("value").toFloat(),
                    curve = BezierControlPoints(
                        x1 = kfJson.optDouble("curve_x1", 0.42).toFloat(),
                        y1 = kfJson.optDouble("curve_y1", 0.0).toFloat(),
                        x2 = kfJson.optDouble("curve_x2", 0.58).toFloat(),
                        y2 = kfJson.optDouble("curve_y2", 1.0).toFloat()
                    )
                ))
            }
        }
        return AnimatableProperty(defaultValue, keyframes)
    }

    private fun deserializeEffects(json: JSONArray?): List<VisualEffect> {
        if (json == null) return emptyList()
        val effects = mutableListOf<VisualEffect>()
        for (i in 0 until json.length()) {
            try {
                val effectJson = json.getJSONObject(i)
                val id = effectJson.optString("id", java.util.UUID.randomUUID().toString())
                val type = EffectType.valueOf(effectJson.optString("type", "MOTION_BLUR"))
                val isEnabled = effectJson.optBoolean("isEnabled", true)
                val paramsObj = effectJson.optJSONObject("parameters")
                val params = mutableMapOf<String, EffectParameter>()
                if (paramsObj != null) {
                    for (key in paramsObj.keys()) {
                        val p = paramsObj.getJSONObject(key)
                        params[key] = EffectParameter(
                            key = p.optString("key", key),
                            name = p.optString("name", key),
                            value = p.optDouble("value", 0.5).toFloat(),
                            min = p.optDouble("min", 0.0).toFloat(),
                            max = p.optDouble("max", 1.0).toFloat()
                        )
                    }
                }
                effects.add(VisualEffect(id = id, type = type, isEnabled = isEnabled, parameters = params))
            } catch (e: Exception) {
                // Skip malformed effect
            }
        }
        return effects
    }
}
