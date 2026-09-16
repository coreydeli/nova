package com.papi.nova.ui

import com.papi.nova.shared.polaris.model.PolarisGame

internal data class NovaPlayDestination(
    val id: String,
    val name: String,
    val game: PolarisGame?,
    val state: String,
    /** The Space serves a game library at all; a Space without one is not asked for this title. */
    val libraryEnabled: Boolean = true,
    /** The host's answer to "can this device open it now", plus a Space already running for us. */
    val canOpen: Boolean = true,
    val blockedReason: String? = null,
    /** [game] is that Space's Steam, offered because the title is not installed there yet. */
    val viaSteam: Boolean = false,
)
