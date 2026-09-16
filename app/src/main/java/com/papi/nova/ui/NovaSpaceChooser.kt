package com.papi.nova.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.papi.nova.api.PolarisSpaces
import com.papi.nova.ui.compose.LocalNovaComposeColors
import com.papi.nova.ui.compose.NOVA_FIRST_FOCUS_SETTLE_MS
import com.papi.nova.ui.compose.NovaActionButton
import kotlinx.coroutines.delay

internal fun spaceStateLabel(state: String): String = when (state) {
    "ready" -> "Ready To Play"
    "starting" -> "Starting"
    "running" -> "Your Stream Is Running"
    "stopping" -> "Stopping"
    "in_use" -> "In Use"
    else -> "Unavailable"
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun NovaSpaceChooser(snapshot: PolarisSpaces, busy: Boolean, error: String?,
    onChoose: (String) -> Unit, onBack: () -> Unit) {
    val colors = LocalNovaComposeColors.current
    val focus = remember { FocusRequester() }
    val backFocus = remember { FocusRequester() }
    val input = LocalInputModeManager.current
    LaunchedEffect(snapshot.selectedId, busy, snapshot.canSwitch) {
        delay(NOVA_FIRST_FOCUS_SETTLE_MS)
        input.requestInputMode(InputMode.Keyboard)
        runCatching { (if (busy || !snapshot.canSwitch) backFocus else focus).requestFocus() }
    }
    Box(Modifier.fillMaxSize().background(colors.window).windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = androidx.compose.ui.Alignment.TopCenter) {
    Column(Modifier.widthIn(max = 720.dp).fillMaxWidth()
        .verticalScroll(rememberScrollState()).padding(24.dp).testTag("nova-space-chooser"),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Change Space", color = colors.textPrimary, fontSize = 28.sp)
        Text("Choose your Space to browse its games and saves. Each Space keeps its own Steam sign-in.",
            color = colors.textSecondary, fontSize = 16.sp)
        Text("The Space name identifies your gaming environment. Check or change the Steam account inside Steam Big Picture.",
            color = colors.textSecondary, fontSize = 14.sp)
        if (!snapshot.canSwitch) Text("Save your game and end your stream before switching Spaces.", color = colors.textPrimary)
        error?.let { Text(it, color = colors.textPrimary) }
        if (snapshot.desktopAllowed) {
            NovaActionButton(text = "Desktop", onClick = { onChoose("desktop") },
                selected = snapshot.selectedId == "desktop", enabled = !busy && snapshot.canSwitch, minHeight = 52.dp,
                modifier = (if (snapshot.selectedId == "desktop") Modifier.focusRequester(focus) else Modifier).fillMaxWidth()
                    .testTag("nova-space-choice-desktop"))
            Text("Your computer’s usual games and desktop.", color = colors.textSecondary, fontSize = 14.sp)
        }
        snapshot.spaces.forEach { space ->
            val bringIntoView = remember(space.id) { BringIntoViewRequester() }
            var rowFocused by remember(space.id) { mutableStateOf(false) }
            LaunchedEffect(rowFocused) {
                if (rowFocused) bringIntoView.bringIntoView()
            }
            Row(modifier = Modifier.fillMaxWidth().bringIntoViewRequester(bringIntoView)
                .onFocusChanged { rowFocused = it.hasFocus },
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                NovaSpaceAvatar(space.name)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                NovaActionButton(text = space.name, onClick = { onChoose(space.id) }, selected = space.selected,
                    enabled = !busy && snapshot.canSwitch, minHeight = 52.dp,
                    modifier = (if (space.selected) Modifier.focusRequester(focus) else Modifier).fillMaxWidth()
                        .testTag("nova-space-choice-${space.id}"))
                Text(listOfNotNull(if (space.selected) "Current Space" else null, spaceStateLabel(space.state)).joinToString(" · "),
                    color = colors.textSecondary, fontSize = 14.sp)
            }
        }
        }
        NovaActionButton(text = "Back", onClick = onBack, minHeight = 52.dp,
            modifier = Modifier.focusRequester(backFocus).testTag("nova-space-chooser-back"))
    }
    }
}
