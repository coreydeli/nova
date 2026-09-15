package com.papi.nova.ui

import android.os.Looper
import androidx.compose.runtime.MutableState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import com.papi.nova.api.PolarisApiClient
import com.papi.nova.api.PolarisSpace
import com.papi.nova.api.PolarisSpaces
import com.papi.nova.manager.WorkerLaunchContract
import com.papi.nova.shared.polaris.model.PolarisGame
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Job
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@Config(sdk = [33])
@LooperMode(LooperMode.Mode.PAUSED)
@RunWith(RobolectricTestRunner::class)
class NovaSpaceOpenLifecycleTest {
    private val game = PolarisGame(id = WorkerLaunchContract.APP_UUID, name = "Living Room")
    private val initial = snapshot("Living Room")

    @Test
    fun leavingLibraryDiscardsAnOutstandingSpaceCheck() {
        val api = mock(PolarisApiClient::class.java)
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        `when`(api.getSpaces()).thenAnswer {
            entered.countDown()
            check(release.await(5, TimeUnit.SECONDS))
            snapshot("Late Response")
        }
        val activity = activity(api)
        try {
            open(activity)
            await { entered.count == 0L }
            val request = request(activity)
            pause(activity)
            release.countDown()
            await { request.isCompleted }

            assertSame(initial, state<PolarisSpaces>(activity, "spacesSnapshot").value)
            verify(api, never()).getSessionStatus()
        } finally {
            release.countDown()
            destroy(activity)
        }
    }

    @Test
    fun oldCompletionCannotClearANewerOpenActionAfterReturning() {
        val api = mock(PolarisApiClient::class.java)
        val firstEntered = CountDownLatch(1)
        val secondEntered = CountDownLatch(1)
        val firstRelease = CountDownLatch(1)
        val secondRelease = CountDownLatch(1)
        `when`(api.getSpaces()).thenAnswer {
            firstEntered.countDown()
            check(firstRelease.await(5, TimeUnit.SECONDS))
            initial
        }.thenAnswer {
            secondEntered.countDown()
            check(secondRelease.await(5, TimeUnit.SECONDS))
            initial
        }
        val activity = activity(api)
        try {
            open(activity)
            await { firstEntered.count == 0L }
            val first = request(activity)
            pause(activity)
            // Return before the old blocking HTTP call has finished.
            (activity.lifecycle as LifecycleRegistry).currentState = Lifecycle.State.RESUMED
            state<Boolean>(activity, "spaceOpenPending").value = false
            open(activity)
            await { secondEntered.count == 0L }
            firstRelease.countDown()
            await { first.isCompleted }

            assertTrue(state<Boolean>(activity, "spaceOpenPending").value)
            verify(api, never()).getSessionStatus()
        } finally {
            pause(activity)
            firstRelease.countDown()
            secondRelease.countDown()
            destroy(activity)
        }
    }

    @Test
    fun openingTheChooserDiscardsThePreviousOpenCheck() {
        val api = mock(PolarisApiClient::class.java)
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        `when`(api.getSpaces()).thenAnswer {
            entered.countDown()
            check(release.await(5, TimeUnit.SECONDS))
            snapshot("Late Response")
        }
        val activity = activity(api)
        try {
            open(activity)
            await { entered.count == 0L }
            val request = request(activity)
            NovaLibraryActivity::class.java.getDeclaredMethod("showSpaceChooser").apply {
                isAccessible = true
                invoke(activity)
            }
            release.countDown()
            await { request.isCompleted }

            assertTrue(state<Boolean>(activity, "chooseSpaceVisible").value)
            assertSame(initial, state<PolarisSpaces>(activity, "spacesSnapshot").value)
            verify(api, never()).getSessionStatus()
        } finally {
            release.countDown()
            destroy(activity)
        }
    }

    @Test
    fun aRetiredRequestFailureCannotDisableCurrentSpaceActions() {
        val api = mock(PolarisApiClient::class.java)
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        `when`(api.getSpaces()).thenAnswer {
            entered.countDown()
            check(release.await(5, TimeUnit.SECONDS))
            throw java.io.IOException("Delayed network failure")
        }
        val activity = activity(api)
        try {
            open(activity)
            await { entered.count == 0L }
            val request = request(activity)
            pause(activity)
            release.countDown()
            await { request.isCompleted }

            assertTrue(state<Boolean>(activity, "spacesChecked").value)
            assertNull(state<String?>(activity, "spacesError").value)
            verify(api, never()).getSessionStatus()
        } finally {
            release.countDown()
            destroy(activity)
        }
    }

    private fun activity(api: PolarisApiClient): NovaLibraryActivity {
        // A hostless create initializes Android saved state and exits before library
        // loading or Compose rendering. Supply the request state directly to test
        // the real open and pause handlers without unrelated network activity.
        // These tests assert request isolation, not Activity navigation.
        val activity = Robolectric.buildActivity(NovaLibraryActivity::class.java).create().get()
        NovaLibraryActivity::class.java.getDeclaredField("apiClient").apply {
            isAccessible = true
            set(activity, api)
        }
        (activity.lifecycle as LifecycleRegistry).currentState = Lifecycle.State.RESUMED
        state<PolarisSpaces>(activity, "spacesSnapshot").value = initial
        state<Boolean>(activity, "spacesChecked").value = true
        return activity
    }

    private fun open(activity: NovaLibraryActivity) {
        NovaLibraryActivity::class.java.getDeclaredMethod("openSpace", PolarisGame::class.java).apply {
            isAccessible = true
            invoke(activity, game)
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

    private fun request(activity: NovaLibraryActivity): Job =
        activity.lifecycleScope.coroutineContext[Job]!!.children.single()

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
        assertTrue("Timed out waiting for the controlled request", condition())
    }

    private fun snapshot(name: String) =
        PolarisSpaces(true, true, true, "living-room",
            listOf(PolarisSpace("living-room", name, "ready", true)))
}
