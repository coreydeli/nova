package com.papi.nova.ui

import android.content.Context
import com.papi.nova.shared.polaris.model.PolarisGame
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@Config(sdk = [33])
@RunWith(RobolectricTestRunner::class)
class NovaFaceButtonLayoutOverridesTest {
    private val context: Context
        get() = RuntimeEnvironment.getApplication()
    private val preferences
        get() = context.getSharedPreferences("nova_prefs", Context.MODE_PRIVATE)
    private val game = PolarisGame(id = "zelda", appId = 7, name = "The Legend of Zelda")

    @Before
    @After
    fun clearPreferences() {
        preferences.edit().clear().commit()
    }

    @Test
    fun savedChoiceIsCanonicalAndScopedToTheGame() {
        NovaFaceButtonLayoutOverrides.save(context, game, " Positions ")

        assertEquals("positions", NovaFaceButtonLayoutOverrides.load(context, game))
        assertEquals("positions", preferences.getString("face_button_layout_override_zelda", null))
        assertNull(NovaFaceButtonLayoutOverrides.load(context, PolarisGame(id = "other", appId = 8, name = "Other")))
    }

    @Test
    fun anUnknownValueIsRetiredRatherThanTrusted() {
        preferences.edit().putString("face_button_layout_override_zelda", "sideways").commit()

        assertNull(NovaFaceButtonLayoutOverrides.load(context, game))
        assertFalse(preferences.contains("face_button_layout_override_zelda"))

        NovaFaceButtonLayoutOverrides.save(context, game, "labels")
        NovaFaceButtonLayoutOverrides.save(context, game, "nonsense")
        assertNull(NovaFaceButtonLayoutOverrides.load(context, game))
    }

    @Test
    fun theLaunchFlipFollowsTheGameThenTheSettings() {
        assertTrue(NovaFaceButtonLayoutOverrides.flipFaceButtons("positions", globalFlip = false))
        assertFalse(NovaFaceButtonLayoutOverrides.flipFaceButtons("labels", globalFlip = true))
        assertTrue(NovaFaceButtonLayoutOverrides.flipFaceButtons(null, globalFlip = true))
        assertFalse(NovaFaceButtonLayoutOverrides.flipFaceButtons("", globalFlip = false))
    }
}
