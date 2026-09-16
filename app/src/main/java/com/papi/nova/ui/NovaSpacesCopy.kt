package com.papi.nova.ui

import androidx.annotation.StringRes
import com.papi.nova.R
import com.papi.nova.api.PolarisSpace
import com.papi.nova.api.PolarisSpaces

/**
 * One vocabulary for Spaces, keyed on the words the host sends, so the library, the chooser,
 * Play Setup and the host card describe the same Space with the same words.
 */
internal object NovaSpacesCopy {
    /** One word per state of /polaris/v1/spaces; anything else, or no answer yet, is "Status unknown". */
    @StringRes
    fun stateLabel(state: String?): Int = when (state) {
        "ready" -> R.string.nova_space_state_ready
        "starting" -> R.string.nova_space_state_starting
        "running" -> R.string.nova_space_state_running
        "stopping" -> R.string.nova_space_state_stopping
        "in_use" -> R.string.nova_space_state_in_use
        "unavailable" -> R.string.nova_space_state_unavailable
        else -> R.string.nova_space_state_unknown
    }

    /** Why the host cannot offer Spaces, or null while it can. */
    @StringRes
    fun unavailableReason(snapshot: PolarisSpaces): Int? {
        if (snapshot.available) return null
        return when (snapshot.unavailableReason) {
            "no_space_assigned" -> R.string.nova_space_unavailable_no_space_assigned
            "stopping" -> R.string.nova_space_unavailable_stopping
            "reconfiguring" -> R.string.nova_space_unavailable_reconfiguring
            "admin_failed" -> R.string.nova_space_unavailable_admin_failed
            "selection_failed" -> R.string.nova_space_unavailable_selection_failed
            "controller_missing" -> R.string.nova_space_unavailable_controller_missing
            else -> R.string.nova_space_unavailable_generic
        }
    }

    /** A device with nothing to stream: the host says so, or lists no Space and allows no Desktop. */
    fun noSpaceAssigned(snapshot: PolarisSpaces): Boolean = snapshot.enabled &&
        (snapshot.unavailableReason == "no_space_assigned" || (snapshot.spaces.isEmpty() && !snapshot.desktopAllowed))

    /** Why this device cannot change Space right now, or null while it can. */
    @StringRes
    fun switchBlockedReason(snapshot: PolarisSpaces): Int? {
        if (snapshot.canSwitch) return null
        unavailableReason(snapshot)?.let { return it }
        return when (snapshot.switchBlockedReason) {
            "your_stream" -> R.string.nova_space_switch_blocked_your_stream
            "desktop_stream" -> R.string.nova_space_switch_blocked_desktop_stream
            else -> R.string.nova_space_switch_blocked_generic
        }
    }

    /** Why a Space cannot be opened now, or null when it can be opened or resumed. */
    @StringRes
    fun openBlockedReason(space: PolarisSpace): Int? = openBlockedReason(space.state, space.openable, space.blockedReason)

    @StringRes
    fun openBlockedReason(state: String, openable: Boolean, blockedReason: String?): Int? = when {
        openable -> null
        blockedReason == "at_capacity" -> R.string.nova_space_blocked_at_capacity
        state == "starting" -> R.string.nova_space_blocked_starting
        state == "stopping" -> R.string.nova_space_blocked_stopping
        state == "in_use" -> R.string.nova_space_in_use
        state == "unavailable" -> R.string.nova_space_blocked_unavailable
        else -> R.string.nova_space_blocked_generic
    }

    /** The short form of [openBlockedReason], for a Play button that cannot act yet. */
    @StringRes
    fun playBlockedLabel(state: String, blockedReason: String?): Int = when {
        blockedReason == "at_capacity" -> R.string.nova_space_play_blocked_at_capacity
        state == "starting" -> R.string.nova_space_play_blocked_starting
        state == "stopping" -> R.string.nova_space_play_blocked_stopping
        state == "in_use" -> R.string.nova_space_play_blocked_in_use
        state == "unavailable" -> R.string.nova_space_play_blocked_unavailable
        else -> R.string.nova_space_checking
    }

    enum class EmptyLibraryCase { NO_SPACE_ASSIGNED, HOST_UNAVAILABLE }

    /**
     * Whether an empty library is explained by Spaces, and how. Null when it is empty for an
     * ordinary reason, so the usual "No games yet" stands. Before this, a device with no
     * Space assigned read "No games yet. Manage Library", a diagnosis of the wrong thing.
     */
    fun emptyLibraryCase(snapshot: PolarisSpaces): EmptyLibraryCase? = when {
        !snapshot.enabled -> null
        noSpaceAssigned(snapshot) -> EmptyLibraryCase.NO_SPACE_ASSIGNED
        !snapshot.available -> EmptyLibraryCase.HOST_UNAVAILABLE
        else -> null
    }
}
