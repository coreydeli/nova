package com.papi.nova.manager

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReconnectRecoveryTrackerTest {
    @Test
    fun aStreamThatRendersBrieflyAndDropsDoesNotEarnTheBudgetBack() {
        // Resetting on the first frame let a stream that renders and then dies relaunch forever.
        val tracker = ReconnectRecoveryTracker(stableMs = 15_000)

        assertFalse(tracker.onSample(framesRendered = 120, nowMs = 1_000))
        assertFalse(tracker.onSample(framesRendered = 240, nowMs = 2_000))
        assertFalse(tracker.onSample(framesRendered = 360, nowMs = 15_999))
    }

    @Test
    fun renderingThatHoldsForTheStableWindowRecoversExactlyOnce() {
        val tracker = ReconnectRecoveryTracker(stableMs = 15_000)

        assertFalse(tracker.onSample(framesRendered = 120, nowMs = 1_000))
        assertTrue(tracker.onSample(framesRendered = 1_900, nowMs = 16_000))
        assertFalse(tracker.onSample(framesRendered = 2_020, nowMs = 17_000))
    }

    @Test
    fun aSampleWithNoNewFramesStartsTheWaitOver() {
        val tracker = ReconnectRecoveryTracker(stableMs = 15_000)

        assertFalse(tracker.onSample(framesRendered = 120, nowMs = 1_000))
        assertFalse(tracker.onSample(framesRendered = 120, nowMs = 10_000))
        assertFalse(tracker.onSample(framesRendered = 240, nowMs = 11_000))
        assertFalse(tracker.onSample(framesRendered = 1_900, nowMs = 25_999))
        assertTrue(tracker.onSample(framesRendered = 2_020, nowMs = 26_000))
    }

    @Test
    fun noFramesNeverRecovers() {
        val tracker = ReconnectRecoveryTracker(stableMs = 15_000)

        assertFalse(tracker.onSample(framesRendered = 0, nowMs = 1_000))
        assertFalse(tracker.onSample(framesRendered = 0, nowMs = 60_000))
    }
}
