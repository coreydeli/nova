package com.papi.nova.ui

import com.papi.nova.shared.polaris.model.PolarisGame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class NovaLibraryLiveRefreshTest {
    private fun game(revision: String) = PolarisGame(
        id = "992FF124-4652-5708-501D-EDDBBB80EA8E",
        name = "Control Ultimate Edition",
        artwork = PolarisGame.ArtworkManifest(revision = revision),
    )

    @Test
    fun readsAtOnceOnReturnThenEveryThirtySecondsAndBacksOffToTwoMinutes() {
        assertEquals(0L, NovaLibraryLiveRefresh.delayBeforeRead(first = true, failures = 0))
        assertEquals(30_000L, NovaLibraryLiveRefresh.delayBeforeRead(first = false, failures = 0))
        assertEquals(60_000L, NovaLibraryLiveRefresh.delayBeforeRead(first = false, failures = 1))
        assertEquals(120_000L, NovaLibraryLiveRefresh.delayBeforeRead(first = false, failures = 2))
        assertEquals(120_000L, NovaLibraryLiveRefresh.delayBeforeRead(first = false, failures = 9))
    }

    @Test
    fun neverRacesALoadThePersonOrTheLibraryStarted() {
        assertTrue(NovaLibraryLiveRefresh.mayRead(initialLoading = false, refreshing = false, spaceSwitching = false))
        assertFalse(NovaLibraryLiveRefresh.mayRead(initialLoading = true, refreshing = false, spaceSwitching = false))
        assertFalse(NovaLibraryLiveRefresh.mayRead(initialLoading = false, refreshing = true, spaceSwitching = false))
        assertFalse(NovaLibraryLiveRefresh.mayRead(initialLoading = false, refreshing = false, spaceSwitching = true))
    }

    @Test
    fun changesTheScreenOnlyWhenTheLibraryChanged() {
        val shown = listOf(game("a1"))
        assertNull(NovaLibraryLiveRefresh.changedLibrary(shown, listOf(game("a1"))))
        val read = listOf(game("b2"))
        assertSame(read, NovaLibraryLiveRefresh.changedLibrary(shown, read))
    }

    @Test
    fun aFreshPlaytimeReadStampIsNoChangeButNewPlaytimeIs() {
        // Polaris stamps read_at with the time it re-read Steam's files, so it moves on almost
        // every 30 s read; treating that as a change would rebuild the library each time.
        fun played(seconds: Long, readAt: Long) =
            game("a1").copy(playTime = PolarisGame.PlayTime(seconds = seconds, source = "steam", readAt = readAt))
        val shown = listOf(played(3_600, 1_789_000_000))
        assertNull(NovaLibraryLiveRefresh.changedLibrary(shown, listOf(played(3_600, 1_789_000_031))))
        val more = listOf(played(3_660, 1_789_000_061))
        assertSame(more, NovaLibraryLiveRefresh.changedLibrary(shown, more))
    }
}
