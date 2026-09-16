package com.papi.nova.ui

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Spaces speak one grammar on every surface.
 *
 * The feature arrived in three vocabularies (Space, environment, profile; Change, Choose,
 * Play In) because nothing read the whole app at once. This does, so the words that left
 * stay gone and the words that stayed are the resources every surface shares.
 */
class NovaSpacesVocabularyTest {
    private val spacesSources = listOf(
        "src/main/java/com/papi/nova/ui/NovaSpaceChooser.kt",
        "src/main/java/com/papi/nova/ui/NovaEnvironmentBar.kt",
        "src/main/java/com/papi/nova/ui/NovaSpaceContent.kt",
        "src/main/java/com/papi/nova/ui/NovaSpacesCopy.kt",
        "src/main/java/com/papi/nova/ui/NovaLibraryActivity.kt",
        "src/main/java/com/papi/nova/ui/NovaGameDetailActivity.kt",
        "src/main/java/com/papi/nova/grid/PcGridAdapter.kt",
        "src/main/java/com/papi/nova/Game.kt",
        "src/main/java/com/papi/nova/GameMenu.kt",
        "src/main/java/com/papi/nova/ui/NovaQuickMenuUiState.kt",
    )

    @Test
    fun theWordsThatLeftStayGone() {
        val banned = listOf(
            "\"Choose Space", "\"Playing In", "Your Environment", "Changing Environment", "\"Leave Space",
            "\"Space Could Not Start", "\"Spaces Available", "Refresh Spaces", "\"Space In Use", "\"Where This Game Opens",
            "\"Change Space\"", "\"Open Steam Big Picture\"", "Checking Space Status", "Choose Where To Play",
        )
        val offenders = spacesSources.flatMap { path ->
            val text = File(path).readText()
            banned.filter { text.contains(it) }.map { "$path: $it" }
        }
        assertEquals(
            "a Spaces surface says these in a resource of its own, in the shared vocabulary, or not at all:\n" + offenders.joinToString("\n"),
            emptyList<String>(),
            offenders,
        )
    }

    @Test
    fun spaceStringsUseTheGlossary() {
        val strings = File("src/main/res/values/strings.xml").readText()
        val spaceStrings = Regex("""<string name="(nova_space_[a-z_]+)">(.*?)</string>""")
            .findAll(strings).map { it.groupValues[1] to it.groupValues[2] }.toList()
        assertTrue("the Spaces strings live in strings.xml, not in Kotlin", spaceStrings.size >= 80)
        val banned = listOf("profile", "seat", "worker", "environment", "multiseat", "Choose Space", "Play In", "Playing In ")
        val offenders = spaceStrings.filter { (_, text) -> banned.any { text.contains(it, ignoreCase = true) } }
        assertEquals("one noun, Space; one verb, Change Space:\n" + offenders.joinToString("\n"), emptyList<Pair<String, String>>(), offenders)
        assertEquals("one label for the one verb", 1, spaceStrings.count { it.second == "Change Space" })
        for (state in listOf("ready", "starting", "running", "stopping", "in_use", "unavailable", "unknown")) {
            assertTrue("a word for $state", spaceStrings.any { it.first == "nova_space_state_$state" })
        }
    }

    @Test
    fun portraitKeepsItsHeaderAndAddsTheSpaceRow() {
        val source = File("src/main/java/com/papi/nova/ui/NovaLibraryActivity.kt").readText()
        val screen = source.substring(source.indexOf("private fun NovaLibraryScreen("), source.indexOf("private fun NovaLibraryHomeHero("))
        val portraitStart = screen.indexOf(".padding(bottom = controllerHintBarBottomPadding)")
        val portrait = screen.substring(portraitStart, screen.indexOf("AnimatedVisibility(", portraitStart))
        assertTrue(portrait.contains("NovaLibraryTopHeader("))
        assertTrue("the Space row follows the header as its own row", portrait.indexOf("NovaEnvironmentBar(") > portrait.indexOf("NovaLibraryTopHeader("))
        assertFalse(
            "portrait used to borrow the landscape strip for the Space row and lost Options and System off the right edge",
            portrait.contains("NovaLibraryLandscapeShowcaseStripContent("),
        )
    }

    @Test
    fun theChooserIsBuiltFromTheDetailWindowRows() {
        val chooser = File("src/main/java/com/papi/nova/ui/NovaSpaceChooser.kt").readText()
        assertTrue(chooser.contains("NovaSteamChoiceRow(") && chooser.contains("NovaControllerHintBar("))
        assertFalse(
            "focus is claimed once when the chooser opens; re-requesting it on every snapshot moved the cursor on poll blips",
            chooser.contains("LaunchedEffect(snapshot"),
        )
        assertTrue(chooser.contains("LaunchedEffect(Unit)"))
    }

    @Test
    fun theParticleFieldBuildsNoGradientsPerFrameAndStopsWhenUnseen() {
        val view = File("src/main/java/com/papi/nova/ui/SpaceParticleView.kt").readText()
        val onDraw = view.substring(view.indexOf("override fun onDraw("), view.indexOf("override fun onAttachedToWindow("))
        assertFalse("gradients are built once and moved with a matrix", onDraw.contains("RadialGradient(") || onDraw.contains("LinearGradient("))
        assertTrue(view.contains("override fun onWindowVisibilityChanged(") && view.contains("fun setCovered("))
        val library = File("src/main/java/com/papi/nova/ui/NovaLibraryActivity.kt").readText()
        assertTrue("the library tells the field when the chooser or the Space screen covers it", library.contains("view.setCovered(chooseSpaceVisible || space != null)"))
    }
}
