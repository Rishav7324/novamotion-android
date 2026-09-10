#include "spline_interpolator.h"

namespace novamotion {

float SplineInterpolator::solveCurveX(float x, float x1, float x2) {
    float t2 = x;
    // Newton-Raphson fast iterations (usually converges in 4-6 iterations)
    for (int i = 0; i < 8; i++) {
        float x2_est = sampleCurveX(t2, x1, x2) - x;
        if (std::abs(x2_est) < 1e-6f) {
            return t2;
        }
        float d2 = sampleCurveDerivativeX(t2, x1, x2);
        if (std::abs(d2) < 1e-6f) {
            break;
        }
        t2 -= x2_est / d2;
    }

    // Binary subdivision fallback if Newton-Raphson diverges
    float t0 = 0.0f;
    float t1 = 1.0f;
    t2 = x;

    while (t0 < t1) {
        float x2_est = sampleCurveX(t2, x1, x2);
        if (std::abs(x2_est - x) < 1e-6f) {
            return t2;
        }
        if (x > x2_est) {
            t0 = t2;
        } else {
            t1 = t2;
        }
        t2 = (t1 - t0) * 0.5f + t0;
    }

    return t2;
}

float SplineInterpolator::evaluateBezier(float t, float x1, float y1, float x2, float y2) {
    if (t <= 0.0f) return 0.0f;
    if (t >= 1.0f) return 1.0f;

    // Linear curve optimization
    if (x1 == y1 && x2 == y2) return t;

    float solvedT = solveCurveX(t, x1, x2);
    return sampleCurveY(solvedT, y1, y2);
}

} // namespace novamotion
