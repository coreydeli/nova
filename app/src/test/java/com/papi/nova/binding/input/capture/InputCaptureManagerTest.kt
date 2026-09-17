package com.papi.nova.binding.input.capture

import android.app.Activity
import android.os.Build
import android.view.View
import com.papi.nova.BuildConfig
import com.papi.nova.R
import com.papi.nova.binding.input.evdev.EvdevListener
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class InputCaptureManagerTest {
    @Test
    @Config(sdk = [33])
    fun nativePointerCaptureWinsOnAndroidOAndLaterForEveryFlavor() {
        // Root builds used to reach for evdev first, which needs su, even where Android's own
        // pointer capture works. Evdev is only the fallback below Android O. Only the root
        // flavor's unit tests can catch that ordering; on nonRoot evdev is never supported.
        val provider = InputCaptureManager.getInputCaptureProvider(streamActivity(), NoOpEvdevListener)

        assertTrue(provider is AndroidNativePointerCaptureProvider)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    fun aDeviceWithNoCaptureSupportGetsTheNullProviderInsteadOfNull() {
        // Game dereferences the provider unconditionally, so a null here crashes the stream.
        assumeFalse(BuildConfig.ROOT_BUILD)

        val provider = InputCaptureManager.getInputCaptureProvider(streamActivity(), NoOpEvdevListener)

        assertTrue(provider is NullCaptureProvider)
    }

    private fun streamActivity(): Activity {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        activity.setContentView(View(activity).apply { id = R.id.streamContainer })
        return activity
    }

    private object NoOpEvdevListener : EvdevListener {
        override fun mouseMove(deltaX: Int, deltaY: Int) = Unit

        override fun mouseButtonEvent(buttonId: Int, down: Boolean) = Unit

        override fun mouseVScroll(amount: Byte) = Unit

        override fun mouseHScroll(amount: Byte) = Unit

        override fun keyboardEvent(buttonDown: Boolean, keyCode: Short) = Unit
    }
}
