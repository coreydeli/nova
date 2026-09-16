package com.papi.nova.ui

import com.papi.nova.R
import com.papi.nova.shared.polaris.model.PolarisGame
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Play Setup offers the other side of a title only when it can find it there. Steam games pair
 * by app id; Steam Big Picture has none, so it pairs with Big Picture on the other side, and a
 * card nobody can choose says why instead of describing a choice it does not offer.
 */
class NovaPlayDestinationMatchTest {
    private val match = NovaPlayDestinationMatch

    private fun spaceGame(target: String, name: String, appid: String = "", space: String = "alex") =
        PolarisGame(id = "space.$space.$target", name = name, steamAppid = appid,
            space = PolarisGame.SpaceContext(space, space.replaceFirstChar { it.uppercase() }, target))

    private val desktopBigPicture = PolarisGame(id = "B6A90319", name = "Steam Big Picture", steamBigPicture = true)
    private val desktopControl = PolarisGame(id = "992FF124", name = "Control Ultimate Edition", source = "steam", steamAppid = "870780")
    private val desktopLowRes = PolarisGame(id = "F727EEEE", name = "Low Res Desktop")
    private val desktopLibrary = listOf(desktopLowRes, desktopControl, desktopBigPicture)

    private val spaceBigPicture = spaceGame("big-picture-v1", "Steam Big Picture")
    private val spaceControl = spaceGame("870780", "Control Ultimate Edition", appid = "870780")
    private val spaceLibrary = listOf(spaceControl, spaceBigPicture)

    @Test
    fun aSpacesBigPictureOpensTheDesktopsBigPicture() {
        assertTrue(match.isBigPicture(spaceBigPicture))
        assertTrue("Big Picture has no app id, so Desktop has to be asked by what it is", match.needsDesktopLibrary(spaceBigPicture))
        assertSame(desktopBigPicture, match.desktopTitle(spaceBigPicture, desktopLibrary))
    }

    @Test
    fun theDesktopsBigPictureOpensEachSpacesBigPicture() {
        assertTrue(match.isBigPicture(desktopBigPicture))
        assertTrue(match.needsSpaceLibrary(desktopBigPicture))
        assertSame(desktopBigPicture, match.desktopTitle(desktopBigPicture, desktopLibrary))
        val title = match.spaceTitle(desktopBigPicture, spaceLibrary)
        assertSame(spaceBigPicture, title)
        assertNull("Big Picture itself is never offered through that Space's Steam", match.spaceSteam(desktopBigPicture, spaceLibrary, title))
        assertNull(match.spaceSteam(desktopBigPicture, listOf(spaceControl), null))
    }

    @Test
    fun steamGamesStillPairByAppIdAndFallBackToThatSpacesSteam() {
        assertSame(desktopControl, match.desktopTitle(spaceControl, desktopLibrary))
        assertSame(spaceControl, match.spaceTitle(desktopControl, spaceLibrary))
        val notInstalled = listOf(spaceBigPicture)
        assertNull(match.spaceTitle(desktopControl, notInstalled))
        assertSame(spaceBigPicture, match.spaceSteam(desktopControl, notInstalled, null))
    }

    @Test
    fun anOlderHostWithoutTheMarkLeavesTheDesktopsBigPictureUnpaired() {
        val unmarked = desktopBigPicture.copy(steamBigPicture = false)
        assertFalse(match.isBigPicture(unmarked))
        assertFalse("nothing to look for, so no Space library is fetched", match.needsSpaceLibrary(unmarked))
        assertNull(match.desktopTitle(spaceBigPicture, listOf(desktopLowRes, desktopControl, unmarked)))
    }

    @Test
    fun anEntryWithoutAnAppIdNeverMatchesAnotherEntryWithout() {
        assertFalse(match.needsSpaceLibrary(desktopLowRes))
        assertNull("an empty app id is not a match for Big Picture's empty one", match.spaceTitle(desktopLowRes, spaceLibrary))
        val spaceOther = spaceGame("12", "Custom", space = "alex")
        assertFalse(match.needsDesktopLibrary(spaceOther))
        assertNull(match.desktopTitle(spaceOther, desktopLibrary))
    }

    @Test
    fun aCardNobodyCanChooseSaysWhy() {
        fun card(id: String, game: PolarisGame?, libraryEnabled: Boolean = true, canOpen: Boolean = true,
                 state: String = "ready", blockedReason: String? = null, viaSteam: Boolean = false) =
            NovaPlayDestination(id, id, game, state, libraryEnabled, canOpen, blockedReason, viaSteam)

        assertEquals(R.string.nova_space_option_desktop, match.caption(card("desktop", desktopBigPicture)))
        assertEquals(R.string.nova_space_option_desktop_missing, match.caption(card("desktop", null)))
        assertEquals(R.string.nova_space_option_no_library, match.caption(card("alex", null, libraryEnabled = false)))
        assertEquals(R.string.nova_space_option_missing, match.caption(card("alex", null)))
        assertEquals(R.string.nova_space_blocked_starting, match.caption(card("alex", spaceBigPicture, canOpen = false, state = "starting")))
        assertEquals(R.string.nova_space_blocked_generic, match.caption(card("alex", spaceBigPicture, canOpen = false, state = "ready")))
        assertEquals(R.string.nova_space_option_via_steam, match.caption(card("alex", spaceBigPicture, viaSteam = true)))
        assertEquals(R.string.nova_space_option_use, match.caption(card("alex", spaceControl)))
    }

    @Test
    fun theWhyNamesChangeSpaceInTheLibraryWithoutADash() {
        val strings = File("src/main/res/values/strings.xml").readText()
        for (name in listOf("nova_space_option_desktop_missing", "nova_space_option_missing")) {
            val text = Regex("""<string name="$name">(.*?)</string>""").find(strings)?.groupValues?.get(1)
            requireNotNull(text) { "$name is missing" }
            assertTrue("$name points at the way that does work", text.contains("Change Space in the Library"))
            assertFalse("$name uses no dash", text.contains("—") || text.contains("–") || text.contains(" - "))
        }
    }
}
