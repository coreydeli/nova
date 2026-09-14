package com.papi.nova.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.papi.nova.manager.WorkerLaunchContract
import com.papi.nova.shared.polaris.model.PolarisGame
import com.papi.nova.ui.compose.NovaComposeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class NovaSpaceComposeTest {
    @get:Rule val compose = createComposeRule()
    private val space = PolarisGame(id = WorkerLaunchContract.APP_UUID, name = "Living Room")

    private fun awaitFocus(tag: String) {
        compose.waitUntil(timeoutMillis = 2000) {
            runCatching { compose.onNodeWithTag(tag).assertIsFocused(); true }.getOrDefault(false)
        }
    }

    @Test fun primaryHasFirstFocusAndControllerCanOpenWithoutDetails() {
        var opens = 0
        compose.setContent {
            NovaComposeTheme {
                NovaSpaceContent(space, "Gaming PC", null, { opens++ }, {}, {})
            }
        }
        awaitFocus("nova-space-primary")
        compose.onNodeWithText("Your Space").assertIsDisplayed()
        compose.onNodeWithTag("nova-space-primary").assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionCenter) }
        compose.runOnIdle { assertEquals(1, opens) }
        compose.onNodeWithTag("nova-game-detail-overview").assertDoesNotExist()
    }

    @Test fun settingsReturnRestoresControllerFocusAndDoNotLaunch() {
        val settings = mutableStateOf(false)
        var opens = 0
        compose.setContent {
            NovaComposeTheme {
                NovaSpaceContent(space, "Gaming PC", null, { opens++ }, { settings.value = true },
                    { settings.value = false }, showSettings = settings.value)
            }
        }
        awaitFocus("nova-space-primary")
        compose.onNodeWithTag("nova-space-primary").performKeyInput { pressKey(Key.DirectionRight) }
        awaitFocus("nova-space-settings")
        compose.onNodeWithTag("nova-space-settings").assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionCenter) }
        awaitFocus("nova-space-settings-done")
        compose.onNodeWithTag("nova-space-settings-done").assertIsFocused().performClick()
        awaitFocus("nova-space-settings")
        compose.onNodeWithTag("nova-space-settings").assertIsFocused()
        compose.runOnIdle { assertEquals(0, opens) }
    }

    @Test fun longSpaceNameAndLargeTextKeepPhoneActionsReachable() {
        compose.setContent {
            NovaComposeTheme {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.5f)) {
                    NovaSpaceContent(space.copy(name = "Family Gaming Space With A Longer Name"), "Gaming PC", null, {}, {}, {},
                        modifier = Modifier.requiredSize(360.dp, 390.dp))
                }
            }
        }
        compose.onNodeWithTag("nova-space-primary").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("nova-space-settings").performScrollTo().assertIsDisplayed()
    }

    @Test fun anotherDeviceCannotBeResumedFromTheSpaceAction() {
        val session = NovaLibraryActiveSessionUiState(WorkerLaunchContract.APP_ID, WorkerLaunchContract.APP_UUID,
            space.name, "Another Device", false, 0, false, false, 1920, 1080, 60f)
        compose.setContent {
            NovaComposeTheme { NovaSpaceContent(space, "Gaming PC", session, {}, {}, {}) }
        }
        awaitFocus("nova-space-settings")
        compose.onNodeWithTag("nova-space-primary").assertIsNotEnabled()
        compose.onNodeWithText("Resume Space").assertDoesNotExist()
    }

    @Test fun settingsOffer120FpsWithoutHostTopologyControls() {
        var selected = 0
        val row = NovaPlaySetupRowState(NovaPlaySetupRow.FRAME_RATE, "Frame Rate", "Saved For Next Launch", "60 FPS", "",
            listOf(NovaPlaySetupOption("120 FPS", "", onSelect = { selected = 120 })))
        compose.setContent {
            NovaComposeTheme {
                NovaSpaceContent(space, "Gaming PC", null, {}, {}, {}, showSettings = true, settingsRows = listOf(row,
                    row.copy(row = NovaPlaySetupRow.WHERE_IT_RUNS, label = "Where It Runs")))
            }
        }
        compose.onNodeWithText("120 FPS").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(120, selected) }
        compose.onNodeWithText("Where It Runs").assertDoesNotExist()
        compose.onNodeWithText("Your Space").assertExists()
    }

    @Test fun hostConstraintKeepsSettingsReachableWithAController() {
        val ready = mutableStateOf(true)
        compose.setContent {
            NovaComposeTheme {
                NovaSpaceContent(space, "Gaming PC", null, {}, {}, {}, primaryEnabled = ready.value,
                    message = if (ready.value) null else "Polaris is offering 60 FPS for this device.")
            }
        }
        awaitFocus("nova-space-primary")
        compose.runOnIdle { ready.value = false }
        awaitFocus("nova-space-settings")
        compose.onNodeWithTag("nova-space-primary").assertIsNotEnabled()
        compose.onNodeWithTag("nova-space-message").assertIsDisplayed()
    }
}
