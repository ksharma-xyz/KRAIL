package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.LocalContentAlpha
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.modifier.scalingKlickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.themeContentColor
import xyz.ksharma.krail.taj.tokens.ContentAlphaTokens.DisabledContentAlpha
import xyz.ksharma.krail.taj.tokens.ContentAlphaTokens.EnabledContentAlpha

private val SmallRadioButtonHeight = 32.dp // no token equivalent

@Composable
fun OutlineRadioButton(
    text: String,
    themeColor: Color,
    modifier: Modifier = Modifier,
    type: RadioButtonType = RadioButtonType.DEFAULT,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
) {
    val contentAlphaProvider =
        rememberSaveable(enabled) { if (enabled) EnabledContentAlpha else DisabledContentAlpha }

    CompositionLocalProvider(
        LocalContentAlpha provides contentAlphaProvider,
    ) {
        val dim = KrailTheme.dimensions
        val contentAlpha = LocalContentAlpha.current
        val backgroundColor = remember(selected, themeColor, contentAlpha) {
            if (selected) themeColor.copy(alpha = contentAlpha) else Color.Transparent
        }
        val borderColor =
            remember(themeColor, contentAlpha) { themeColor.copy(alpha = contentAlpha) }

        Box(
            modifier = modifier
                .height(
                    when (type) {
                        RadioButtonType.SMALL -> SmallRadioButtonHeight
                        RadioButtonType.DEFAULT -> dim.buttonRoundSize
                    },
                )
                .clip(shape = RoundedCornerShape(dim.radiusS))
                .background(
                    color = backgroundColor,
                    shape = RoundedCornerShape(dim.radiusS),
                )
                .border(
                    width = dim.strokeRegular,
                    color = borderColor,
                    shape = RoundedCornerShape(dim.radiusS),
                )
                .scalingKlickable(
                    onClick = onClick,
                    enabled = enabled,
                )
                .padding(vertical = dim.spacingXS, horizontal = dim.spacingL),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = KrailTheme.typography.title,
                color = if (selected) themeContentColor() else themeColor,
            )
        }
    }
}

enum class RadioButtonType {
    SMALL,
    DEFAULT,
}
