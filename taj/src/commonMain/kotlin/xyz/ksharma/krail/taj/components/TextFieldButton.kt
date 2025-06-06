package xyz.ksharma.krail.taj.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.LocalContentAlpha
import xyz.ksharma.krail.taj.LocalTextColor
import xyz.ksharma.krail.taj.LocalTextStyle
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.tokens.TextFieldTokens
import xyz.ksharma.krail.taj.tokens.TextFieldTokens.TextFieldHeight

/**
 * A button that looks like a text field.
 *
 * @param modifier The modifier to apply to this component.
 * @param enabled Whether the button is enabled.
 * @param content The content of the button.
 *
 * TODO - how to ensure modifiers for TextField and TextFieldButton are consistent?
 */
@Composable
fun TextFieldButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val contentAlpha = if (enabled) 1f else TextFieldTokens.DisabledLabelOpacity

    CompositionLocalProvider(
        LocalTextColor provides KrailTheme.colors.onSurface,
        LocalTextStyle provides KrailTheme.typography.bodyLarge,
        LocalContentAlpha provides contentAlpha,
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(TextFieldHeight)
                .clip(RoundedCornerShape(50))
                .background(color = KrailTheme.colors.surface)
                .klickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            content()
        }
    }
}
