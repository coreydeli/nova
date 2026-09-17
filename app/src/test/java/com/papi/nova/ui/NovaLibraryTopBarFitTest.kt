package com.papi.nova.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The landscape strip's fit, with widths taken from papi's Retroid Pocket 6 screenshot of
 * 2026-09-16 20:51: density 369, font scale 0.85, an 815 dp strip, so 795 dp inside its padding.
 * Text widths measured there are scaled for other font scales; the avatar scales from 1.0 up.
 */
class NovaLibraryTopBarFitTest {

    /** Text widths in dp at font scale 0.85, from the screenshot. */
    private object Retroid {
        const val HOST_NAME = 49f // "pc-papi.lan"
        const val HOST_STATUS = 53f // "Polaris ready"
        const val COUNT = 34f // "2 shown"
        const val LAYOUT = 23f // "Stage"
        const val CAPTION = 45f // "Your Space"
        const val SPACE_NAME = 65f // "papi - steam"
        const val CHEVRON = 5f // "›"
        const val PLAYING = 31f // "Playing", badge words only
        const val OPTIONS = 32f // "Options"
        const val SYSTEM = 28f // "System"
        const val EYEBROW = 100f // "RESUME YOUR STREAM"
        const val TITLE = 72f // "Animal Well"
        const val RESUME = 62f // "Resume Stream"
        const val END = 53f // "End Session"
        const val STRIP = 815f
    }

    private fun widths(
        fontScale: Float = 0.85f,
        stripWidth: Float = Retroid.STRIP,
        spaces: Boolean = true,
        playing: Boolean = false,
        live: Boolean = false,
    ): NovaTopBarWidths {
        val k = fontScale / 0.85f
        val iconScale = fontScale.coerceIn(1f, 1.6f)
        val largeText = fontScale >= 1.5f
        return NovaTopBarWidths(
            available = stripWidth - 20f,
            gap = 8f,
            slack = 6f,
            hostName = Retroid.HOST_NAME * k,
            hostStatus = Retroid.HOST_STATUS * k,
            identityCap = 168f,
            identityFloor = 56f,
            meta = minOf(132f, (Retroid.COUNT + Retroid.LAYOUT) * k + 6f),
            space = if (!spaces) null else NovaTopBarSpaceWidths(
                chrome = 18f + 28f * iconScale + Retroid.CHEVRON * k + 8f,
                columnGap = 8f,
                caption = Retroid.CAPTION * k,
                name = Retroid.SPACE_NAME * k,
                status = if (playing) Retroid.PLAYING * k + 16f else 0f,
                statusDot = 8f,
                statusGap = 6f,
                cap = 280f * fontScale.coerceAtLeast(1f),
            ),
            continueCard = if (!live) null else NovaTopBarContinueWidths(
                padding = 4f,
                cover = if (largeText) 63f else 49f,
                textMin = minOf(maxOf(Retroid.EYEBROW, Retroid.TITLE) * k, 64f),
                gap = 7f,
                primary = maxOf(88f, Retroid.RESUME * k + 24f),
                secondary = maxOf(72f, Retroid.END * k + 24f),
            ),
            options = Retroid.OPTIONS * k + 24f,
            system = Retroid.SYSTEM * k + 24f,
        )
    }

    private fun fits(widths: NovaTopBarWidths, fit: NovaTopBarFit) {
        assertEquals("the row fits without scrolling", 0f, novaTopBarOverflow(widths, fit), 0.01f)
    }

    @Test
    fun theRetroidStageLibraryKeepsEverythingIdleAndWhilePlaying() {
        for (playing in listOf(false, true)) {
            val widths = widths(playing = playing)
            val fit = novaLibraryTopBarFit(widths)
            assertEquals("nothing is left out on the Retroid in Stage (playing=$playing)", NovaTopBarFit(), fit)
            fits(widths, fit)
        }
    }

    @Test
    fun aLiveGridOrCompactLibraryFitsOnTheRetroidWithResumeAndEndSession() {
        // The old strip forced this row to 980 dp inside an 815 dp strip and scrolled Options and
        // System 165 dp past the right edge. Measured, it needs about 758 dp.
        val widths = widths(playing = true, live = true)
        val fit = novaLibraryTopBarFit(widths)
        assertEquals(NovaTopBarFit(), fit)
        fits(widths, fit)
    }

    @Test
    fun atDefaultTextTheCountAndLayoutGoFirstAndNothingElse() {
        val widths = widths(fontScale = 1f, playing = true, live = true)
        val fit = novaLibraryTopBarFit(widths)
        assertFalse(fit.showMeta)
        assertEquals("only the count and layout give way", NovaTopBarFit(showMeta = false), fit)
        fits(widths, fit)
    }

    @Test
    fun largeTextGivesWayInTheDocumentedOrderAndStopsOnceItFits() {
        val widths = widths(fontScale = 1.5f, playing = true, live = true)
        val fit = novaLibraryTopBarFit(widths)
        assertEquals(
            NovaTopBarFit(
                showMeta = false,
                showSpaceCaption = false,
                showHostStatus = false,
                showContinueCover = false,
                showContinueText = false,
                compactSpaceStatus = true,
            ),
            fit,
        )
        assertTrue("the Space name is still whole", fit.showSpaceName && fit.spaceNameMax.isInfinite())
        fits(widths, fit)
    }

    @Test
    fun theBadgeLosesItsWordsBeforeTheSpaceNameEllipsizes() {
        val widths = widths(fontScale = 1.5f, stripWidth = 700f, playing = true, live = true)
        val fit = novaLibraryTopBarFit(widths)
        assertTrue(fit.compactSpaceStatus)
        assertTrue("the name ellipsizes but stays", fit.showSpaceName && fit.spaceNameMax.isFinite())
        assertTrue("the control gets the exact width its name was cut to", fit.spaceWidth.isFinite())
        assertTrue("the host name is untouched until the Space name has given what it can", fit.identityMax.isInfinite())
        assertTrue(fit.showContinueSecondary)
        fits(widths, fit)
    }

    @Test
    fun whenTheNameIsNotEnoughItGoesAndTheHostNameEllipsizesToItsFloor() {
        val widths = widths(fontScale = 1.5f, stripWidth = 640f, playing = true, live = true)
        val fit = novaLibraryTopBarFit(widths)
        assertFalse("avatar, status dot and chevron remain", fit.showSpaceName)
        assertTrue(fit.identityMax.isFinite() && fit.identityMax >= 56f)
        assertTrue("End Session stays while anything else could give", fit.showContinueSecondary)
        fits(widths, fit)
    }

    @Test
    fun endSessionIsTheLastThingToGo() {
        val widths = widths(fontScale = 1.5f, stripWidth = 560f, playing = true, live = true)
        val fit = novaLibraryTopBarFit(widths)
        assertFalse(fit.showSpaceName)
        assertEquals(56f, fit.identityMax, 0.01f)
        assertFalse(fit.showContinueSecondary)
        fits(widths, fit)
    }

    @Test
    fun aHostWithoutSpacesAndATabletKeepEverything() {
        for (case in listOf(widths(fontScale = 1f, spaces = false, live = true), widths(fontScale = 1f, stripWidth = 1280f, playing = true, live = true))) {
            val fit = novaLibraryTopBarFit(case)
            assertEquals(NovaTopBarFit(), fit)
            fits(case, fit)
        }
    }

    @Test
    fun optionsAndSystemAreNeverPartOfWhatGivesWay() {
        // Every width the fit can change is on the host side or in the Space control; the menus are
        // measured once and counted whole in every state.
        for (strip in listOf(560f, 640f, 700f, 815f, 1280f)) {
            for (scale in listOf(0.85f, 1f, 1.5f)) {
                val widths = widths(fontScale = scale, stripWidth = strip, playing = true, live = true)
                val fit = novaLibraryTopBarFit(widths)
                val right = listOfNotNull(
                    widths.meta.takeIf { fit.showMeta },
                    widths.space?.let { novaTopBarSpaceWidth(it, fit) },
                    widths.options,
                    widths.system,
                )
                val required = novaTopBarIdentityWidth(widths, fit) + widths.gap +
                    novaTopBarContinueWidth(widths.continueCard!!, fit) + widths.gap +
                    right.sum() + widths.gap * (right.size - 1) + widths.slack
                assertEquals(required, novaTopBarRequiredWidth(widths, fit), 0.01f)
                fits(widths, fit)
            }
        }
    }

    @Test
    fun theContinueCardIsMeasuredByTheRulesItDrawsWith() {
        fun hero(secondaryLabel: String?, secondary: NovaLibraryHeroSecondaryAction?) = NovaLibraryHeroState(
            game = null,
            title = "Animal Well",
            subtitle = "",
            caption = "",
            eyebrow = "Resume your stream",
            actionLabel = "Resume Stream",
            badges = emptyList(),
            reason = NovaLibraryHeroReason.ACTIVE_SESSION,
            primaryAction = NovaLibraryHeroPrimaryAction.RESUME,
            supportingLine = "",
            artworkFallbackTitle = "",
            artworkFallbackSubtitle = "",
            secondaryActionLabel = secondaryLabel,
            secondaryAction = secondary,
        )
        val owned = hero("End Session", NovaLibraryHeroSecondaryAction.END_SESSION).topBarContinue()
        assertEquals("End Session", owned.secondaryActionLabel)
        assertFalse("no game, no cover to measure", owned.hasCover)
        assertEquals(
            "a label without an action is never drawn, so it is never measured",
            null,
            hero("End Session", null).topBarContinue().secondaryActionLabel,
        )
    }

    @Test
    fun spaceInitialsSkipWordsWithoutALetter() {
        assertEquals("PS", novaSpaceInitials("papi - steam"))
        assertEquals("LR", novaSpaceInitials("Living Room"))
        assertEquals("A", novaSpaceInitials("Alex's Space"))
        assertEquals("2F", novaSpaceInitials("2nd Floor"))
        assertEquals("", novaSpaceInitials("  "))
    }
}
