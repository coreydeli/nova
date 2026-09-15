package com.papi.nova.preferences

import android.os.Build
import android.view.Display

/**
 * Panel-capability rules for the standard stream FPS options.
 *
 * Extracted from the legacy settings fragment so every surface that offers an FPS
 * choice (legacy PreferenceFragment, Compose settings, Play Setup) culls with the
 * same thresholds instead of each growing its own.
 */
object NovaDisplayFpsCapability {
    /**
     * The standard stream FPS options, ascending. 144, 165 and 240 are the panels
     * shipping in handhelds and tablets; Polaris advertises up to its owned-display
     * ceiling (240 by default, polaris#686), so a panel that can present them is
     * offered them here rather than only through the Native entry.
     */
    @JvmField
    val STANDARD_FPS_VALUES: List<Int> = listOf(30, 60, 90, 120, 144, 165, 240)

    /**
     * A standard option above 60 is offered when the panel is within this many Hz
     * of it, so a 119.88 Hz panel still presents 120 and an 88 Hz one presents 90.
     */
    const val PANEL_TOLERANCE_HZ = 2f

    /** Minimum panel refresh rate required to offer the 120 FPS option. */
    const val FPS_120_MIN_PANEL_HZ = 120f - PANEL_TOLERANCE_HZ

    /** Minimum panel refresh rate required to offer the 90 FPS option. */
    const val FPS_90_MIN_PANEL_HZ = 90f - PANEL_TOLERANCE_HZ

    /** The slowest panel that may be offered [fps]; 30 and 60 are always offered. */
    @JvmStatic
    fun minPanelHzFor(fps: Int): Float = if (fps <= 60) 0f else fps - PANEL_TOLERANCE_HZ

    /** The panel's fastest refresh rate across all supported display modes. */
    @JvmStatic
    fun maxSupportedFps(display: Display): Float {
        var maxSupportedFps = display.refreshRate
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (candidate in display.supportedModes) {
                if (candidate.refreshRate > maxSupportedFps) {
                    maxSupportedFps = candidate.refreshRate
                }
            }
        }
        return maxSupportedFps
    }

    /** The standard FPS options this panel can actually present, ascending. */
    @JvmStatic
    fun allowedFpsValues(maxSupportedFps: Float): List<Int> =
        STANDARD_FPS_VALUES.filter { maxSupportedFps >= minPanelHzFor(it) }

    /**
     * Coerces a stored standard FPS value down to the fastest option the panel allows.
     * Non-standard values (native/custom rates) pass through untouched, matching the
     * legacy culling which only ever rewrote the standard entries it removed.
     */
    @JvmStatic
    fun coerce(requestedFps: Int, maxSupportedFps: Float): Int {
        val allowed = allowedFpsValues(maxSupportedFps)
        if (requestedFps in allowed || requestedFps !in STANDARD_FPS_VALUES) {
            return requestedFps
        }
        return allowed.last { it <= requestedFps }
    }
}
