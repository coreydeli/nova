# Native Deck audio backend

The native connection's audio callback decodes GameStream Opus multistream
packets through libopus and queues interleaved float PCM for PipeWire playback.
The graphical Play flow still uses the existing handoff. This backend is one
part of the supported Deck client, not release acceptance.

The decoder accepts validated 48 kHz configurations with up to eight channels.
It preserves the negotiated Opus mapping and GameStream speaker mask. Packet
decoding has a preallocated 120 ms maximum output buffer. The single-producer,
single-consumer output queue holds at most 120 ms and rejects an entire packet
on overflow. PipeWire's realtime callback only consumes PCM, zero-fills missing
samples and updates atomic counters; it does not decode, allocate or take the
decoder's lifecycle mutex. The requested graph latency is 5 ms; this is a
request, not a measured end-to-end latency claim.

Initialization fails when the default PipeWire server is unavailable. Stop
destroys the stream and joins its loop before buffers can be reused. A new
initialization clears media counters and queued PCM. Missing output, start
failure, corrupt Opus and output disconnection have fixed diagnostic text.
The backend does not fall back to PulseAudio or automatically reconnect output.

Native CLI progress and final receipts distinguish:

- `decodedFrames`: successfully decoded frames per channel.
- `queuedFrames`: decoded frames accepted by the bounded queue.
- `submittedFrames`: PCM frames copied into PipeWire buffers, not proof of
  audible speaker output.
- `silenceFrames`: zero-filled frames supplied because the queue was empty.
- `droppedFrames`: frames rejected because the queue was full.
- `decodeErrors`: invalid Opus packets rejected without queueing PCM.

Build dependencies include the libopus and PipeWire development packages
(`opus-devel pipewire-devel` on Fedora; `opus pipewire` on Arch). The Flatpak
manifest exposes the default `xdg-run/pipewire-0` socket for the native path.

Run the normal Deck CTest suite, or the focused tests:

```sh
ctest --test-dir build/deck --output-on-failure -R 'opus_audio|audio_unavailable|pipewire_'
```

The decoder tests encode real stereo, 5.1 and 7.1 packets at 2.5, 5, 10, 20,
40 and 60 ms, compare every decoded sample against a reference decoder, and
exercise invalid configurations, corrupt packets, queue overflow, concurrent
producer/consumer ordering, stop and repeated initialization. When `pipewire`
and `pw-link` are installed, CTest also starts a private daemon with a null sink
and checks real PCM submission plus server disconnection. Those tests never
start a game host or connect to the user's audio server.

Remaining acceptance includes the installed Flatpak on Deck speakers and
headphones, A/V synchronization, latency and underruns under game load, output
switching and suspend/resume. GUI session recovery must surface output failure
and offer a fresh session rather than treating the packet count as playback.
