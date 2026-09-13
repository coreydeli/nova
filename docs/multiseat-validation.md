# Multiseat streaming validation

A Nova Debug build enables native protocol checks by default. Those checks
include synthetic FEC packet drops and require an extra parity packet before
delivering video. A complete frame can therefore be rejected in this mode when
that extra packet is absent. Use normal native packet handling when measuring
stream delivery, decoder stalls, or latency.

Build an Android debug package with normal native packet handling:

~~~sh
./gradlew -PnovaAbis=arm64-v8a -PnovaNativeDebugChecks=false \
  :app:externalNativeBuildNonRoot_gameDebug \
  :app:assembleNonRoot_gameDebug
~~~

This keeps the debug application ID and signing configuration, so an existing
matching debug installation can be updated without clearing its data. It does
not make the Java and Kotlin portions of the package equivalent to a release
build. Use a qualified release or benchmark build for release performance claims.

Omit the property, or set `-PnovaNativeDebugChecks=true`, to retain native
assertions and synthetic FEC validation. The property changes a native build
argument, which separates native outputs for the two modes. Release native
builds do not enable these debug checks.

Include both networking and application tags when retaining a private smoke log:

~~~sh
adb logcat -v threadtime -T 1 \
  'moonlight-common-c:I' 'com.papi.nova.LimeLog:D' 'AndroidRuntime:E' '*:S'
~~~

Record the exact source revision, build command, APK hash, and packaged
`libmoonlight-core.so` hash. Verify the native library changed between modes.
A successful Gradle command alone does not prove that the intended native
library was rebuilt and packaged.

Keep raw logs, captures, pairing material, device identities, and profile data
out of public evidence. Public results should state the duration, negotiated
video and audio contract, client loss and decoder observations, and cleanup.
Worker frame counts and host packet capture alone do not establish client
delivery. A decoder watchdog message alone does not prove a hardware decoder
fault.

## Audio playback measurements

Nova reports a bounded audio playback summary every ten seconds and at stream
cleanup. The audio callback only updates counters; it does not allocate a
snapshot or write a log on every packet. The summary distinguishes:

- Native pending compressed audio and the existing queue skip decision.
- AudioTrack blocking write duration, short writes, and negative error results.
- Time outside the previous callback, including native work and scheduling.
- Controller audio haptics work before the AudioTrack write.
- Android's cumulative application write buffer underrun count and buffer size.

The pending audio value is not AudioTrack latency. Written samples are
interleaved PCM shorts, while buffer size is in audio frames. Unavailable
platform counters use -1. Report windows are interval summaries; the first and
last cumulative underrun counts inside a test are not exact test endpoints.
An underrun counter or queue skip alone does not establish audible quality or
identify where the upstream delay occurred.

The dedicated native audio playback thread requests Android's audio priority
on its first decoded callback. Setup runs on a different thread. The request
preserves an already higher priority and playback continues if a device rejects
it. The actual priority is logged once. Audio buffer size, queue threshold and
write behavior are unchanged.

For comparisons, retain the complete connection log and a separately timed
steady gameplay interval. Account for host and device clock offsets when
matching the interval to logcat. Keep startup stalls visible in the full
connection evidence rather than treating a quiet gameplay interval as proof
that every connection phase passed.

For a debug receive timing comparison, also pass
`-PnovaAudioReceiveDiagnostics=true`. This opt-in build observes only the
audio socket call in the unchanged vendored audio implementation. Normal
builds compile that implementation directly. The observer returns the original
socket result and preserves its error value.

The `Nova: audio receive` summary reports data and FEC packet counts, socket
timeouts and errors, the longest receive call, time outside receive calls,
and gaps between returned audio data packets. It logs once per ten second
window and records no addresses, packet contents or credentials. The first
wait for traffic is excluded. Receive time includes kernel and thread
scheduling as well as waiting for packets; these are application receive
observations, not radio arrival timestamps.

Record a new native library hash for this build and compare its windows with
playback and host capture timing. Disable the property again for the ordinary
playback build after the experiment.

On 2026-09-13, the five playback counter tests passed and ordinary and receive
observer APKs both built. After physical comparisons, an ordinary APK from
`15555ac1eab3ca72edb0bc1d1d7964ab604225cc` was installed with both native
diagnostic flags false. Its packaged native library matched the earlier
ordinary priority build. The
[Polaris audio timing report](https://github.com/papi-ux/polaris/blob/67b12eee/docs/research/container-multiseat-audio-timing.md)
retains the failed 120 FPS high bitrate observation alongside quiet diagnostic
repeats. Those repeats do not establish that the audio failure is fixed.

A separate library check with Polaris `4ee1468d` showed the assigned Steam
profile's name on the physical Android client. Nova continues to identify the
profile launch by its stable app UUID, independently of that display name.
