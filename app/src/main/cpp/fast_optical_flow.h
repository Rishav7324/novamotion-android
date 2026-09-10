#ifndef NOVAMOTION_FAST_OPTICAL_FLOW_H
#define NOVAMOTION_FAST_OPTICAL_FLOW_H

#include <cstdint>
#include <utility>

namespace novamotion {

/**
 * Fast SIMD/C++ Sum of Absolute Differences (SAD) block-matching motion tracker.
 */
class FastOpticalFlow {
public:
    static std::pair<float, float> trackPatch(
        const uint8_t* templateLuma,
        int templateWidth,
        int templateHeight,
        const uint8_t* searchWindowLuma,
        int searchWidth,
        int searchHeight,
        int startX,
        int startY,
        int maxSearchRange = 16
    );
};

} // namespace novamotion

#endif // NOVAMOTION_FAST_OPTICAL_FLOW_H
