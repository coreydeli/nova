package com.papi.nova.binding.input.capture

import android.annotation.TargetApi
import android.app.Activity
import android.hardware.input.InputManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View

// We extend AndroidPointerIconCaptureProvider because we want to also get the
// pointer icon hiding behavior over our stream view just in case pointer capture
// is unavailable on this system (ex: DeX, ChromeOS)
@TargetApi(Build.VERSION_CODES.O)
class AndroidNativePointerCaptureProvider internal constructor(
    activity: Activity,
    private val targetView: View,
    private val connectedDeviceSources: () -> List<Int>,
) : AndroidPointerIconCaptureProvider(activity, targetView), InputManager.InputDeviceListener {
    constructor(activity: Activity, targetView: View) :
        this(activity, targetView, { connectedInputDeviceSources() })

    private val inputManager: InputManager = activity.getSystemService(InputManager::class.java)
    private val recaptureHandler = Handler(Looper.getMainLooper())
    private val recaptureAfterFocus = Runnable {
        if (isCapturing && !isCursorVisible && hasCaptureCompatibleInputDevice()) {
            targetView.requestPointerCapture()
        }
    }

    // We only capture the pointer if we have a compatible InputDevice
    // present. This is a workaround for an Android 12 regression causing
    // incorrect mouse input when using the SPen.
    // https://github.com/moonlight-stream/moonlight-android/issues/1030
    private fun hasCaptureCompatibleInputDevice(): Boolean {
        val isChromeOs = targetView.context.packageManager.hasSystemFeature(CHROME_OS_FEATURE)
        return connectedDeviceSources().any { sources -> isCaptureCompatible(sources, isChromeOs) }
    }

    override fun destroy() {
        recaptureHandler.removeCallbacks(recaptureAfterFocus)
        disableCapture()
    }

    override fun showCursor() {
        super.showCursor()

        // It is important to unregister the listener *before* releasing pointer capture,
        // because releasing pointer capture can cause an onInputDeviceChanged() callback
        // for devices with a touchpad (like a DS4 controller).
        inputManager.unregisterInputDeviceListener(this)
        targetView.releasePointerCapture()
    }

    override fun hideCursor() {
        super.hideCursor()

        // Listen for device events to enable/disable capture
        inputManager.registerInputDeviceListener(this, null)

        // Capture now if we have a capture-capable device
        if (hasCaptureCompatibleInputDevice()) {
            targetView.requestPointerCapture()
        }
    }

    override fun onWindowFocusChanged(focusActive: Boolean) {
        // NB: We have to check cursor visibility here because Android pointer capture
        // doesn't support capturing the cursor while it's visible. Enabling pointer
        // capture implicitly hides the cursor.
        if (!focusActive || !isCapturing || isCursorVisible) {
            return
        }

        // Recapture the pointer if focus was regained. On Android Q,
        // we have to delay a bit before requesting capture because otherwise
        // we'll hit the "requestPointerCapture called for a window that has no focus"
        // error and it will not actually capture the cursor.
        recaptureHandler.removeCallbacks(recaptureAfterFocus)
        recaptureHandler.postDelayed(recaptureAfterFocus, RECAPTURE_AFTER_FOCUS_DELAY_MS)
    }

    override fun eventHasRelativeMouseAxes(event: MotionEvent?): Boolean {
        val motionEvent = event ?: return false

        return hasRelativeMouseAxes(
            motionEvent.source,
            motionEvent.getToolType(0),
            targetView.hasPointerCapture(),
        )
    }

    override fun getRelativeAxisX(event: MotionEvent?, pointerIndex: Int): Float {
        val motionEvent = event ?: return 0f

        return sumWithHistory(motionEvent, relativeAxisX(motionEvent.source), pointerIndex)
    }

    override fun getRelativeAxisY(event: MotionEvent?, pointerIndex: Int): Float {
        val motionEvent = event ?: return 0f

        return sumWithHistory(motionEvent, relativeAxisY(motionEvent.source), pointerIndex)
    }

    override fun onInputDeviceAdded(deviceId: Int) {
        // Check if we've added a capture-compatible device
        if (!targetView.hasPointerCapture() && hasCaptureCompatibleInputDevice()) {
            targetView.requestPointerCapture()
        }
    }

    override fun onInputDeviceRemoved(deviceId: Int) {
        // Check if the capture-compatible device was removed
        if (targetView.hasPointerCapture() && !hasCaptureCompatibleInputDevice()) {
            targetView.releasePointerCapture()
        }
    }

    override fun onInputDeviceChanged(deviceId: Int) {
        // Emulating a remove+add should be sufficient for our purposes.
        //
        // Note: This callback must be handled carefully because it can happen as a result of
        // calling requestPointerCapture(). This can cause trackpad devices to gain SOURCE_MOUSE_RELATIVE
        // and re-enter this callback.
        onInputDeviceRemoved(deviceId)
        onInputDeviceAdded(deviceId)
    }

    companion object {
        private const val CHROME_OS_FEATURE = "org.chromium.arc.device_management"
        private const val RECAPTURE_AFTER_FOCUS_DELAY_MS = 500L

        @JvmStatic
        fun isCaptureProviderSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

        /**
         * Whether a device with these sources should make us request pointer capture.
         *
         * A mouse only reports SOURCE_MOUSE_RELATIVE once a window holds pointer capture,
         * so plain SOURCE_MOUSE has to count here or capture is never requested for it.
         */
        internal fun isCaptureCompatible(sources: Int, isChromeOs: Boolean): Boolean {
            // Skip touchscreens when considering compatible capture devices.
            // Samsung devices on Android 12 will report a sec_touchpad device
            // with SOURCE_TOUCHSCREEN, SOURCE_KEYBOARD, and SOURCE_MOUSE.
            // Upon enabling pointer capture, that device will switch to
            // SOURCE_KEYBOARD and SOURCE_TOUCHPAD.
            // Only skip on non ChromeOS devices cause the ChromeOS pointer else
            // gets disabled removing relative mouse capabilities
            // on Chromebooks with touchscreens
            if (supportsSource(sources, InputDevice.SOURCE_TOUCHSCREEN) && !isChromeOs) {
                return false
            }

            return supportsSource(sources, InputDevice.SOURCE_MOUSE) ||
                supportsSource(sources, InputDevice.SOURCE_MOUSE_RELATIVE) ||
                supportsSource(sources, InputDevice.SOURCE_TOUCHPAD)
        }

        // SOURCE_MOUSE_RELATIVE is how SOURCE_MOUSE appears when our view has pointer capture.
        // SOURCE_TOUCHPAD will have relative axes populated iff our view has pointer capture.
        // See https://developer.android.com/reference/android/view/View#requestPointerCapture()
        internal fun hasRelativeMouseAxes(source: Int, toolType: Int, hasPointerCapture: Boolean): Boolean =
            (source == InputDevice.SOURCE_MOUSE_RELATIVE && toolType == MotionEvent.TOOL_TYPE_MOUSE) ||
                (source == InputDevice.SOURCE_TOUCHPAD && hasPointerCapture)

        // A captured mouse reports its movement on AXIS_X and AXIS_Y. A captured touchpad
        // keeps the finger's position there and reports movement on the relative axes.
        internal fun relativeAxisX(source: Int): Int =
            if (source == InputDevice.SOURCE_MOUSE_RELATIVE) MotionEvent.AXIS_X else MotionEvent.AXIS_RELATIVE_X

        internal fun relativeAxisY(source: Int): Int =
            if (source == InputDevice.SOURCE_MOUSE_RELATIVE) MotionEvent.AXIS_Y else MotionEvent.AXIS_RELATIVE_Y

        // Android batches movement between frames into one event, and the samples it folded
        // in live in the event's history. Each one is a delta of its own, so all of them count.
        internal fun sumWithHistory(event: MotionEvent, axis: Int, pointerIndex: Int): Float {
            var total = event.getAxisValue(axis, pointerIndex)
            for (i in 0 until event.historySize) {
                total += event.getHistoricalAxisValue(axis, pointerIndex, i)
            }
            return total
        }

        private fun supportsSource(sources: Int, source: Int): Boolean = (sources and source) == source

        private fun connectedInputDeviceSources(): List<Int> =
            InputDevice.getDeviceIds().asList().mapNotNull { id -> InputDevice.getDevice(id)?.sources }
    }
}
