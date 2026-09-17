package com.papi.nova.ui

import com.papi.nova.shared.polaris.model.PolarisGame

/**
 * When the library re-reads the host while it is on screen.
 *
 * A cover saved in Polaris's console, a renamed entry or a newly imported game reaches an open
 * library without pull-to-refresh. The first read comes as soon as the library is back on
 * screen, then one every [INTERVAL_MS]. A failed read doubles the wait up to [MAX_INTERVAL_MS]
 * and shows nothing, since nobody asked for it. Covers are cached by manifest revision, so a
 * read loads only the artwork that changed.
 */
internal object NovaLibraryLiveRefresh {
    const val INTERVAL_MS = 30_000L
    const val MAX_INTERVAL_MS = 120_000L

    /** The wait before the next read: none on return, then the interval, doubled after each failure. */
    fun delayBeforeRead(first: Boolean, failures: Int): Long = when {
        first -> 0L
        failures <= 0 -> INTERVAL_MS
        else -> (INTERVAL_MS shl failures.coerceAtMost(2)).coerceAtMost(MAX_INTERVAL_MS)
    }

    /**
     * Whether a background read may run or land now. The first load, a refresh the person asked
     * for and a Space switch own the library until they finish, so a quiet read never races them.
     */
    fun mayRead(initialLoading: Boolean, refreshing: Boolean, spaceSwitching: Boolean): Boolean =
        !initialLoading && !refreshing && !spaceSwitching

    /**
     * The library to show after a read, or null when nothing changed and the screen stays as it is.
     * Polaris stamps Steam playtime with the moment it re-read Steam's files, which is new on
     * almost every read and shows nowhere, so that stamp alone is no change.
     */
    fun changedLibrary(shown: List<PolarisGame>, read: List<PolarisGame>): List<PolarisGame>? =
        read.takeIf { withoutReadStamps(it) != withoutReadStamps(shown) }

    private fun withoutReadStamps(games: List<PolarisGame>): List<PolarisGame> =
        games.map { game -> game.playTime?.let { game.copy(playTime = it.copy(readAt = 0)) } ?: game }
}
