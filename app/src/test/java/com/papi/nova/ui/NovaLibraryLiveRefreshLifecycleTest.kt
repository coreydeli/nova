package com.papi.nova.ui

import android.os.Looper
import androidx.compose.runtime.MutableState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import com.papi.nova.api.PolarisApiClient
import com.papi.nova.api.PolarisClientSettings
import com.papi.nova.shared.polaris.model.PolarisGame
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@Config(sdk = [33])
@LooperMode(LooperMode.Mode.PAUSED)
@RunWith(RobolectricTestRunner::class)
class NovaLibraryLiveRefreshLifecycleTest {
    private fun control(revision: String) = PolarisGame(
        id = "992FF124-4652-5708-501D-EDDBBB80EA8E",
        name = "Control Ultimate Edition",
        artwork = PolarisGame.ArtworkManifest(revision = revision),
    )

    @Test
    fun aCoverChangedOnTheHostReachesTheOpenLibraryAndLeavingStopsTheReads() {
        val reads = AtomicInteger()
        val api = mock(PolarisApiClient::class.java)
        `when`(api.getAllGames()).thenAnswer {
            reads.incrementAndGet()
            listOf(control("after-console-save"))
        }
        val activity = activity(api)
        try {
            games(activity).value = listOf(control("before"))
            start(activity)

            // Back on screen: one read at once, and the changed artwork is shown.
            await { games(activity).value.single().artwork?.revision == "after-console-save" }
            assertEquals(1, reads.get())

            // Thirty seconds later it reads again; the same library changes nothing.
            val shown = games(activity).value
            advance(Duration.ofSeconds(31))
            await { reads.get() == 2 }
            assertTrue(shown === games(activity).value)

            // Leaving the library stops the reads.
            pause(activity)
            advance(Duration.ofMinutes(3))
            Thread.sleep(50)
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(2, reads.get())
        } finally {
            destroy(activity)
        }
    }

    @Test
    fun aReadAfterAFailedLoadClearsTheFailureAndBringsTheSettings() {
        val api = mock(PolarisApiClient::class.java)
        val settings = PolarisClientSettings(revision = "host-settings")
        `when`(api.getAllGames()).thenReturn(listOf(control("recovered")))
        `when`(api.getClientSettings()).thenReturn(settings)
        val activity = activity(api)
        try {
            // The first load failed on flaky Wi-Fi: no games, an error and no settings.
            state<String?>(activity, "loadErrorMessage").value = "timeout"
            state<PolarisClientSettings?>(activity, "clientSettings").value = null
            start(activity)

            await { games(activity).value.singleOrNull()?.artwork?.revision == "recovered" }
            assertNull(state<String?>(activity, "loadErrorMessage").value)
            assertTrue(state<PolarisClientSettings?>(activity, "clientSettings").value === settings)
        } finally {
            destroy(activity)
        }
    }

    @Test
    fun aRefreshThePersonStartedIsNeverOvertaken() {
        val reads = AtomicInteger()
        val api = mock(PolarisApiClient::class.java)
        `when`(api.getAllGames()).thenAnswer {
            reads.incrementAndGet()
            listOf(control("background"))
        }
        val activity = activity(api)
        try {
            games(activity).value = listOf(control("before"))
            state<Boolean>(activity, "isRefreshing").value = true
            start(activity)
            advance(Duration.ofSeconds(31))
            Thread.sleep(50)
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(0, reads.get())
            assertEquals("before", games(activity).value.single().artwork?.revision)
        } finally {
            destroy(activity)
        }
    }

    private fun activity(api: PolarisApiClient): NovaLibraryActivity {
        // A hostless create exits before loading; supply the client, the artwork view model and
        // a finished first load so only the background reads touch the host.
        val activity = Robolectric.buildActivity(NovaLibraryActivity::class.java).create().get()
        NovaLibraryActivity::class.java.getDeclaredField("apiClient").apply {
            isAccessible = true
            set(activity, api)
        }
        NovaLibraryActivity::class.java.getDeclaredField("artworkLibraryUpdateViewModel").apply {
            isAccessible = true
            set(activity, NovaArtworkLibraryUpdateViewModel(api))
        }
        (activity.lifecycle as LifecycleRegistry).currentState = Lifecycle.State.RESUMED
        state<Boolean>(activity, "isInitialLoading").value = false
        return activity
    }

    private fun start(activity: NovaLibraryActivity) {
        NovaLibraryActivity::class.java.getDeclaredMethod("startLibraryPolling").apply {
            isAccessible = true
            invoke(activity)
        }
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun pause(activity: NovaLibraryActivity) {
        (activity.lifecycle as LifecycleRegistry).currentState = Lifecycle.State.STARTED
        NovaLibraryActivity::class.java.getDeclaredMethod("onPause").apply {
            isAccessible = true
            invoke(activity)
        }
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun destroy(activity: NovaLibraryActivity) {
        (activity.lifecycle as LifecycleRegistry).currentState = Lifecycle.State.DESTROYED
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun advance(duration: Duration) {
        shadowOf(Looper.getMainLooper()).idleFor(duration)
    }

    private fun games(activity: NovaLibraryActivity): MutableState<List<PolarisGame>> = state(activity, "allGames")

    @Suppress("UNCHECKED_CAST")
    private fun <T> state(activity: NovaLibraryActivity, name: String): MutableState<T> =
        NovaLibraryActivity::class.java.getDeclaredField(name + "\$delegate").run {
            isAccessible = true
            get(activity) as MutableState<T>
        }

    private fun await(condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(4)
        while (!condition() && System.nanoTime() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue("Timed out waiting for the background read", condition())
    }
}
