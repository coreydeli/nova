package com.papi.nova.ui

import android.content.Context
import com.papi.nova.shared.polaris.model.PolarisGame

/**
 * Remembers, per game, whether the face buttons are sent by label or by position.
 *
 * Polaris' virtual Switch Pro maps A to the east button by name, so an Xbox-layout pad
 * confirms with its south button in a Switch game. A player who wants the positions to
 * match flips it here for the games that need it instead of for every game in Settings.
 */
object NovaFaceButtonLayoutOverrides {
    const val LABELS = "labels"
    const val POSITIONS = "positions"

    private const val PREFS_NAME = "nova_prefs"
    private const val KEY_PREFIX = "face_button_layout_override_"

    private fun key(game: PolarisGame): String =
        KEY_PREFIX + game.id.ifBlank { game.appId.toString() }

    fun normalize(raw: String?): String? = when (raw?.trim()?.lowercase()) {
        LABELS -> LABELS
        POSITIONS -> POSITIONS
        else -> null
    }

    fun load(context: Context, game: PolarisGame): String? {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = preferences.getString(key(game), null) ?: return null
        val normalized = normalize(raw)
        if (normalized == null) {
            preferences.edit().remove(key(game)).apply()
            return null
        }
        if (normalized != raw) {
            preferences.edit().putString(key(game), normalized).apply()
        }
        return normalized
    }

    fun save(context: Context, game: PolarisGame, layout: String) {
        val normalized = normalize(layout)
        if (normalized == null) {
            clear(context, game)
            return
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(key(game), normalized)
            .apply()
    }

    /** Remove the per-game choice so this game follows the Settings flip again. */
    fun clear(context: Context, game: PolarisGame) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(key(game))
            .apply()
    }

    /** Whether this launch swaps A/B and X/Y: the per-game choice when set, else the Settings flip. */
    fun flipFaceButtons(layout: String?, globalFlip: Boolean): Boolean = when (normalize(layout)) {
        POSITIONS -> true
        LABELS -> false
        else -> globalFlip
    }
}
