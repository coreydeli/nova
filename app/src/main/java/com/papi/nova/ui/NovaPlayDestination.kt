package com.papi.nova.ui

import androidx.annotation.StringRes
import com.papi.nova.R
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

/**
 * Which entry plays a title on each side of Play Setup, and what each card says.
 *
 * A Steam game is the same game wherever its Steam app id matches. Steam Big Picture has no app
 * id, so it pairs by what it is: a Space's big-picture-v1 entry on one side, and the desktop entry
 * the host marks steam_big_picture on the other. A host that does not send that mark leaves the
 * desktop's Big Picture unpaired, as it always was.
 */
internal object NovaPlayDestinationMatch {
    const val SPACE_BIG_PICTURE_TARGET = "big-picture-v1"
    const val DESKTOP_ID = "desktop"

    fun isBigPicture(game: PolarisGame): Boolean =
        game.space?.target == SPACE_BIG_PICTURE_TARGET || (game.space == null && game.steamBigPicture)

    /** Whether finding a Space game on Desktop needs the Desktop library at all. */
    fun needsDesktopLibrary(game: PolarisGame): Boolean =
        game.space != null && (game.steamAppid.isNotEmpty() || isBigPicture(game))

    /** The Desktop library entry that plays [game]: itself when it is a Desktop game, else its pair or null. */
    fun desktopTitle(game: PolarisGame, desktopGames: List<PolarisGame>): PolarisGame? = when {
        game.space == null -> game
        isBigPicture(game) -> desktopGames.firstOrNull { it.space == null && it.steamBigPicture }
        game.steamAppid.isNotEmpty() -> desktopGames.firstOrNull { it.steamAppid == game.steamAppid }
        else -> null
    }

    /** Whether finding [game] in another Space needs that Space's library at all. */
    fun needsSpaceLibrary(game: PolarisGame): Boolean = game.steamAppid.isNotEmpty() || isBigPicture(game)

    /** The entry in a Space's [library] that plays [game], or null. */
    fun spaceTitle(game: PolarisGame, library: List<PolarisGame>?): PolarisGame? = library?.firstOrNull {
        if (isBigPicture(game)) it.space?.target == SPACE_BIG_PICTURE_TARGET
        else game.steamAppid.isNotEmpty() && it.steamAppid == game.steamAppid
    }

    /** That Space's Steam, offered when a Steam game is not installed there yet. Never for Big Picture itself. */
    fun spaceSteam(game: PolarisGame, library: List<PolarisGame>?, title: PolarisGame?): PolarisGame? =
        if (title != null || isBigPicture(game)) null
        else library?.firstOrNull { it.space?.target == SPACE_BIG_PICTURE_TARGET }

    /**
     * The caption a card shows, as a string resource that takes the card's name as its one
     * argument when it has a placeholder. A card nobody can choose says why and where to go
     * instead, rather than describing a choice it does not offer.
     */
    @StringRes
    fun caption(choice: NovaPlayDestination): Int = when {
        choice.id == DESKTOP_ID ->
            if (choice.game == null) R.string.nova_space_option_desktop_missing else R.string.nova_space_option_desktop
        !choice.libraryEnabled -> R.string.nova_space_option_no_library
        choice.game == null -> R.string.nova_space_option_missing
        !choice.canOpen -> NovaSpacesCopy.openBlockedReason(choice.state, choice.canOpen, choice.blockedReason)
            ?: R.string.nova_space_blocked_generic
        choice.viaSteam -> R.string.nova_space_option_via_steam
        else -> R.string.nova_space_option_use
    }
}
