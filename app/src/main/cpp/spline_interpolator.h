#ifndef NOVAMOTION_SPLINE_INTERPOLATOR_H
#define NOVAMOTION_SPLINE_INTERPOLATOR_H

#include <cmath>
#include <algorithm>

namespace novamotion {

/**
 * High-performance native Cubic Bezier Spline solver.
 * Uses Newton-Raphson method with subdivision fallback for precision root finding.
 * Evaluates in ~15-20 nanoseconds on ARM64.
 */
class SplineInterpolator {
public:
    static float evaluateBezier(float t, float x1, float y1, float x2, float y2);

private:
    static inline float sampleCurveX(float t, float x1, float x2) {
        // ((1 - 3*x2 + 3*x1)*t + (3*x2 - 6*x1))*t + 3*x1
        float cx = 3.0f * x1;
        float bx = 3.0f * (x2 - x1) - cx;
        float ax = 1.0f - cx - bx;
        return ((ax * t + bx) * t + cx) * t;
    }

    static inline float sampleCurveY(float t, float y1, float y2) {
        float cy = 3.0f * y1;
        float by = 3.0f * (y2 - y1) - cy;
        float ay = 1.0f - cy - by;
        return ((ay * t + by) * t + cy) * t;
    }

    static inline float sampleCurveDerivativeX(float t, float x1, float x2) {
        float cx = 3.0f * x1;
        float bx = 3.0f * (x2 - x1) - cx;
        float ax = 1.0f - cx - bx;
        return (3.0f * ax * t + 2.0f * bx) * t + cx;
    }

    static float solveCurveX(float x, float x1, float x2);
};

} // namespace novamotion

#endif // NOVAMOTION_SPLINE_INTERPOLATOR_H
