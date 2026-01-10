package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.components.NavActionButton
import xyz.ksharma.krail.taj.components.TextField

@Composable
fun SearchTopBar(
    placeholderText: String,
    focusRequester: FocusRequester,
    keyboard: SoftwareKeyboardController?,
    selectionType: StopSelectionType,
    onTypeSelected: (StopSelectionType) -> Unit,
    onBackClick: () -> Unit,
    onTextChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0
    val showRadioGroup = !imeVisible

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
            filter = { input -> input.filter { it.isLetterOrDigit() || it.isWhitespace() } },
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
            onTextChanged(value.toString())
        }

        AnimatedVisibility(
            visible = showRadioGroup,
            enter = fadeIn() + expandHorizontally(expandFrom = Alignment.End),
            exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.End),
        ) {
            StopSelectionRadioGroup(
                selectionType = selectionType,
                onTypeSelected = onTypeSelected,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}
