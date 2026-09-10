#ifndef NOVAMOTION_COLOR_LUT_H
#define NOVAMOTION_COLOR_LUT_H

#include <cstdint>

namespace novamotion {

/**
 * High performance trilinear 3D LUT sampler in C++.
 * Interpolates volumetric color grading cubes (e.g. 33x33x33 or 17x17x17).
 */
class ColorLut {
public:
    static void applyLut3D(
        uint32_t* rgbaPixels,
        int pixelCount,
        const float* lutData,
        int lutSize
    );
};

} // namespace novamotion

#endif // NOVAMOTION_COLOR_LUT_H
