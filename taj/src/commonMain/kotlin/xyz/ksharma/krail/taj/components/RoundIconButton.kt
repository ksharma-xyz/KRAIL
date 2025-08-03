package xyz.ksharma.krail.taj.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.taj.LocalContainerColor
import xyz.ksharma.krail.taj.LocalContentColor
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.tokens.BadgeTokens
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
    showBadge: Boolean = false,
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
                .size(RoundButtonSize)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(color = color ?: LocalContainerColor.current)
                    .klickable(onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                content()
            }

            if (showBadge) {
                Box(
                    modifier = Modifier
                        .size(BadgeTokens.BadgeSize)
                        .align(Alignment.TopEnd)
                        .offset(x = BadgeTokens.BadgeOffsetX, y = BadgeTokens.BadgeOffsetY)
                        .clip(CircleShape)
                        .background(KrailTheme.colors.badge)
                )
            }
        }
    }
}

// region Previews

@Preview
@Composable
private fun PreviewRoundIconButton() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        RoundIconButton(
            onClick = { }
        ) {
            Image(
                imageVector = Icons.Default.Add,
                contentDescription = "Add"
            )
        }
    }
}

@Preview
@Composable
private fun PreviewRoundIconButtonWithBadge() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        RoundIconButton(
            showBadge = true,
            onClick = { }
        ) {
            Image(
                imageVector = Icons.Default.Add,
                contentDescription = "Add"
            )
        }
    }
}

// endregion
