#include "worker-audio.h"
#include <assert.h>

static void started(void) {}
int main(void) {
    const int ordinary = CAPABILITY_SUPPORTS_ARBITRARY_AUDIO_DURATION |
                         CAPABILITY_SLOW_OPUS_DECODER | CAPABILITY_DIRECT_SUBMIT;
    const AUDIO_RENDERER_CALLBACKS original = {.start = started, .capabilities = ordinary};
    AUDIO_RENDERER_CALLBACKS selected = workerAudioCallbacks(&original, false);
    assert(selected.capabilities == ordinary && selected.start == started);
    selected = workerAudioCallbacks(&original, true);
    assert(selected.capabilities == CAPABILITY_DIRECT_SUBMIT && selected.start == started);
    // A worker launch must not alter the callbacks reused by the next stream.
    assert(original.capabilities == ordinary);
    selected = workerAudioCallbacks(&original, false);
    assert(selected.capabilities == ordinary && selected.start == started);
    return 0;
}
