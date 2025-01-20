package xyz.ksharma.krail.taj.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import xyz.ksharma.krail.taj.LocalContainerColor
import xyz.ksharma.krail.taj.LocalContentColor
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.tokens.ButtonTokens.RoundButtonSize

/**
 * A round icon button with customizable content and colors.
 *
 * @param modifier Modifier to be applied to the button.
 * @param color Background color of the button. Defaults to the theme's secondary container color.
 * @param onClickLabel Semantic / accessibility label for the [onClick] action.
 * @param content Composable content to be displayed inside the button.
 * @param onClick Lambda to be invoked when the button is clicked.
 */
@Composable
fun RoundIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color? = null,
    onClickLabel: String? = null,
    content: @Composable () -> Unit = {},
) {
    CompositionLocalProvider(
        LocalContainerColor provides KrailTheme.colors.surface,
        LocalContentColor provides KrailTheme.colors.onSurface,
    ) {
        Box(
            modifier = modifier
                .size(RoundButtonSize) // TODO - token "SearchButtonHeight"
                .clip(CircleShape)
                .background(color = color ?: LocalContainerColor.current)
                .clickable(
                    role = Role.Button,
                    onClickLabel = onClickLabel,
                ) { onClick() },
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

// region Previews

// endregion
