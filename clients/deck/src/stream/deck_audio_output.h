#pragma once

#include <atomic>
#include <cstddef>
#include <cstdint>
#include <memory>
#include <span>
#include <vector>

namespace nova::deck::stream {

// Interleaved float PCM in the channel order of GameStream's speaker mask.
struct DeckAudioFormat {
    int sampleRate = 0;
    int channels = 0;
    unsigned channelMask = 0;
};

struct DeckAudioOutputStats {
    std::uint64_t submittedFrames = 0;
    std::uint64_t silenceFrames = 0;
    std::uint64_t droppedFrames = 0;
    bool available = false;
};

// One producer (the Opus callback), one consumer (PipeWire's realtime thread).
// Configure/reset only when both are stopped. Neither push nor pop allocates,
// blocks or overwrites data still being read. Overflow rejects the entire write.
class DeckPcmRingBuffer {
public:
    void configure(std::size_t capacitySamples);
    bool push(std::span<const float> samples);
    std::size_t pop(std::span<float> destination);
    std::size_t capacity() const;

private:
    std::vector<float> samples_;
    alignas(64) std::atomic<std::uint64_t> writeIndex_{0};
    alignas(64) std::atomic<std::uint64_t> readIndex_{0};
};

// Tests inject a recorder. Production owns the PipeWire implementation; no
// display, host connection, or QML object is needed to decode and play audio.
class DeckPcmOutput {
public:
    virtual ~DeckPcmOutput() = default;
    virtual bool open(const DeckAudioFormat& format) = 0;
    virtual bool start() = 0;
    virtual void stop() = 0;
    virtual void close() = 0;
    virtual bool write(std::span<const float> samples) = 0;
    virtual DeckAudioOutputStats stats() const = 0;
};

std::unique_ptr<DeckPcmOutput> makeDeckPipeWireOutput();

} // namespace nova::deck::stream
