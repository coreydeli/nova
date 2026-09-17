package com.papi.nova.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.papi.nova.ui.compose.NovaActionSurface
import com.papi.nova.ui.compose.NovaBadge
import com.papi.nova.ui.compose.NovaRadius

/**
 * The device's Space as one control: avatar, caption, name and a chevron in a single surface
 * that opens the chooser.
 *
 * It used to be a label that filled a fixed 300 dp slot with its own Change Space button at
 * the far end, so in the landscape strip the button floated alone in the middle of the bar,
 * away from the name it changes. Now the whole row is the action, sized to its content.
 *
 * It stays a control when this device has nowhere else to go. Drawn as plain text it could not
 * be pressed and said nothing about why: a newly paired device with a Space but no Desktop
 * Access simply could not switch (papi, 2026-09-16 21:27). The chooser it opens names the
 * reason, for example that Desktop Access is off and where to turn it on.
 *
 * What it says comes from the host, never from a fallback (see [NovaSpacesCopy.environmentLabel]):
 * an unavailable host reads "Unavailable on the host", Desktop is named only when the host
 * selected it, and Desktop is never called a Space.
 *
 * In the landscape strip the control sits in the right-hand cluster beside Options and System,
 * and [novaLibraryTopBarFit] may ask it to drop its caption, show its status as a dot, or leave
 * out the name. TalkBack still reads every word.
 */
@Composable
internal fun NovaEnvironmentBar(
    spaces: PolarisSpaces,
    enabled: Boolean,
    onChoose: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    /** False while the last check failed: the control says so rather than showing a stale word. */
    statusKnown: Boolean = true,
    /** Full width with its own panel chrome, for a row that is not already inside a strip. */
    framed: Boolean = false,
    /** A Space change is on the wire: the caption says so and the control waits for the answer. */
    changing: Boolean = false,
    /** The caption line; the strip leaves it out first when the row is tight. */
    showCaption: Boolean = true,
    /** The status as a dot instead of a badge with words. */
    compactStatus: Boolean = false,
    /** The name itself; left out only when nothing else was enough, leaving avatar and chevron. */
    showName: Boolean = true,
) {
    val colors = LocalNovaComposeColors.current
    val fontScale = LocalDensity.current.fontScale.coerceIn(1f, 1.6f)
    val strings = rememberNovaEnvironmentStrings(spaces, statusKnown, changing)
    val caption = strings.caption
    val name = strings.name
    val status = strings.status
    val spoken = listOfNotNull("$caption: $name", status).joinToString(", ")
    val change = stringResource(R.string.nova_space_change)
    val minHeight = if (compact) 48.dp else 54.dp
    Box(modifier.testTag("nova-library-environment")) {
        val active = enabled && !changing
        NovaActionSurface(
            onClick = onChoose,
            enabled = active,
            contentDescription = "$spoken. $change.",
            minHeight = minHeight,
            // Beside Options and System in the strip it wears their shape; a full-width row
            // keeps the row radius every selectable row uses.
            cornerRadius = if (framed) NovaRadius.row else NovaRadius.hero,
            contentPadding = PaddingValues(start = 8.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
            contentAlignment = Alignment.CenterStart,
            modifier = (if (framed) Modifier.fillMaxWidth() else Modifier).testTag("nova-environment-choose"),
        ) { contentColor, focused ->
            NovaEnvironmentLabelRow(
                caption = caption,
                name = name,
                status = status,
                compact = compact,
                fontScale = fontScale,
                nameColor = contentColor,
                captionColor = if (active) colors.textSecondary else colors.textMuted,
                chevronColor = when {
                    !active -> colors.textMuted
                    focused -> colors.accent
                    else -> colors.textSecondary
                },
                fill = framed,
                showCaption = showCaption,
                compactStatus = compactStatus,
                showName = showName,
            )
        }
    }
}

/** The control's words as the host gave them, shared by the control and the strip that measures it. */
internal data class NovaEnvironmentStrings(
    val caption: String,
    val name: String,
    val status: String?,
)

@Composable
internal fun rememberNovaEnvironmentStrings(
    spaces: PolarisSpaces,
    statusKnown: Boolean,
    changing: Boolean,
): NovaEnvironmentStrings {
    val label = NovaSpacesCopy.environmentLabel(spaces, statusKnown, changing)
    return NovaEnvironmentStrings(
        caption = stringResource(label.caption),
        name = label.nameRes?.let { stringResource(it) } ?: label.spaceName.orEmpty(),
        status = label.status?.let { stringResource(it) },
    )
}

/** Avatar, caption over name, status and, when the row is an action, the chevron that says so. */
@Composable
private fun NovaEnvironmentLabelRow(
    caption: String,
    name: String,
    status: String?,
    compact: Boolean,
    fontScale: Float,
    nameColor: Color,
    captionColor: Color,
    chevronColor: Color?,
    fill: Boolean,
    showCaption: Boolean,
    compactStatus: Boolean,
    showName: Boolean,
) {
    val colors = LocalNovaComposeColors.current
    Row(
        modifier = if (fill) Modifier.fillMaxWidth() else Modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp),
    ) {
        NovaSpaceAvatar(name, Modifier.size((if (compact) 28.dp else 34.dp) * fontScale), decorative = true)
        if (showCaption || showName || status != null) Column(
            Modifier.weight(1f, fill = fill),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            if (showCaption) {
                Text(
                    text = caption,
                    color = captionColor,
                    fontSize = if (compact) 10.sp else 12.sp,
                    lineHeight = if (compact) 12.sp else 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (showName) {
                    Text(
                        text = name,
                        color = nameColor,
                        fontSize = if (compact) 13.sp else 16.sp,
                        lineHeight = if (compact) 16.sp else 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
                status?.let {
                    if (compactStatus) {
                        // The words go before the name does; the dot keeps the state visible and
                        // TalkBack reads it from the control's description.
                        Spacer(
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(colors.accent)
                                .testTag("nova-environment-status-dot"),
                        )
                    } else {
                        NovaBadge(text = it, modifier = Modifier.testTag("nova-environment-status"))
                    }
                }
            }
        }
        chevronColor?.let {
            Text(
                text = "›",
                color = it,
                fontSize = if (compact) 20.sp else 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clearAndSetSemantics { },
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
    val initials = novaSpaceInitials(name)
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

/**
 * Up to two initials from the words of a Space name. Words without a letter or digit are skipped,
 * so "papi - steam" is "PS", not "P-".
 */
internal fun novaSpaceInitials(name: String): String {
    val person = name.trim().removeSuffix(" Space").removeSuffix("’s").removeSuffix("'s")
    return person.split(Regex("\\s+"))
        .mapNotNull { word -> word.firstOrNull { it.isLetterOrDigit() }?.uppercase() }
        .take(2)
        .joinToString("")
}
