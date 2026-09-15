package com.papi.nova.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.papi.nova.api.PolarisSpaces
import com.papi.nova.ui.compose.LocalNovaComposeColors
import com.papi.nova.ui.compose.NovaActionButton

/** Environment identity stays above the same library on handhelds and TVs. */
@Composable
internal fun NovaEnvironmentBar(spaces: PolarisSpaces, enabled: Boolean, onChoose: () -> Unit,
    modifier: Modifier = Modifier) {
    val colors = LocalNovaComposeColors.current
    val name = spaces.selected?.name ?: "Desktop"
    Row(modifier.fillMaxWidth().heightIn(min = 54.dp).testTag("nova-library-environment"),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        NovaSpaceAvatar(name)
        Text("Playing In", color = colors.textSecondary, fontSize = 14.sp)
        Text(name, color = colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        if (spaces.spaces.size + (if (spaces.desktopAllowed) 1 else 0) > 1) {
            NovaActionButton(text = "Change Space", onClick = onChoose, enabled = enabled, minHeight = 48.dp,
                contentDescription = "Playing In $name. Change Space.",
                modifier = Modifier.testTag("nova-environment-choose"))
        }
    }
}

@Composable
internal fun NovaSpaceAvatar(name: String, modifier: Modifier = Modifier) {
    val colors = LocalNovaComposeColors.current
    val person = name.trim().removeSuffix(" Space").removeSuffix("’s").removeSuffix("'s")
    val initials = person.split(Regex("\\s+")).take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")
    Box(modifier.size(38.dp).clip(CircleShape).background(colors.accent.copy(alpha = .18f)),
        contentAlignment = Alignment.Center) {
        Text(initials, color = colors.accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
