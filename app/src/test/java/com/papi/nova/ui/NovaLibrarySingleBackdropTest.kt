package com.papi.nova.ui

import com.papi.nova.shared.polaris.model.PolarisGame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * One artwork layer under the library in every layout (papi, 2026-09-16, on the open grid build:
 * "looks like its good in Stage, but it doubles in Grid and Compact"). His Compact screenshot's
 * fine detail matched a single full-screen crop of the focused hero (r = 0.85) and no second copy
 * at any placement (|r| <= 0.02 on the residual). What read as a second background was the poster
 * grid's painted bottom wash: window colour up to 0.95 alpha over the grid's bounds, ending in a
 * line above the controller hints, which the removed panel used to frame.
 */
class NovaLibrarySingleBackdropTest {
    private fun read(path: String): String = File(path).readText()

    private fun game(
        lastLaunched: Long = 0,
        revision: String = "r1",
        heroUrl: String = "/polaris/v1/games/g/artwork/hero",
        id: String = "g",
    ) = PolarisGame(
        id = id,
        name = "Big Walk",
        lastLaunched = lastLaunched,
        artwork = PolarisGame.ArtworkManifest(
            revision = revision,
            assets = PolarisGame.ArtworkAssets(
                hero = PolarisGame.ArtworkAsset(url = heroUrl, cached = true),
            ),
        ),
    )

    @Test
    fun aRefreshThatOnlyMovesPlayStatsKeepsTheSameBackdrop() {
        val before = novaLibraryCinematicBackdropTarget(game(lastLaunched = 0))
        val after = novaLibraryCinematicBackdropTarget(game(lastLaunched = 1_789_600_000))
        assertNotNull(before)
        assertEquals(
            "a library refresh that only changes last-launched must not restart the backdrop crossfade; " +
                "the target is the artwork, not every field of the game",
            before,
            after,
        )
        assertEquals(before.hashCode(), after.hashCode())
    }

    @Test
    fun newArtworkOrAnotherGameIsANewBackdrop() {
        val current = novaLibraryCinematicBackdropTarget(game())
        assertNotEquals(current, novaLibraryCinematicBackdropTarget(game(revision = "r2")))
        assertNotEquals(current, novaLibraryCinematicBackdropTarget(game(heroUrl = "/other")))
        assertNotEquals(current, novaLibraryCinematicBackdropTarget(game(id = "other")))
    }

    @Test
    fun theGridFadesItsOwnPixelsAndPaintsNothingOverTheBackdrop() {
        val activity = read("src/main/java/com/papi/nova/ui/NovaLibraryActivity.kt")
        val chrome = read("src/main/java/com/papi/nova/ui/NovaLibraryCinematicChrome.kt")
        val fade = chrome
            .substringAfter("internal fun Modifier.novaLibraryGridBottomFade(")
            .substringBefore("\n@Composable")

        assertEquals(
            "exactly one full-bleed artwork layer: the shared cinematic backdrop, called once by the library screen",
            1,
            activity.windowed("NovaLibraryCinematicBackdrop(".length).count { it == "NovaLibraryCinematicBackdrop(" },
        )
        assertTrue(
            "the poster grid fades on its own layer with a DstIn mask; a colour painted over its bounds is a " +
                "second background that ends in a seam where the grid stops above the hints",
            activity.contains("modifier = Modifier.fillMaxSize().novaLibraryGridBottomFade(NovaLibraryGridScrollFadeHeight)") &&
                fade.contains("compositingStrategy = CompositingStrategy.Offscreen") &&
                fade.contains("blendMode = BlendMode.DstIn"),
        )
        assertFalse(
            "the mask paints no theme colour, only alpha",
            fade.contains("LocalNovaComposeColors") || fade.contains("colors.window") || fade.contains(".background("),
        )
        assertFalse(
            "the painted wash box at the grid's foot is gone (papi: it doubles in Grid and Compact)",
            activity.contains(".height(NovaLibraryGridScrollFadeHeight)") ||
                activity.contains("LocalNovaComposeColors.current.window.copy(alpha = 0.95f)"),
        )
        assertFalse(
            "the backdrop target compares by artwork identity, not as a data class of the whole game",
            chrome.contains("data class NovaLibraryCinematicBackdropTarget"),
        )
    }
}
