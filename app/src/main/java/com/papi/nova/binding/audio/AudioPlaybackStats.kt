package com.papi.nova.binding.audio

/**
 * Playback-thread counters. Recording a packet allocates nothing and does no logging.
 *
 * Native queue depth is compressed audio waiting for the decoder, not AudioTrack latency.
 * Callback idle time excludes the previous callback's haptics and blocking write.
 */
internal class AudioPlaybackStats {
    private var windowStartNs = 0L
    private var previousEndNs = 0L
    private var hasPreviousCallback = false
    private var callbacks = 0L
    private var skippedPackets = 0L
    private var writtenSamples = 0L
    private var shortWrites = 0L
    private var writeErrors = 0L
    private var lastWriteError = 0
    private var slowWrites = 0L
    private var maxPendingMs = 0
    private var maxIdleNs = 0L
    private var maxHapticNs = 0L
    private var maxWriteNs = 0L

    fun record(
        startNs: Long,
        hapticsEndNs: Long,
        writeStartNs: Long,
        endNs: Long,
        pendingMs: Int,
        requestedSamples: Int,
        skipped: Boolean,
        writeResult: Int
    ) {
        if (callbacks == 0L) windowStartNs = startNs
        if (hasPreviousCallback) maxIdleNs = maxOf(maxIdleNs, startNs - previousEndNs)
        previousEndNs = endNs
        hasPreviousCallback = true
        callbacks++
        maxPendingMs = maxOf(maxPendingMs, pendingMs)
        maxHapticNs = maxOf(maxHapticNs, hapticsEndNs - startNs)
        if (skipped) {
            skippedPackets++
        } else {
            val writeNs = endNs - writeStartNs
            maxWriteNs = maxOf(maxWriteNs, writeNs)
            if (writeNs > SLOW_WRITE_NS) slowWrites++
            if (writeResult < 0) {
                writeErrors++
                lastWriteError = writeResult
            } else {
                writtenSamples += writeResult
                if (writeResult < requestedSamples) shortWrites++
            }
        }
    }

    fun reportDue(nowNs: Long): Boolean =
        callbacks > 0 && nowNs - windowStartNs >= REPORT_INTERVAL_NS

    fun snapshot(nowNs: Long): Snapshot? {
        if (callbacks == 0L) return null
        val result = Snapshot(
            (nowNs - windowStartNs) / 1_000_000, callbacks, skippedPackets,
            writtenSamples, shortWrites, writeErrors, lastWriteError, slowWrites,
            maxPendingMs, maxIdleNs / 1_000, maxHapticNs / 1_000, maxWriteNs / 1_000
        )
        callbacks = 0
        skippedPackets = 0
        writtenSamples = 0
        shortWrites = 0
        writeErrors = 0
        lastWriteError = 0
        slowWrites = 0
        maxPendingMs = 0
        maxIdleNs = 0
        maxHapticNs = 0
        maxWriteNs = 0
        // Keep previousEndNs so a stall crossing a report boundary is still visible.
        return result
    }

    data class Snapshot(
        val durationMs: Long,
        val callbacks: Long,
        val skippedPackets: Long,
        val writtenSamples: Long,
        val shortWrites: Long,
        val writeErrors: Long,
        val lastWriteError: Int,
        val slowWrites: Long,
        val maxPendingMs: Int,
        val maxIdleUs: Long,
        val maxHapticUs: Long,
        val maxWriteUs: Long
    )

    companion object {
        private const val REPORT_INTERVAL_NS = 10_000_000_000L
        private const val SLOW_WRITE_NS = 20_000_000L
    }
}
