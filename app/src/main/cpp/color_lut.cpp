#include "color_lut.h"
#include <algorithm>
#include <cmath>

namespace novamotion {

void ColorLut::applyLut3D(
    uint32_t* rgbaPixels,
    int pixelCount,
    const float* lutData,
    int lutSize
) {
    if (pixelCount <= 0 || !lutData || lutSize <= 1) return;

    const float maxIdx = static_cast<float>(lutSize - 1);
    const int lutSize2 = lutSize * lutSize;

    for (int i = 0; i < pixelCount; ++i) {
        uint32_t p = rgbaPixels[i];
        float r = (p & 0xFF) / 255.0f;
        float g = ((p >> 8) & 0xFF) / 255.0f;
        float b = ((p >> 16) & 0xFF) / 255.0f;
        uint32_t a = (p >> 24) & 0xFF;

        float rx = r * maxIdx;
        float gy = g * maxIdx;
        float bz = b * maxIdx;

        int r0 = std::min(static_cast<int>(rx), lutSize - 2);
        int g0 = std::min(static_cast<int>(gy), lutSize - 2);
        int b0 = std::min(static_cast<int>(bz), lutSize - 2);

        float dr = rx - r0;
        float dg = gy - g0;
        float db = bz - b0;

        auto getLut = [&](int red, int green, int blue, int channel) -> float {
            int index = (blue * lutSize2 + green * lutSize + red) * 3 + channel;
            return lutData[index];
        };

        float outR = 0.0f;
        float outG = 0.0f;
        float outB = 0.0f;

        // Trilinear interpolation across 8 cube vertices
        for (int c = 0; c < 3; ++c) {
            float c000 = getLut(r0,     g0,     b0,     c);
            float c100 = getLut(r0 + 1, g0,     b0,     c);
            float c010 = getLut(r0,     g0 + 1, b0,     c);
            float c110 = getLut(r0 + 1, g0 + 1, b0,     c);
            float c001 = getLut(r0,     g0,     b0 + 1, c);
            float c101 = getLut(r0 + 1, g0,     b0 + 1, c);
            float c011 = getLut(r0,     g0 + 1, b0 + 1, c);
            float c111 = getLut(r0 + 1, g0 + 1, b0 + 1, c);

            float c00 = c000 * (1.0f - dr) + c100 * dr;
            float c10 = c010 * (1.0f - dr) + c110 * dr;
            float c01 = c001 * (1.0f - dr) + c101 * dr;
            float c11 = c011 * (1.0f - dr) + c111 * dr;

            float c0 = c00 * (1.0f - dg) + c10 * dg;
            float c1 = c01 * (1.0f - dg) + c11 * dg;

            float val = c0 * (1.0f - db) + c1 * db;
            if (c == 0) outR = val;
            else if (c == 1) outG = val;
            else outB = val;
        }

        uint32_t nr = std::clamp(static_cast<int>(outR * 255.0f + 0.5f), 0, 255);
        uint32_t ng = std::clamp(static_cast<int>(outG * 255.0f + 0.5f), 0, 255);
        uint32_t nb = std::clamp(static_cast<int>(outB * 255.0f + 0.5f), 0, 255);

        rgbaPixels[i] = nr | (ng << 8) | (nb << 16) | (a << 24);
    }
}

} // namespace novamotion
