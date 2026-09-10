#include <jni.h>
#include <string>
#include <vector>
#include "spline_interpolator.h"
#include "fast_optical_flow.h"
#include "audio_dsp.h"
#include "color_lut.h"

extern "C" {

JNIEXPORT jstring JNICALL
Java_com_novamotion_core_nativedrive_NativeBridge_getNativeEngineVersion(
    JNIEnv* env,
    jobject /* this */
) {
    return env->NewStringUTF("NovaMotion Native Engine v2.1.0-SIMD (C++17/ARM64)");
}

JNIEXPORT jfloat JNICALL
Java_com_novamotion_core_nativedrive_NativeBridge_evaluateBezierNative(
    JNIEnv* env,
    jobject /* this */,
    jfloat t,
    jfloat x1,
    jfloat y1,
    jfloat x2,
    jfloat y2
) {
    return novamotion::SplineInterpolator::evaluateBezier(t, x1, y1, x2, y2);
}

JNIEXPORT jfloatArray JNICALL
Java_com_novamotion_core_nativedrive_NativeBridge_trackPatchNative(
    JNIEnv* env,
    jobject /* this */,
    jbyteArray templateLuma,
    jint templateWidth,
    jint templateHeight,
    jbyteArray searchWindowLuma,
    jint searchWidth,
    jint searchHeight,
    jint startX,
    jint startY,
    jint maxSearchRange
) {
    jbyte* tBytes = env->GetByteArrayElements(templateLuma, nullptr);
    jbyte* sBytes = env->GetByteArrayElements(searchWindowLuma, nullptr);

    auto result = novamotion::FastOpticalFlow::trackPatch(
        reinterpret_cast<const uint8_t*>(tBytes),
        templateWidth,
        templateHeight,
        reinterpret_cast<const uint8_t*>(sBytes),
        searchWidth,
        searchHeight,
        startX,
        startY,
        maxSearchRange
    );

    env->ReleaseByteArrayElements(templateLuma, tBytes, JNI_ABORT);
    env->ReleaseByteArrayElements(searchWindowLuma, sBytes, JNI_ABORT);

    jfloatArray out = env->NewFloatArray(2);
    jfloat buf[2] = { result.first, result.second };
    env->SetFloatArrayRegion(out, 0, 2, buf);
    return out;
}

JNIEXPORT void JNICALL
Java_com_novamotion_core_nativedrive_NativeBridge_computeSpectrumNative(
    JNIEnv* env,
    jobject /* this */,
    jfloatArray pcmSamples,
    jint sampleCount,
    jfloatArray outMagnitudes,
    jint outputBands
) {
    jfloat* pcm = env->GetFloatArrayElements(pcmSamples, nullptr);
    jfloat* out = env->GetFloatArrayElements(outMagnitudes, nullptr);

    novamotion::AudioDsp::computeMagnitudeSpectrum(pcm, sampleCount, out, outputBands);

    env->ReleaseFloatArrayElements(pcmSamples, pcm, JNI_ABORT);
    env->ReleaseFloatArrayElements(outMagnitudes, out, 0); // commit output
}

JNIEXPORT void JNICALL
Java_com_novamotion_core_nativedrive_NativeBridge_apply3DLutNative(
    JNIEnv* env,
    jobject /* this */,
    jintArray rgbaPixels,
    jint pixelCount,
    jfloatArray lutData,
    jint lutSize
) {
    jint* pixels = env->GetIntArrayElements(rgbaPixels, nullptr);
    jfloat* lut = env->GetFloatArrayElements(lutData, nullptr);

    novamotion::ColorLut::applyLut3D(
        reinterpret_cast<uint32_t*>(pixels),
        pixelCount,
        lut,
        lutSize
    );

    env->ReleaseIntArrayElements(rgbaPixels, pixels, 0); // commit output
    env->ReleaseFloatArrayElements(lutData, lut, JNI_ABORT);
}

} // extern "C"
