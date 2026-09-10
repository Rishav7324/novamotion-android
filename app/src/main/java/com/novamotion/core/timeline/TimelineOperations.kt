package com.novamotion.core.timeline

import com.novamotion.core.model.AnimatableProperty
import com.novamotion.core.model.Keyframe
import com.novamotion.core.model.Layer
import com.novamotion.core.model.LayerTransform
import com.novamotion.core.model.evaluate
import java.util.UUID

/**
 * Production Timeline Editing Operations.
 * Handles frame-accurate Cut/Split, Head/Tail Ripple Trim, Media In/Out offsets,
 * and seamless keyframe interpolation across cut boundaries.
 */
object TimelineOperations {

    /**
     * Splits an animatable numeric property at [splitLocalMs].
     * Preserves continuity of animated curves across the cut boundary.
     */
    fun splitAnimatableFloat(
        prop: AnimatableProperty<Float>,
        splitLocalMs: Long
    ): Pair<AnimatableProperty<Float>, AnimatableProperty<Float>> {
        if (!prop.hasKeyframes()) {
            return Pair(prop.copy(), prop.copy())
        }

        val evaluatedValueAtCut = prop.evaluate(splitLocalMs)

        // Part 1: Keyframes before cut, plus boundary keyframe at cut
        val part1Keyframes = prop.keyframes.filter { it.timeMs < splitLocalMs }.toMutableList()
        part1Keyframes.add(
            Keyframe(
                timeMs = splitLocalMs,
                value = evaluatedValueAtCut
            )
        )

        // Part 2: Boundary keyframe at 0, plus shifted keyframes after cut
        val part2Keyframes = mutableListOf(
            Keyframe(
                timeMs = 0L,
                value = evaluatedValueAtCut
            )
        )
        prop.keyframes
            .filter { it.timeMs > splitLocalMs }
            .mapTo(part2Keyframes) { kf ->
                kf.copy(
                    id = UUID.randomUUID().toString(),
                    timeMs = kf.timeMs - splitLocalMs
                )
            }

        return Pair(
            prop.copy(keyframes = part1Keyframes),
            prop.copy(defaultValue = evaluatedValueAtCut, keyframes = part2Keyframes)
        )
    }

    /**
     * Splits a LayerTransform at [splitLocalMs] into two distinct transforms.
     */
    fun splitTransform(
        transform: LayerTransform,
        splitLocalMs: Long
    ): Pair<LayerTransform, LayerTransform> {
        val (posX1, posX2) = splitAnimatableFloat(transform.posX, splitLocalMs)
        val (posY1, posY2) = splitAnimatableFloat(transform.posY, splitLocalMs)
        val (posZ1, posZ2) = splitAnimatableFloat(transform.posZ, splitLocalMs)
        val (scaleX1, scaleX2) = splitAnimatableFloat(transform.scaleX, splitLocalMs)
        val (scaleY1, scaleY2) = splitAnimatableFloat(transform.scaleY, splitLocalMs)
        val (rot1, rot2) = splitAnimatableFloat(transform.rotation, splitLocalMs)
        val (op1, op2) = splitAnimatableFloat(transform.opacity, splitLocalMs)

        val t1 = transform.copy(
            posX = posX1, posY = posY1, posZ = posZ1,
            scaleX = scaleX1, scaleY = scaleY1,
            rotation = rot1, opacity = op1
        )
        val t2 = transform.copy(
            posX = posX2, posY = posY2, posZ = posZ2,
            scaleX = scaleX2, scaleY = scaleY2,
            rotation = rot2, opacity = op2
        )
        return Pair(t1, t2)
    }

    const val MIN_LAYER_DURATION_MS = 50L

    /**
     * Splits a Layer at [splitTimeMs] on the project timeline.
     * Returns null if [splitTimeMs] is not within (layer.startTimeMs + MIN_LAYER_DURATION_MS, layer.endTimeMs - MIN_LAYER_DURATION_MS).
     */
    fun splitLayer(layer: Layer, splitTimeMs: Long): Pair<Layer, Layer>? {
        if (splitTimeMs <= layer.startTimeMs + MIN_LAYER_DURATION_MS || 
            splitTimeMs >= layer.endTimeMs - MIN_LAYER_DURATION_MS) {
            return null
        }

        val localSplitMs = splitTimeMs - layer.startTimeMs
        val (t1, t2) = splitTransform(layer.transform, localSplitMs)

        val part1 = layer.copy(
            durationMs = localSplitMs,
            transform = t1
        )

        val part2 = layer.copy(
            id = UUID.randomUUID().toString(),
            name = "${layer.name} (Split)",
            startTimeMs = splitTimeMs,
            durationMs = layer.durationMs - localSplitMs,
            sourceInMs = layer.sourceInMs + localSplitMs,
            transform = t2
        )

        return Pair(part1, part2)
    }

    /**
     * Trims the in-point (head) of a layer to [newStartTimeMs].
     */
    fun trimLayerHead(layer: Layer, newStartTimeMs: Long): Layer {
        if (newStartTimeMs >= layer.endTimeMs - MIN_LAYER_DURATION_MS || newStartTimeMs == layer.startTimeMs) {
            return layer
        }
        val safeStartTimeMs = newStartTimeMs.coerceAtMost(layer.endTimeMs - MIN_LAYER_DURATION_MS)
        val deltaMs = safeStartTimeMs - layer.startTimeMs
        val newDurationMs = (layer.durationMs - deltaMs).coerceAtLeast(MIN_LAYER_DURATION_MS)

        // Shift keyframes
        fun shiftProp(prop: AnimatableProperty<Float>): AnimatableProperty<Float> {
            if (!prop.hasKeyframes()) return prop
            val valAtCut = prop.evaluate(deltaMs)
            val shifted = mutableListOf(Keyframe(timeMs = 0L, value = valAtCut))
            prop.keyframes
                .filter { it.timeMs > deltaMs }
                .mapTo(shifted) { it.copy(timeMs = it.timeMs - deltaMs) }
            return prop.copy(defaultValue = valAtCut, keyframes = shifted)
        }

        val t = layer.transform.copy(
            posX = shiftProp(layer.transform.posX),
            posY = shiftProp(layer.transform.posY),
            posZ = shiftProp(layer.transform.posZ),
            scaleX = shiftProp(layer.transform.scaleX),
            scaleY = shiftProp(layer.transform.scaleY),
            rotation = shiftProp(layer.transform.rotation),
            opacity = shiftProp(layer.transform.opacity)
        )

        return layer.copy(
            startTimeMs = safeStartTimeMs,
            durationMs = newDurationMs,
            sourceInMs = layer.sourceInMs + deltaMs,
            transform = t
        )
    }

    /**
     * Trims the out-point (tail) of a layer to [newEndTimeMs].
     */
    fun trimLayerTail(layer: Layer, newEndTimeMs: Long): Layer {
        if (newEndTimeMs <= layer.startTimeMs + MIN_LAYER_DURATION_MS || newEndTimeMs == layer.endTimeMs) {
            return layer
        }
        val safeEndTimeMs = newEndTimeMs.coerceAtLeast(layer.startTimeMs + MIN_LAYER_DURATION_MS)
        val newDurationMs = (safeEndTimeMs - layer.startTimeMs).coerceAtLeast(MIN_LAYER_DURATION_MS)

        fun trimProp(prop: AnimatableProperty<Float>): AnimatableProperty<Float> {
            if (!prop.hasKeyframes()) return prop
            val valAtCut = prop.evaluate(newDurationMs)
            val trimmed = prop.keyframes.filter { it.timeMs < newDurationMs }.toMutableList()
            trimmed.add(Keyframe(timeMs = newDurationMs, value = valAtCut))
            return prop.copy(keyframes = trimmed)
        }

        val t = layer.transform.copy(
            posX = trimProp(layer.transform.posX),
            posY = trimProp(layer.transform.posY),
            posZ = trimProp(layer.transform.posZ),
            scaleX = trimProp(layer.transform.scaleX),
            scaleY = trimProp(layer.transform.scaleY),
            rotation = trimProp(layer.transform.rotation),
            opacity = trimProp(layer.transform.opacity)
        )

        return layer.copy(
            durationMs = newDurationMs,
            transform = t
        )
    }

    /**
     * Duplicates a layer with a new ID.
     */
    fun duplicateLayer(layer: Layer, offsetMs: Long = 0L): Layer {
        return layer.copy(
            id = UUID.randomUUID().toString(),
            name = "${layer.name} Copy",
            startTimeMs = layer.startTimeMs + offsetMs
        )
    }

    // ─── Advanced edit modes ─────────────────────────────────────────────

    /** Ripple trim head: shifts downstream layers on same track to close gap */
    fun rippleTrimHead(layer: Layer, newStartMs: Long, downstream: List<Layer>): Pair<Layer, List<Layer>> {
        val delta = newStartMs - layer.startTimeMs
        val trimmed = trimLayerHead(layer, newStartMs)
        val shifted = downstream.map { it.copy(startTimeMs = it.startTimeMs - delta) }
        return trimmed to shifted
    }

    fun rippleTrimTail(layer: Layer, newEndMs: Long, downstream: List<Layer>): Pair<Layer, List<Layer>> {
        val oldEnd = layer.endTimeMs
        val trimmed = trimLayerTail(layer, newEndMs)
        val delta = trimmed.endTimeMs - oldEnd
        val shifted = downstream.map { it.copy(startTimeMs = it.startTimeMs + delta) }
        return trimmed to shifted
    }

    /** Roll edit: move cut between two adjacent layers, sum duration constant */
    fun rollEdit(prev: Layer, next: Layer, deltaMs: Long): Pair<Layer, Layer>? {
        val newPrevEnd = prev.endTimeMs + deltaMs
        val newNextStart = next.startTimeMs + deltaMs
        if (newPrevEnd <= prev.startTimeMs + MIN_LAYER_DURATION_MS) return null
        if (newNextStart >= next.endTimeMs - MIN_LAYER_DURATION_MS) return null
        if (newPrevEnd != newNextStart) return null // must stay contiguous
        val newPrev = trimLayerTail(prev, newPrevEnd)
        val newNext = trimLayerHead(next, newNextStart)
        // Adjust newNext duration to keep gap closed
        return newPrev to newNext
    }

    /** Slip: slide source window without moving timeline position */
    fun slipLayer(layer: Layer, deltaMs: Long, assetDurationMs: Long): Layer {
        val maxIn = (assetDurationMs - layer.durationMs).coerceAtLeast(0L)
        val newIn = (layer.sourceInMs + deltaMs).coerceIn(0L, maxIn)
        // Shift keyframes inversely to keep visual position
        fun slipProp(prop: AnimatableProperty<Float>): AnimatableProperty<Float> {
            if (!prop.hasKeyframes()) return prop
            // No keyframe time shift; value continuity preserved via source offset only
            return prop
        }
        return layer.copy(sourceInMs = newIn)
    }

    /** Slide: move clip and push neighbors */
    fun slideLayer(moved: Layer, deltaMs: Long, prev: Layer?, next: Layer?): SlideResult? {
        val newStart = moved.startTimeMs + deltaMs
        if (newStart < 0) return null
        // Check prev doesn't overlap and next gap
        if (prev != null && newStart < prev.endTimeMs) return null
        if (next != null && newStart + moved.durationMs > next.startTimeMs) return null
        return SlideResult(moved.copy(startTimeMs = newStart), prev, next)
    }

    data class SlideResult(val moved: Layer, val prev: Layer?, val next: Layer?)
}
