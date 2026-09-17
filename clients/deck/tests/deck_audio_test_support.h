#pragma once

#include "stream/deck_audio_output.h"
#include <Limelight.h>
#include <opus/opus_multistream.h>
#include <cmath>
#include <stdexcept>

namespace nova::deck::test {

inline OPUS_MULTISTREAM_CONFIGURATION stereoConfig() {
    OPUS_MULTISTREAM_CONFIGURATION config{};
    config.sampleRate = 48000;
    config.channelCount = 2;
    config.streams = 1;
    config.coupledStreams = 1;
    config.samplesPerFrame = 240;
    config.mapping[0] = 0;
    config.mapping[1] = 1;
    return config;
}

class RecordingPcmOutput final : public stream::DeckPcmOutput {
public:
    bool open(const stream::DeckAudioFormat& requested) override {
        format = requested;
        outputStats = {};
        outputStats.available = allowOpen;
        pcm.clear();
        ++openCalls;
        return allowOpen;
    }
    bool start() override { return started = allowStart && outputStats.available; }
    void stop() override { close(); }
    void close() override { started = false; outputStats.available = false; }
    bool write(std::span<const float> samples) override {
        if (!started || !outputStats.available) return false;
        if (rejectWrites) {
            outputStats.droppedFrames += samples.size() / format.channels;
            return false;
        }
        pcm.insert(pcm.end(), samples.begin(), samples.end());
        return true;
    }
    stream::DeckAudioOutputStats stats() const override { return outputStats; }

    stream::DeckAudioFormat format{};
    stream::DeckAudioOutputStats outputStats{};
    std::vector<float> pcm;
    int openCalls = 0;
    bool started = false;
    bool allowOpen = true;
    bool allowStart = true;
    bool rejectWrites = false;
};

inline std::vector<char> encodePacket(const OPUS_MULTISTREAM_CONFIGURATION& config, int frames) {
    int error = OPUS_OK;
    auto* encoder = opus_multistream_encoder_create(config.sampleRate, config.channelCount,
        config.streams, config.coupledStreams, config.mapping, OPUS_APPLICATION_AUDIO, &error);
    if (!encoder || error != OPUS_OK) throw std::runtime_error("Opus test encoder creation failed");
    std::vector<float> pcm(static_cast<std::size_t>(frames * config.channelCount));
    for (int frame = 0; frame < frames; ++frame) {
        for (int channel = 0; channel < config.channelCount; ++channel) {
            pcm[frame * config.channelCount + channel] =
                0.25f * std::sin(frame * (0.025f + channel * 0.017f));
        }
    }
    std::vector<char> packet(16384);
    const int length = opus_multistream_encode_float(encoder, pcm.data(), frames,
        reinterpret_cast<unsigned char*>(packet.data()), packet.size());
    opus_multistream_encoder_destroy(encoder);
    if (length <= 0) throw std::runtime_error("Opus test packet encoding failed");
    packet.resize(length);
    return packet;
}

} // namespace nova::deck::test
