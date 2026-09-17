package com.papi.nova.binding.input.capture

import android.app.Activity
import android.content.Context
import android.os.Looper
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@Config(sdk = [33])
@RunWith(RobolectricTestRunner::class)
class AndroidNativePointerCaptureProviderTest {
    @Test
    fun aMouseThatNothingHasCapturedYetCountsAsACaptureDevice() {
        // An attached USB or Bluetooth mouse reports plain SOURCE_MOUSE until a window holds
        // pointer capture. Requiring SOURCE_MOUSE_RELATIVE here meant capture was never
        // requested for it, which is how a mouse on a tablet went dead (nova#315).
        assertTrue(AndroidNativePointerCaptureProvider.isCaptureCompatible(InputDevice.SOURCE_MOUSE, false))
        assertTrue(
            AndroidNativePointerCaptureProvider.isCaptureCompatible(
                InputDevice.SOURCE_KEYBOARD or InputDevice.SOURCE_MOUSE,
                false,
            ),
        )
        assertTrue(AndroidNativePointerCaptureProvider.isCaptureCompatible(InputDevice.SOURCE_MOUSE_RELATIVE, false))
        assertTrue(AndroidNativePointerCaptureProvider.isCaptureCompatible(InputDevice.SOURCE_TOUCHPAD, false))
    }

    @Test
    fun touchscreensDoNotRequestCaptureExceptOnChromeOs() {
        val samsungTouchpad = InputDevice.SOURCE_TOUCHSCREEN or InputDevice.SOURCE_KEYBOARD or InputDevice.SOURCE_MOUSE
        val stylusScreen = InputDevice.SOURCE_TOUCHSCREEN or InputDevice.SOURCE_STYLUS

        assertFalse(AndroidNativePointerCaptureProvider.isCaptureCompatible(samsungTouchpad, false))
        assertTrue(AndroidNativePointerCaptureProvider.isCaptureCompatible(samsungTouchpad, true))
        assertFalse(AndroidNativePointerCaptureProvider.isCaptureCompatible(stylusScreen, false))
        assertFalse(AndroidNativePointerCaptureProvider.isCaptureCompatible(stylusScreen, true))
        assertFalse(AndroidNativePointerCaptureProvider.isCaptureCompatible(InputDevice.SOURCE_KEYBOARD, false))
        assertFalse(
            AndroidNativePointerCaptureProvider.isCaptureCompatible(
                InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_JOYSTICK,
                false,
            ),
        )
    }

    @Test
    fun grabbedInputPassesTheMouseGateWithoutPointerCapture() {
        // Game drops every mouse event while isCapturingActive() is false. Tying it to
        // hasPointerCapture() dropped the mouse whenever capture was missing, including on
        // DeX and ChromeOS, where capture can be unavailable but the pointer still works.
        val (provider, view) = provider(sources = mutableListOf())

        assertFalse(provider.isCapturingActive())

        provider.enableCapture()

        assertEquals(0, view.requests)
        assertTrue(provider.isCapturingActive())

        provider.disableCapture()

        assertFalse(provider.isCapturingActive())
    }

    @Test
    fun hidingTheCursorRequestsCaptureOnlyWithACaptureDevice() {
        val (withMouse, mouseView) = provider(sources = mutableListOf(InputDevice.SOURCE_MOUSE))
        withMouse.enableCapture()
        assertEquals(1, mouseView.requests)

        val (withoutMouse, keyboardView) = provider(sources = mutableListOf(InputDevice.SOURCE_KEYBOARD))
        withoutMouse.enableCapture()
        assertEquals(0, keyboardView.requests)
    }

    @Test
    fun showingTheCursorReleasesCapture() {
        val (provider, view) = provider(sources = mutableListOf(InputDevice.SOURCE_MOUSE))

        provider.enableCapture()
        provider.showCursor()

        assertEquals(1, view.releases)
    }

    @Test
    fun regainingFocusRecapturesAfterTheAndroidQDelay() {
        val (provider, view) = provider(sources = mutableListOf(InputDevice.SOURCE_MOUSE))
        provider.enableCapture()
        assertEquals(1, view.requests)

        provider.onWindowFocusChanged(true)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(499))
        assertEquals(1, view.requests)

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1))
        assertEquals(2, view.requests)
    }

    @Test
    fun regainingFocusLeavesAVisibleCursorAndAnUngrabbedStreamAlone() {
        val (ungrabbed, ungrabbedView) = provider(sources = mutableListOf(InputDevice.SOURCE_MOUSE))
        ungrabbed.onWindowFocusChanged(true)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
        assertEquals(0, ungrabbedView.requests)

        val (visibleCursor, visibleView) = provider(sources = mutableListOf(InputDevice.SOURCE_MOUSE))
        visibleCursor.enableCapture()
        visibleCursor.showCursor()
        visibleCursor.onWindowFocusChanged(true)
        visibleCursor.onWindowFocusChanged(false)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
        assertEquals(1, visibleView.requests)

        // Showing the cursor inside the delay must not be undone when the delay runs out.
        val (raced, racedView) = provider(sources = mutableListOf(InputDevice.SOURCE_MOUSE))
        raced.enableCapture()
        raced.onWindowFocusChanged(true)
        raced.showCursor()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
        assertEquals(1, racedView.requests)
    }

    @Test
    fun unpluggingTheLastMouseReleasesCaptureAndPluggingOneInRequestsIt() {
        val sources = mutableListOf(InputDevice.SOURCE_MOUSE)
        val (provider, view) = provider(sources = sources)
        provider.enableCapture()
        view.captured = true

        sources.clear()
        provider.onInputDeviceRemoved(7)
        assertEquals(1, view.releases)

        view.captured = false
        sources.add(InputDevice.SOURCE_MOUSE)
        provider.onInputDeviceAdded(8)
        assertEquals(2, view.requests)
    }

    @Test
    fun capturedMouseMovementCountsEverySampleAndroidBatched() {
        val (provider, _) = provider(sources = mutableListOf(InputDevice.SOURCE_MOUSE))
        val event = motionEvent(
            InputDevice.SOURCE_MOUSE_RELATIVE,
            MotionEvent.TOOL_TYPE_MOUSE,
            listOf(
                mapOf(MotionEvent.AXIS_X to 2f, MotionEvent.AXIS_Y to -1f),
                mapOf(MotionEvent.AXIS_X to 1f, MotionEvent.AXIS_Y to -2f),
                mapOf(MotionEvent.AXIS_X to 3f, MotionEvent.AXIS_Y to -4f),
            ),
        )

        assertTrue(provider.eventHasRelativeMouseAxes(event))
        assertEquals(6f, provider.getRelativeAxisX(event), 0.001f)
        assertEquals(-7f, provider.getRelativeAxisY(event), 0.001f)
        event.recycle()
    }

    @Test
    fun capturedTouchpadMovementComesFromTheRelativeAxes() {
        val (provider, view) = provider(sources = mutableListOf(InputDevice.SOURCE_TOUCHPAD))
        val event = motionEvent(
            InputDevice.SOURCE_TOUCHPAD,
            MotionEvent.TOOL_TYPE_FINGER,
            listOf(
                mapOf(
                    MotionEvent.AXIS_X to 500f,
                    MotionEvent.AXIS_Y to 300f,
                    MotionEvent.AXIS_RELATIVE_X to 4f,
                    MotionEvent.AXIS_RELATIVE_Y to 5f,
                ),
            ),
        )

        assertFalse(provider.eventHasRelativeMouseAxes(event))

        view.captured = true

        assertTrue(provider.eventHasRelativeMouseAxes(event))
        assertEquals(4f, provider.getRelativeAxisX(event), 0.001f)
        assertEquals(5f, provider.getRelativeAxisY(event), 0.001f)
        event.recycle()
    }

    @Test
    fun anUncapturedMouseHasNoRelativeAxes() {
        assertFalse(
            AndroidNativePointerCaptureProvider.hasRelativeMouseAxes(
                InputDevice.SOURCE_MOUSE,
                MotionEvent.TOOL_TYPE_MOUSE,
                false,
            ),
        )
        assertFalse(
            AndroidNativePointerCaptureProvider.hasRelativeMouseAxes(
                InputDevice.SOURCE_MOUSE_RELATIVE,
                MotionEvent.TOOL_TYPE_FINGER,
                true,
            ),
        )
    }

    private fun provider(sources: MutableList<Int>): Pair<AndroidNativePointerCaptureProvider, CaptureRecordingView> {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val view = CaptureRecordingView(activity)
        return AndroidNativePointerCaptureProvider(activity, view) { sources.toList() } to view
    }

    private fun motionEvent(source: Int, toolType: Int, samples: List<Map<Int, Float>>): MotionEvent {
        val properties = arrayOf(
            MotionEvent.PointerProperties().apply {
                id = 0
                this.toolType = toolType
            },
        )

        fun coords(values: Map<Int, Float>) = arrayOf(
            MotionEvent.PointerCoords().apply {
                values.forEach { (axis, value) -> setAxisValue(axis, value) }
            },
        )

        val event = MotionEvent.obtain(
            0L,
            0L,
            MotionEvent.ACTION_MOVE,
            1,
            properties,
            coords(samples.first()),
            0,
            0,
            1f,
            1f,
            1,
            0,
            source,
            0,
        )
        samples.drop(1).forEachIndexed { index, values ->
            event.addBatch((index + 1).toLong(), coords(values), 0)
        }
        return event
    }

    private class CaptureRecordingView(context: Context) : View(context) {
        var requests = 0
        var releases = 0
        var captured = false

        override fun requestPointerCapture() {
            requests++
        }

        override fun releasePointerCapture() {
            releases++
        }

        override fun hasPointerCapture(): Boolean = captured
    }
}
