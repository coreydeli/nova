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
