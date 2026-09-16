package com.papi.nova.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.papi.nova.R
import com.papi.nova.api.PolarisSpaces
import com.papi.nova.ui.compose.LocalNovaComposeColors
import com.papi.nova.ui.compose.LocalNovaLibrarySurfaces
import com.papi.nova.ui.compose.LocalNovaMenuOpacityScale
import com.papi.nova.ui.compose.NovaActionButton
import com.papi.nova.ui.compose.NovaBadge
import com.papi.nova.ui.compose.NovaRadius

/**
 * The device's Space and the one verb for changing it, in a row the library toolbar can host.
 *
 * What it says comes from the host, never from a fallback: a host that cannot offer Spaces
 * reads "Unavailable on the host", and Desktop is named only when the host selected it. The
 * old bar fell back to Desktop whenever nothing was selected, so an unavailable host read as
 * playing in Desktop, with a Change Space button whose rows were all disabled.
 */
@Composable
internal fun NovaEnvironmentBar(
    spaces: PolarisSpaces,
    enabled: Boolean,
    onChoose: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    /** False while the last check failed: the chip says so rather than showing a stale word. */
    statusKnown: Boolean = true,
    /** Its own panel chrome, for a row that is not already inside a strip. */
    framed: Boolean = false,
) {
    val colors = LocalNovaComposeColors.current
    val surfaces = LocalNovaLibrarySurfaces.current
    val fontScale = LocalDensity.current.fontScale.coerceIn(1f, 1.6f)
    val unavailable = NovaSpacesCopy.unavailableReason(spaces)
    val selected = spaces.selected
    val lead = stringResource(if (unavailable != null) R.string.nova_space_bar_spaces else R.string.nova_space_bar_your_space)
    val name = when {
        unavailable != null -> stringResource(R.string.nova_space_bar_unavailable)
        spaces.desktopSelected -> stringResource(R.string.nova_space_desktop)
        selected != null -> selected.name
        else -> stringResource(R.string.nova_space_bar_none)
    }
    val status = when {
        unavailable != null || selected == null -> null
        !statusKnown -> stringResource(R.string.nova_space_state_unknown)
        selected.state == "ready" -> null
        else -> stringResource(NovaSpacesCopy.stateLabel(selected.state))
    }
    val offersChoice = spaces.spaces.size + (if (spaces.desktopAllowed) 1 else 0) > 1
    val change = stringResource(R.string.nova_space_change)
    val shape = RoundedCornerShape(NovaRadius.row)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = if (compact) 48.dp else 54.dp)
            .then(
                if (framed) {
                    Modifier
                        .clip(shape)
                        .background(surfaces.panel.copy(alpha = 0.34f * LocalNovaMenuOpacityScale.current))
                        .border(1.dp, surfaces.tileBorder, shape)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                } else {
                    Modifier
                }
            )
            .testTag("nova-library-environment"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp),
    ) {
        NovaSpaceAvatar(name, Modifier.size((if (compact) 28.dp else 38.dp) * fontScale), decorative = true)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = lead,
                color = colors.textSecondary,
                fontSize = if (compact) 10.sp else 12.sp,
                lineHeight = if (compact) 12.sp else 14.sp,
                maxLines = 1,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = name,
                    color = colors.textPrimary,
                    fontSize = if (compact) 13.sp else 16.sp,
                    lineHeight = if (compact) 16.sp else 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                status?.let { NovaBadge(text = it, modifier = Modifier.testTag("nova-environment-status")) }
            }
        }
        if (offersChoice) {
            NovaActionButton(
                text = change,
                onClick = onChoose,
                enabled = enabled,
                minHeight = 48.dp,
                fontSize = if (compact) 11.sp else 13.sp,
                contentDescription = "$lead: $name. $change.",
                modifier = Modifier.testTag("nova-environment-choose"),
            )
        }
    }
}

/**
 * Initials for a Space name. Decorative beside the name itself, so TalkBack does not read
 * "L R, Living Room".
 */
@Composable
internal fun NovaSpaceAvatar(name: String, modifier: Modifier = Modifier, decorative: Boolean = false) {
    val colors = LocalNovaComposeColors.current
    val person = name.trim().removeSuffix(" Space").removeSuffix("’s").removeSuffix("'s")
    val initials = person.split(Regex("\\s+")).take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")
    Box(
        modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(colors.accent.copy(alpha = .18f))
            .then(if (decorative) Modifier.clearAndSetSemantics { } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials, color = colors.accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
