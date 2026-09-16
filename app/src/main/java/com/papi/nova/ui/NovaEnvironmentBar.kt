package com.papi.nova.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.papi.nova.R
import com.papi.nova.api.PolarisSpaces
import com.papi.nova.ui.compose.LocalNovaComposeColors
import com.papi.nova.ui.compose.LocalNovaLibrarySurfaces
import com.papi.nova.ui.compose.LocalNovaMenuOpacityScale
import com.papi.nova.ui.compose.NovaActionSurface
import com.papi.nova.ui.compose.NovaBadge
import com.papi.nova.ui.compose.NovaRadius

/**
 * The device's Space as one control: avatar, caption, name and a chevron in a single surface
 * that opens the chooser.
 *
 * It used to be a label that filled a fixed 300 dp slot with its own Change Space button at
 * the far end, so in the landscape strip the button floated alone in the middle of the bar,
 * away from the name it changes. Now the whole row is the action, sized to its content, and
 * when there is nothing to choose it is plain text with no chevron and nothing to focus.
 *
 * What it says comes from the host, never from a fallback (see [NovaSpacesCopy.environmentLabel]):
 * an unavailable host reads "Unavailable on the host", Desktop is named only when the host
 * selected it, and Desktop is never called a Space.
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
) {
    val colors = LocalNovaComposeColors.current
    val surfaces = LocalNovaLibrarySurfaces.current
    val fontScale = LocalDensity.current.fontScale.coerceIn(1f, 1.6f)
    val label = NovaSpacesCopy.environmentLabel(spaces, statusKnown, changing)
    val caption = stringResource(label.caption)
    val name = label.nameRes?.let { stringResource(it) } ?: label.spaceName.orEmpty()
    val status = label.status?.let { stringResource(it) }
    val spoken = listOfNotNull("$caption: $name", status).joinToString(", ")
    val change = stringResource(R.string.nova_space_change)
    val minHeight = if (compact) 48.dp else 54.dp
    Box(modifier.testTag("nova-library-environment")) {
        if (label.offersChoice) {
            val active = enabled && !changing
            NovaActionSurface(
                onClick = onChoose,
                enabled = active,
                contentDescription = "$spoken. $change.",
                minHeight = minHeight,
                cornerRadius = NovaRadius.row,
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
                )
            }
        } else {
            val shape = RoundedCornerShape(NovaRadius.row)
            Box(
                modifier = (
                    if (framed) {
                        Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .background(surfaces.panel.copy(alpha = 0.34f * LocalNovaMenuOpacityScale.current))
                            .border(1.dp, surfaces.tileBorder, shape)
                            .padding(horizontal = 10.dp)
                    } else {
                        Modifier.padding(horizontal = 2.dp)
                    }
                    )
                    .heightIn(min = minHeight)
                    .semantics(mergeDescendants = true) { contentDescription = spoken },
                contentAlignment = Alignment.CenterStart,
            ) {
                NovaEnvironmentLabelRow(
                    caption = caption,
                    name = name,
                    status = status,
                    compact = compact,
                    fontScale = fontScale,
                    nameColor = colors.textPrimary,
                    captionColor = colors.textSecondary,
                    chevronColor = null,
                    fill = framed,
                )
            }
        }
    }
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
) {
    Row(
        modifier = if (fill) Modifier.fillMaxWidth() else Modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp),
    ) {
        NovaSpaceAvatar(name, Modifier.size((if (compact) 28.dp else 34.dp) * fontScale), decorative = true)
        Column(Modifier.weight(1f, fill = fill), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = caption,
                color = captionColor,
                fontSize = if (compact) 10.sp else 12.sp,
                lineHeight = if (compact) 12.sp else 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                status?.let { NovaBadge(text = it, modifier = Modifier.testTag("nova-environment-status")) }
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
