#pragma once
#include <Limelight.h>

// The worker contract requires five millisecond Opus packets at every bitrate.
// Copy the callbacks per connection so the next ordinary stream retains its
// normal low-bandwidth and slow-decoder negotiation.
static inline AUDIO_RENDERER_CALLBACKS workerAudioCallbacks(
        const AUDIO_RENDERER_CALLBACKS* original, bool workerProfile) {
    AUDIO_RENDERER_CALLBACKS selected = *original;
    if (workerProfile) {
        selected.capabilities &= ~(CAPABILITY_SUPPORTS_ARBITRARY_AUDIO_DURATION |
                                   CAPABILITY_SLOW_OPUS_DECODER);
    }
    return selected;
}
