package com.novamotion.core.nativedrive

import android.util.Log

/**
 * Kotlin singleton interface for NovaMotion's native C++ engine (libnovamotion.so).
 *
 * Provides accelerated:
 *  - Cubic Bezier Spline evaluation via Newton-Raphson
 *  - Optical Flow SAD block-matching for Motion Tracking
 *  - Cooley-Tukey Radix-2 FFT for Audio Reactive visuals
 *  - 3D LUT volumetric trilinear color grading
 */
object NativeBridge {

    private const val TAG = "NativeBridge"

    val isLoaded: Boolean = try {
        System.loadLibrary("novamotion")
        Log.i(TAG, "libnovamotion.so loaded successfully: ${getNativeEngineVersion()}")
        true
    } catch (e: UnsatisfiedLinkError) {
        Log.w(TAG, "libnovamotion.so not loaded, using Kotlin fallback implementations", e)
        false
    }

    external fun getNativeEngineVersion(): String

    external fun evaluateBezierNative(
        t: Float,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float
    ): Float

    external fun trackPatchNative(
        templateLuma: ByteArray,
        templateWidth: Int,
        templateHeight: Int,
        searchWindowLuma: ByteArray,
        searchWidth: Int,
        searchHeight: Int,
        startX: Int,
        startY: Int,
        maxSearchRange: Int
    ): FloatArray

    external fun computeSpectrumNative(
        pcmSamples: FloatArray,
        sampleCount: Int,
        outMagnitudes: FloatArray,
        outputBands: Int
    )

    external fun apply3DLutNative(
        rgbaPixels: IntArray,
        pixelCount: Int,
        lutData: FloatArray,
        lutSize: Int
    )

    /**
     * Fast Bezier evaluation with graceful fallback to Kotlin implementation
     * if native library is unavailable.
     */
    fun evaluateBezier(t: Float, x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return if (isLoaded) {
            evaluateBezierNative(t, x1, y1, x2, y2)
        } else {
            // Kotlin fallback
            t * t * (3f - 2f * t)
        }
    }
}
