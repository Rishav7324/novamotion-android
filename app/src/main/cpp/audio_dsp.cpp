#include "audio_dsp.h"
#include <cmath>
#include <algorithm>

namespace novamotion {

constexpr float PI = 3.14159265358979323846f;

void AudioDsp::computeFft(std::vector<std::complex<float>>& x) {
    const size_t N = x.size();
    if (N <= 1) return;

    // Bit-reversal permutation
    size_t j = 0;
    for (size_t i = 0; i < N - 1; ++i) {
        if (i < j) {
            std::swap(x[i], x[j]);
        }
        size_t k = N / 2;
        while (k <= j) {
            j -= k;
            k /= 2;
        }
        j += k;
    }

    // Cooley-Tukey Radix-2 butterflies
    for (size_t len = 2; len <= N; len <<= 1) {
        float angle = -2.0f * PI / static_cast<float>(len);
        std::complex<float> wlen(std::cos(angle), std::sin(angle));
        for (size_t i = 0; i < N; i += len) {
            std::complex<float> w(1.0f, 0.0f);
            for (size_t k = 0; k < len / 2; ++k) {
                std::complex<float> u = x[i + k];
                std::complex<float> v = x[i + k + len / 2] * w;
                x[i + k] = u + v;
                x[i + k + len / 2] = u - v;
                w *= wlen;
            }
        }
    }
}

void AudioDsp::computeMagnitudeSpectrum(
    const float* pcmSamples,
    int sampleCount,
    float* outMagnitudes,
    int outputBands
) {
    if (sampleCount <= 0 || outputBands <= 0) return;

    // Nearest power of 2
    size_t N = 1;
    while (N < static_cast<size_t>(sampleCount) && N < 1024) {
        N <<= 1;
    }

    std::vector<std::complex<float>> buffer(N, {0.0f, 0.0f});
    for (size_t i = 0; i < N && i < static_cast<size_t>(sampleCount); ++i) {
        // Apply Hann window
        float hann = 0.5f * (1.0f - std::cos(2.0f * PI * static_cast<float>(i) / static_cast<float>(N - 1)));
        buffer[i] = std::complex<float>(pcmSamples[i] * hann, 0.0f);
    }

    computeFft(buffer);

    size_t halfN = N / 2;
    float step = static_cast<float>(halfN) / static_cast<float>(outputBands);

    for (int b = 0; b < outputBands; ++b) {
        size_t start = static_cast<size_t>(b * step);
        size_t end = std::min(static_cast<size_t>((b + 1) * step), halfN);
        float sum = 0.0f;
        size_t count = 0;
        for (size_t k = start; k < end; ++k) {
            sum += std::abs(buffer[k]);
            count++;
        }
        float avg = count > 0 ? (sum / static_cast<float>(count)) : 0.0f;
        outMagnitudes[b] = std::min(1.0f, avg / static_cast<float>(halfN));
    }
}

} // namespace novamotion
