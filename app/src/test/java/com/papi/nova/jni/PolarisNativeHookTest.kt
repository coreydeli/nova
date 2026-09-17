package com.papi.nova.jni

import com.papi.nova.api.PolarisCapabilities
import com.papi.nova.api.PolarisSessionStatus
import com.papi.nova.manager.ConnectionResilienceManager
import com.papi.nova.manager.FeatureFlagManager
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PolarisNativeHookTest {
    @After
    fun reset() {
        PolarisNativeHook.unregister(current)
        FeatureFlagManager.reset()
    }

    private var current: ConnectionResilienceManager? = null

    @Test
    fun theGameBeingReplacedCannotRemoveTheNewGamesHook() {
        // An automatic reconnect starts the next Game before the old one is destroyed. If the old
        // Game's onDestroy cleared the hook, the new stream's drops would skip recovery entirely.
        val scope = scopeWithCapabilities()
        val old = manager(scope)
        val replacement = manager(scope)
        current = replacement

        PolarisNativeHook.register(old)
        PolarisNativeHook.register(replacement)
        PolarisNativeHook.unregister(old)

        assertTrue(PolarisNativeHook.onPreTermination(-1))
    }

    @Test
    fun theOwnerRemovesItsHook() {
        val manager = manager(scopeWithCapabilities())
        current = manager

        PolarisNativeHook.register(manager)
        PolarisNativeHook.unregister(manager)

        assertFalse(PolarisNativeHook.onPreTermination(-1))
    }

    private fun manager(scope: Long) = ConnectionResilienceManager(
        { PolarisSessionStatus(state = "streaming") },
        NoOpListener,
        scope,
        0,
        QueueingScheduler(),
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

    private object NoOpListener : ConnectionResilienceManager.Listener {
        override fun onReconnectPending(attempt: Int, maxAttempts: Int) = Unit

        override fun onReconnect(errorCode: Int, attempt: Int, maxAttempts: Int) = Unit

        override fun onHostSessionEnded() = Unit

        override fun onHostUnreachable(errorCode: Int) = Unit
    }

    private class QueueingScheduler : ConnectionResilienceManager.Scheduler {
        override fun schedule(delayMs: Long, task: Runnable): Boolean = true

        override fun shutdown() = Unit
    }
}
