#include "fast_optical_flow.h"
#include <cmath>
#include <climits>
#include <algorithm>

namespace novamotion {

std::pair<float, float> FastOpticalFlow::trackPatch(
    const uint8_t* templateLuma,
    int templateWidth,
    int templateHeight,
    const uint8_t* searchWindowLuma,
    int searchWidth,
    int searchHeight,
    int startX,
    int startY,
    int maxSearchRange
) {
    int64_t bestSAD = INT64_MAX;
    int bestDx = 0;
    int bestDy = 0;

    for (int dy = -maxSearchRange; dy <= maxSearchRange; ++dy) {
        int curY = startY + dy;
        if (curY < 0 || curY + templateHeight > searchHeight) continue;

        for (int dx = -maxSearchRange; dx <= maxSearchRange; ++dx) {
            int curX = startX + dx;
            if (curX < 0 || curX + templateWidth > searchWidth) continue;

            int64_t sad = 0;
            for (int ty = 0; ty < templateHeight; ++ty) {
                const uint8_t* tRow = templateLuma + ty * templateWidth;
                const uint8_t* sRow = searchWindowLuma + (curY + ty) * searchWidth + curX;

                // Unrolled inner loop for compiler auto-vectorization (NEON)
                int tx = 0;
                for (; tx + 3 < templateWidth; tx += 4) {
                    sad += std::abs(static_cast<int>(tRow[tx])     - static_cast<int>(sRow[tx]));
                    sad += std::abs(static_cast<int>(tRow[tx + 1]) - static_cast<int>(sRow[tx + 1]));
                    sad += std::abs(static_cast<int>(tRow[tx + 2]) - static_cast<int>(sRow[tx + 2]));
                    sad += std::abs(static_cast<int>(tRow[tx + 3]) - static_cast<int>(sRow[tx + 3]));
                }
                for (; tx < templateWidth; ++tx) {
                    sad += std::abs(static_cast<int>(tRow[tx]) - static_cast<int>(sRow[tx]));
                }
            }

            if (sad < bestSAD) {
                bestSAD = sad;
                bestDx = dx;
                bestDy = dy;
            }
        }
    }

    return { static_cast<float>(bestDx), static_cast<float>(bestDy) };
}

} // namespace novamotion
