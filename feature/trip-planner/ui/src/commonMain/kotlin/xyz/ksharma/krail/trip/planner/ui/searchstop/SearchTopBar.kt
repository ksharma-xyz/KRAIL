package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.components.NavActionButton
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TextField
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.searchstop.map.MapToggleButton
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode

/**
 * @param animateMapButton Gates when the map toggle button becomes visible.
 *
 * - `null`  — permission check not yet done; button stays hidden.
 * - `true`  — permission not yet requested; show button (discovery hint).
 * - `false` — permission granted or denied; show button immediately.
 *
 * Timing (e.g. a delay before setting this from null) is the caller's responsibility.
 */
@Composable
fun SearchTopBar(
    placeholderText: String,
    focusRequester: FocusRequester,
    keyboard: SoftwareKeyboardController?,
    onMapToggle: (Boolean) -> Unit,
    onBackClick: () -> Unit,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    initialText: String = "",
    isMapAvailable: Boolean = false,
    isMapSelected: Boolean = false,
    animateMapButton: Boolean? = null,
) {
    // rememberSaveable survives rotation so the slide-in plays only on first appearance,
    // not again after every configuration change.
    var showMapToggleButton by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(isMapAvailable, animateMapButton) {
        if (!isMapAvailable) {
            showMapToggleButton = false
        } else if (animateMapButton != null && !showMapToggleButton) {
            // Only flip to true once; after that rememberSaveable keeps it true across rotations.
            showMapToggleButton = true
        }
    }

    Row(
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .windowInsetsPadding(WindowInsets.ime)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .padding(bottom = 8.dp)
            .background(color = Color.Transparent, RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)),
    ) {
        TextField(
            placeholder = placeholderText,
            initialText = initialText.ifEmpty { null },
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            maxLength = 30,
            filter = { input ->
                input.filter { it.isLetterOrDigit() || it.isWhitespace() || it == ',' }
            },
            leadingIcon = {
                NavActionButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    iconContentDescription = "Back",
                    onClick = {
                        keyboard?.hide()
                        focusRequester.freeFocus()
                        onBackClick()
                    },
                )
            },
        ) { value ->
            onTextChange(value.toString())
        }

        // animateContentSize is only applied when animateMapButton == true (NotDetermined).
        // For false (Granted/Denied) the button appears instantly with no animation.
        // height(48.dp) locks height so only width animates, preventing the button from
        // drifting up from the bottom-right when both dimensions would otherwise animate.
        Box(
            modifier = Modifier
                .height(48.dp)
                .then(
                    if (animateMapButton == true) {
                        Modifier.animateContentSize(
                            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                        )
                    } else {
                        Modifier
                    },
                ),
        ) {
            if (showMapToggleButton) {
                MapToggleButton(
                    selected = isMapSelected,
                    onClick = { onMapToggle(!isMapSelected) },
                    modifier = Modifier.padding(start = 12.dp),
                ) {
                    Text("Map")
                }
            }
        }
    }
}

// region Previews

@Preview(name = "SearchTopBar - List Selected")
@Composable
private fun PreviewSearchTopBar_List() {
    PreviewTheme {
        val themeColor = remember { mutableStateOf(TransportMode.Bus().colorCode) }
        CompositionLocalProvider(LocalThemeColor provides themeColor) {
            val focusRequester = remember { FocusRequester() }
            SearchTopBar(
                placeholderText = "Station",
                focusRequester = focusRequester,
                keyboard = null,
                isMapSelected = false,
                isMapAvailable = true,
                animateMapButton = false,
                onMapToggle = {},
                onBackClick = {},
                onTextChange = {},
                modifier = Modifier.height(72.dp),
            )
        }
    }
}

@Preview(name = "SearchTopBar - Map Selected")
@Composable
private fun PreviewSearchTopBar_Map() {
    PreviewTheme {
        val themeColor = remember { mutableStateOf(TransportMode.Metro().colorCode) }
        CompositionLocalProvider(LocalThemeColor provides themeColor) {
            val focusRequester = remember { FocusRequester() }
            SearchTopBar(
                placeholderText = "Station",
                focusRequester = focusRequester,
                keyboard = null,
                isMapSelected = true,
                isMapAvailable = true,
                animateMapButton = false,
                onMapToggle = {},
                onBackClick = {},
                onTextChange = {},
                modifier = Modifier.height(72.dp),
            )
        }
    }
}

@Preview(name = "SearchTopBar - Compact (simulate IME)")
@Composable
private fun PreviewSearchTopBar_Compact() {
    PreviewTheme {
        val themeColor = remember { mutableStateOf(TransportMode.Train().colorCode) }
        CompositionLocalProvider(LocalThemeColor provides themeColor) {
            val focusRequester = remember { FocusRequester() }
            Box(modifier = Modifier.width(360.dp).height(72.dp)) {
                SearchTopBar(
                    placeholderText = "Station",
                    focusRequester = focusRequester,
                    keyboard = null,
                    isMapSelected = false,
                    isMapAvailable = true,
                    animateMapButton = false,
                    onMapToggle = {},
                    onBackClick = {},
                    onTextChange = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

// endregion
