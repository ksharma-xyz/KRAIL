package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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

@Composable
fun SearchTopBar(
    placeholderText: String,
    focusRequester: FocusRequester,
    keyboard: SoftwareKeyboardController?,
    onMapToggle: (Boolean) -> Unit,
    onBackClick: () -> Unit,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isMapAvailable: Boolean = false,
    isMapSelected: Boolean = false,
) {
    Row(
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .windowInsetsPadding(WindowInsets.ime)
            .padding(vertical = 12.dp)
            .padding(horizontal = 16.dp)
            .animateContentSize(),
    ) {
        TextField(
            placeholder = placeholderText,
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

        if (isMapAvailable) {
            // Reserve the TextField height for the radio group so hiding it doesn't change layout height.
            MapToggleButton(
                selected = isMapSelected,
                onClick = {
                    onMapToggle(!isMapSelected)
                },
                modifier = Modifier
                    .padding(start = 12.dp)
                    .requiredHeight(48.dp),
            ) {
                Text("Map")
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
            // Do not add external horizontal padding here
            SearchTopBar(
                placeholderText = "Station",
                focusRequester = focusRequester,
                keyboard = null,
                isMapSelected = false,
                isMapAvailable = true,
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
            // Use a fixed width container to simulate a compact layout instead of adding horizontal padding
            Box(modifier = Modifier.width(360.dp).height(72.dp)) {
                SearchTopBar(
                    placeholderText = "Station",
                    focusRequester = focusRequester,
                    keyboard = null,
                    isMapSelected = false,
                    isMapAvailable = true,
                    onMapToggle = {},
                    onBackClick = {},
                    onTextChange = {},
                    modifier = Modifier.fillMaxSize(), // let the bar use its internal padding
                )
            }
        }
    }
}

// endregion
