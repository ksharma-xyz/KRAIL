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
import xyz.ksharma.krail.taj.LocalContentAlpha
import xyz.ksharma.krail.taj.LocalTextColor
import xyz.ksharma.krail.taj.LocalTextStyle
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.tokens.SpacingTokens
import xyz.ksharma.krail.taj.tokens.TextFieldTokens
import xyz.ksharma.krail.taj.tokens.TextFieldTokens.TextFieldHeight

/**
 * A button that looks like a text field.
 *
 * @param onClick What tapping does, or `null` for a field that only reports. A null click is
 * not the same as `enabled = false`: a disabled field is dimmed, which says "you cannot do this
 * yet", where a read-only one is at full strength and simply is not a control. The Ask KRAIL
 * result card shows the two stops it found this way — they are an answer being read, and a
 * field that looks tappable and is not is worse than one that plainly is not.
 * @param modifier The modifier to apply to this component.
 * @param enabled Whether the button is enabled.
 * @param content The content of the button.
 *
 * TODO - how to ensure modifiers for TextField and TextFieldButton are consistent?
 */
@Composable
fun TextFieldButton(
    onClick: (() -> Unit)?,
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
                .then(if (onClick != null) Modifier.klickable(onClick = onClick) else Modifier)
                .padding(horizontal = SpacingTokens.XL, vertical = SpacingTokens.XS),
            contentAlignment = Alignment.CenterStart,
        ) {
            content()
        }
    }
}
