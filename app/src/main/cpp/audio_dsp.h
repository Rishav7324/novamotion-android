#ifndef NOVAMOTION_AUDIO_DSP_H
#define NOVAMOTION_AUDIO_DSP_H

#include <vector>
#include <complex>

namespace novamotion {

class AudioDsp {
public:
    /**
     * In-place Radix-2 Cooley-Tukey Fast Fourier Transform.
     * N must be a power of 2.
     */
    static void computeFft(std::vector<std::complex<float>>& x);

    /**
     * Computes normalized magnitude spectrum [0..1] from raw PCM samples.
     */
    static void computeMagnitudeSpectrum(
        const float* pcmSamples,
        int sampleCount,
        float* outMagnitudes,
        int outputBands
    );
};

} // namespace novamotion

#endif // NOVAMOTION_AUDIO_DSP_H
