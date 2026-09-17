package com.papi.nova.manager

import com.papi.nova.api.PolarisCapabilities
import com.papi.nova.api.PolarisSessionStatus
import com.papi.nova.nvstream.jni.MoonBridge
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionResilienceManagerTest {
    private val scheduler = FakeScheduler()
    private val listener = RecordingListener()
    private val statuses = ArrayDeque<PolarisSessionStatus?>()

    @After
    fun resetCapabilities() {
        FeatureFlagManager.reset()
    }

    @Test
    fun aStreamWithoutPolarisCapabilitiesEndsTheOrdinaryWay() {
        val manager = ConnectionResilienceManager(
            { statuses.removeFirst() },
            listener,
            FeatureFlagManager.beginScope(),
            0,
            scheduler,
        )

        assertFalse(manager.shouldAbsorbError(-1))
        assertTrue(scheduler.pending.isEmpty())
        assertTrue(listener.events.isEmpty())
    }

    @Test
    fun anAliveSessionReconnectsInsteadOfOnlyLogging() {
        // The absorbed error never reaches Game.connectionTerminated, so the alive branch has
        // to hand Game a reconnect. It used to log "Attempting reconnect..." and stop there,
        // leaving the stream frozen until the user ended the session by hand.
        val manager = managerWithCapabilities()
        statuses.add(PolarisSessionStatus(state = "paused"))

        assertTrue(manager.shouldAbsorbError(-1))
        scheduler.runNext()

        assertEquals(listOf("pending 1/4", "reconnect -1 1/4"), listener.events)
        assertEquals(listOf(0L), scheduler.delays)
    }

    @Test
    fun anEndedSessionTakesTheHostEndedTeardown() {
        val manager = managerWithCapabilities()
        statuses.add(PolarisSessionStatus(state = "idle"))

        assertTrue(manager.shouldAbsorbError(-100))
        scheduler.runNext()

        assertEquals(listOf("pending 1/4", "host ended"), listener.events)
    }

    @Test
    fun anUnreachableHostIsCheckedWithBackoffThenGivenBackToTheTerminatedPath() {
        // No answer is not the same as an ended session. It used to share the host-ended
        // branch, which sent the user to the library claiming the host had ended the session.
        val manager = managerWithCapabilities()
        repeat(4) { statuses.add(null) }

        assertTrue(manager.shouldAbsorbError(-1))
        repeat(4) { scheduler.runNext() }

        assertEquals(listOf(0L, 1000L, 3000L, 7000L), scheduler.delays)
        assertEquals(
            listOf("pending 1/4", "pending 2/4", "pending 3/4", "pending 4/4", "unreachable -1"),
            listener.events,
        )
        assertTrue(scheduler.pending.isEmpty())
    }

    @Test
    fun theHostAnsweringOnALaterCheckStillReconnects() {
        val manager = managerWithCapabilities()
        statuses.add(null)
        statuses.add(PolarisSessionStatus(state = "streaming"))

        assertTrue(manager.shouldAbsorbError(-1))
        scheduler.runNext()
        scheduler.runNext()

        assertEquals(listOf("pending 1/4", "pending 2/4", "reconnect -1 2/4"), listener.events)
    }

    @Test
    fun attemptsUsedBeforeARelaunchAreNotGrantedAgain() {
        // Each reconnect is a new Game. Without the carried count a host that accepts the launch
        // but never sends a first frame would be relaunched forever.
        val relaunched = managerWithCapabilities(startingAttempt = 3)
        statuses.add(PolarisSessionStatus(state = "streaming"))

        assertTrue(relaunched.shouldAbsorbError(-100))
        scheduler.runNext()
        assertEquals(listOf(7000L), scheduler.delays)
        assertEquals(listOf("pending 4/4", "reconnect -100 4/4"), listener.events)

        val spent = managerWithCapabilities(startingAttempt = 4)
        assertFalse(spent.shouldAbsorbError(-100))
    }

    @Test
    fun renderedFramesAfterAReconnectRestoreTheFullBudget() {
        val manager = managerWithCapabilities(startingAttempt = 4)
        manager.onReconnectSuccess()
        statuses.add(PolarisSessionStatus(state = "streaming"))

        assertTrue(manager.shouldAbsorbError(-1))
        scheduler.runNext()

        assertEquals(listOf(0L), scheduler.delays)
        assertEquals(listOf("pending 1/4", "reconnect -1 1/4"), listener.events)
    }

    @Test
    fun aShutDownManagerLetsTheErrorThroughWithoutShowingTheOverlay() {
        val manager = managerWithCapabilities()
        manager.shutdown()

        assertFalse(manager.shouldAbsorbError(-1))
        assertTrue("nothing would ever dismiss an overlay shown here", listener.events.isEmpty())
    }

    @Test
    fun aCheckCutShortByShutdownEndsTheStreamOnce() {
        // onDestroy and the host-ended teardown shut the manager down while a status query can be
        // in flight. The interrupted query reports no answer; that must end in one outcome, with
        // no further check scheduled and no second overlay.
        lateinit var manager: ConnectionResilienceManager
        manager = ConnectionResilienceManager(
            {
                manager.shutdown()
                null
            },
            listener,
            scopeWithCapabilities(),
            0,
            scheduler,
        )

        assertTrue(manager.shouldAbsorbError(-1))
        scheduler.runNext()

        assertEquals(listOf("pending 1/4", "unreachable -1"), listener.events)
        assertTrue(scheduler.pending.isEmpty())
    }

    @Test
    fun streamsTheHostEndedOnPurposeAreNotTakenBack() {
        // The host may have handed the session to another device or quit the app while its session
        // still reads alive. Relaunching would take the stream back.
        val manager = managerWithCapabilities()

        assertFalse(manager.shouldAbsorbError(MoonBridge.ML_ERROR_GRACEFUL_TERMINATION))
        assertFalse(manager.shouldAbsorbError(MoonBridge.ML_ERROR_PROTECTED_CONTENT))
        assertFalse(manager.shouldAbsorbError(MoonBridge.ML_ERROR_FRAME_CONVERSION))
        assertTrue(listener.events.isEmpty())
        assertTrue(scheduler.pending.isEmpty())
    }

    @Test
    fun anOutcomeThatThrowsFallsBackToTheTerminatedPath() {
        val throwingListener = object : ConnectionResilienceManager.Listener by listener {
            override fun onReconnect(errorCode: Int, attempt: Int, maxAttempts: Int) {
                throw IllegalStateException("activity gone")
            }
        }
        val manager = ConnectionResilienceManager(
            { PolarisSessionStatus(state = "streaming") },
            throwingListener,
            scopeWithCapabilities(),
            0,
            scheduler,
        )

        assertTrue(manager.shouldAbsorbError(-1))
        scheduler.runNext()

        assertEquals(listOf("pending 1/4", "unreachable -1"), listener.events)
    }

    @Test
    fun aThrowingStatusQueryCountsAsNoAnswer() {
        val manager = ConnectionResilienceManager(
            { throw IllegalStateException("socket closed") },
            listener,
            scopeWithCapabilities(),
            3,
            scheduler,
        )

        assertTrue(manager.shouldAbsorbError(-1))
        scheduler.runNext()

        assertEquals(listOf("pending 4/4", "unreachable -1"), listener.events)
    }

    private fun managerWithCapabilities(startingAttempt: Int = 0) = ConnectionResilienceManager(
        { statuses.removeFirst() },
        listener,
        scopeWithCapabilities(),
        startingAttempt,
        scheduler,
    )

    private fun scopeWithCapabilities(): Long {
        val scope = FeatureFlagManager.beginScope()
        assertTrue(
            FeatureFlagManager.publishForScope(
                scope,
                PolarisCapabilities(
                    server = "polaris",
                    version = "1.4.9",
                    features = PolarisCapabilities.Features(),
                    capture = PolarisCapabilities.CaptureInfo(),
                ),
            ),
        )
        return scope
    }

    private class FakeScheduler : ConnectionResilienceManager.Scheduler {
        val pending = ArrayDeque<Runnable>()
        val delays = mutableListOf<Long>()
        private var shutDown = false

        override fun schedule(delayMs: Long, task: Runnable): Boolean {
            if (shutDown) return false
            delays.add(delayMs)
            pending.add(task)
            return true
        }

        override fun shutdown() {
            // Matches shutdownNow(): queued checks are discarded, a running one finishes.
            shutDown = true
            pending.clear()
        }

        fun runNext() {
            pending.removeFirst().run()
        }
    }

    private class RecordingListener : ConnectionResilienceManager.Listener {
        val events = mutableListOf<String>()

        override fun onReconnectPending(attempt: Int, maxAttempts: Int) {
            events.add("pending $attempt/$maxAttempts")
        }

        override fun onReconnect(errorCode: Int, attempt: Int, maxAttempts: Int) {
            events.add("reconnect $errorCode $attempt/$maxAttempts")
        }

        override fun onHostSessionEnded() {
            events.add("host ended")
        }

        override fun onHostUnreachable(errorCode: Int) {
            events.add("unreachable $errorCode")
        }
    }
}
