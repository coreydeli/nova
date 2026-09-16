package com.papi.nova.binding.audio

import org.junit.Assert.*
import org.junit.Test

class AudioPlaybackStatsTest {
    @Test fun separatesDecoderIdleFromHapticsAndBlockingWrite() {
        val stats = AudioPlaybackStats()
        stats.record(0, 2_000_000, 3_000_000, 28_000_000, 5, 480, false, 480)
        stats.record(35_000_000, 36_000_000, 37_000_000, 42_000_000, 35, 480, false, 480)
        val result = stats.snapshot(42_000_000)!!
        assertEquals(7_000L, result.maxIdleUs)
        assertEquals(2_000L, result.maxHapticUs)
        assertEquals(25_000L, result.maxWriteUs)
        assertEquals(1L, result.slowWrites)
        assertEquals(35, result.maxPendingMs)
        assertEquals(960L, result.writtenSamples)
    }

    @Test fun distinguishesQueueDropsFromShortWritesAndErrors() {
        val stats = AudioPlaybackStats()
        stats.record(0, 0, 0, 1, 40, 480, true, 0)
        stats.record(2, 2, 2, 3, 35, 480, false, 240)
        stats.record(4, 4, 4, 5, 30, 480, false, -6)
        stats.record(6, 6, 6, 7, 25, 480, false, 0)
        val result = stats.snapshot(7)!!
        assertEquals(4L, result.callbacks)
        assertEquals(1L, result.skippedPackets)
        assertEquals(240L, result.writtenSamples)
        assertEquals(2L, result.shortWrites)
        assertEquals(1L, result.writeErrors)
        assertEquals(-6, result.lastWriteError)
    }

    @Test fun reportsOnlyActiveWindowsAfterTenSeconds() {
        val stats = AudioPlaybackStats()
        assertFalse(stats.reportDue(20_000_000_000))
        assertNull(stats.snapshot(20_000_000_000))
        stats.record(-100, -100, -100, -90, 0, 480, false, 480)
        assertFalse(stats.reportDue(9_999_999_899))
        assertTrue(stats.reportDue(9_999_999_900))
        assertEquals(10_000L, stats.snapshot(9_999_999_900)!!.durationMs)
        assertFalse(stats.reportDue(30_000_000_000))
    }

    @Test fun resetsWindowCountersButRetainsIdleAcrossReportBoundary() {
        val stats = AudioPlaybackStats()
        stats.record(0, 1, 1, 1_000_000, 50, 480, true, 0)
        stats.snapshot(1_000_000)
        stats.record(31_000_000, 31_000_000, 31_000_000, 32_000_000, 5, 480, false, 480)
        val result = stats.snapshot(32_000_000)!!
        assertEquals(1L, result.callbacks)
        assertEquals(0L, result.skippedPackets)
        assertEquals(5, result.maxPendingMs)
        assertEquals(30_000L, result.maxIdleUs)
        assertEquals(0L, result.maxHapticUs)
        assertEquals(1_000L, result.maxWriteUs)
        assertEquals(1L, result.durationMs)
    }

    @Test fun twentyMillisecondWriteIsNotCountedAsOverTwenty() {
        val stats = AudioPlaybackStats()
        stats.record(0, 0, 0, 20_000_000, 0, 480, false, 480)
        assertEquals(0L, stats.snapshot(20_000_000)!!.slowWrites)
    }
}
