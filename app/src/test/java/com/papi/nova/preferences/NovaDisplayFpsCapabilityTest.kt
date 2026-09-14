package com.papi.nova.preferences

import org.junit.Assert.assertEquals
import org.junit.Test

class NovaDisplayFpsCapabilityTest {
    @Test
    fun panelBelow90ThresholdOffersOnly30And60() {
        assertEquals(listOf(30, 60), NovaDisplayFpsCapability.allowedFpsValues(59.9f))
        assertEquals(listOf(30, 60), NovaDisplayFpsCapability.allowedFpsValues(60f))
        assertEquals(listOf(30, 60), NovaDisplayFpsCapability.allowedFpsValues(87.9f))
    }

    @Test
    fun panelAt90ThresholdAdds90ButNot120() {
        assertEquals(listOf(30, 60, 90), NovaDisplayFpsCapability.allowedFpsValues(88f))
        assertEquals(listOf(30, 60, 90), NovaDisplayFpsCapability.allowedFpsValues(90f))
        assertEquals(listOf(30, 60, 90), NovaDisplayFpsCapability.allowedFpsValues(117.9f))
    }

    @Test
    fun panelAt120ThresholdAdds120ButNot144() {
        assertEquals(listOf(30, 60, 90, 120), NovaDisplayFpsCapability.allowedFpsValues(118f))
        assertEquals(listOf(30, 60, 90, 120), NovaDisplayFpsCapability.allowedFpsValues(120f))
        assertEquals(listOf(30, 60, 90, 120), NovaDisplayFpsCapability.allowedFpsValues(141.9f))
    }

    // The 165 Hz tablet on polaris#686 could only ask for 165 through the Native
    // entry; with the host advertising up to 240, the ladder has to offer it.
    @Test
    fun fasterPanelsAreOfferedTheRatesTheyCanPresent() {
        assertEquals(listOf(30, 60, 90, 120, 144), NovaDisplayFpsCapability.allowedFpsValues(144f))
        assertEquals(listOf(30, 60, 90, 120, 144, 165), NovaDisplayFpsCapability.allowedFpsValues(165f))
        assertEquals(listOf(30, 60, 90, 120, 144, 165), NovaDisplayFpsCapability.allowedFpsValues(237.9f))
        assertEquals(
            listOf(30, 60, 90, 120, 144, 165, 240),
            NovaDisplayFpsCapability.allowedFpsValues(240f),
        )
    }

    @Test
    fun thresholdsSitJustUnderEachRateSoRealPanelsQualify() {
        // 119.88 Hz panels must still present 120, as they always have.
        assertEquals(118f, NovaDisplayFpsCapability.FPS_120_MIN_PANEL_HZ, 0f)
        assertEquals(88f, NovaDisplayFpsCapability.FPS_90_MIN_PANEL_HZ, 0f)
        assertEquals(0f, NovaDisplayFpsCapability.minPanelHzFor(30), 0f)
        assertEquals(0f, NovaDisplayFpsCapability.minPanelHzFor(60), 0f)
        assertEquals(163f, NovaDisplayFpsCapability.minPanelHzFor(165), 0f)
        assertEquals(238f, NovaDisplayFpsCapability.minPanelHzFor(240), 0f)
    }

    @Test
    fun coerceFollowsTheLegacyFallbackChain() {
        // 120 on a panel that can only offer 90 falls to 90, exactly like the
        // legacy removeEntryFromListAndSetValue(FPS, "120", "90") rewrite.
        assertEquals(90, NovaDisplayFpsCapability.coerce(120, 100f))
        // 120 on a 60Hz panel cascades all the way down, like the legacy
        // double rewrite 120 -> 90 -> 60.
        assertEquals(60, NovaDisplayFpsCapability.coerce(120, 60f))
        assertEquals(60, NovaDisplayFpsCapability.coerce(90, 60f))
        // A pin saved on a 240 Hz panel lands on the fastest rate a slower one has.
        assertEquals(165, NovaDisplayFpsCapability.coerce(240, 165f))
        assertEquals(120, NovaDisplayFpsCapability.coerce(165, 120f))
        assertEquals(60, NovaDisplayFpsCapability.coerce(240, 60f))
    }

    @Test
    fun coerceLeavesAllowedValuesAlone() {
        assertEquals(120, NovaDisplayFpsCapability.coerce(120, 120f))
        assertEquals(90, NovaDisplayFpsCapability.coerce(90, 90f))
        assertEquals(30, NovaDisplayFpsCapability.coerce(30, 60f))
        assertEquals(165, NovaDisplayFpsCapability.coerce(165, 165f))
        assertEquals(240, NovaDisplayFpsCapability.coerce(240, 240f))
    }

    @Test
    fun coerceLeavesNonStandardValuesAlone() {
        // The legacy culling only ever rewrote the standard entries it removed;
        // a native or custom rate must pass through untouched. 144 used to be the
        // example here and is a standard option now, so a rate that stays custom
        // carries the invariant instead.
        assertEquals(100, NovaDisplayFpsCapability.coerce(100, 60f))
        assertEquals(75, NovaDisplayFpsCapability.coerce(75, 60f))
        assertEquals(59, NovaDisplayFpsCapability.coerce(59, 60f))
    }
}
