package com.papi.nova.ui

import com.papi.nova.shared.polaris.model.PolarisGame

internal data class NovaPlayDestination(
    val id: String,
    val name: String,
    val game: PolarisGame?,
    val state: String,
)
