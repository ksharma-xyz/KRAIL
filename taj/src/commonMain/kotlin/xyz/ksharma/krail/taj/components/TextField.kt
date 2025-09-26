package xyz.ksharma.krail.taj.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.taj.LocalContentAlpha
import xyz.ksharma.krail.taj.LocalTextColor
import xyz.ksharma.krail.taj.LocalTextStyle
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.themeColor
import xyz.ksharma.krail.taj.tokens.TextFieldTokens
import xyz.ksharma.krail.taj.tokens.TextFieldTokens.TextFieldHeight
import xyz.ksharma.krail.taj.tokens.TextFieldTokens.TextSelectionBackgroundOpacity

/**
 * Important documentation links:
 * https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.7.0
 */
@Composable
fun TextField(
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    initialText: String? = null,
    enabled: Boolean = true,
    textStyle: TextStyle? = null,
    readOnly: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    imeAction: ImeAction = ImeAction.Default,
    filter: (CharSequence) -> CharSequence = { it },
    maxLength: Int = Int.MAX_VALUE,
    onTextChange: (CharSequence) -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val contentAlpha = if (enabled) 1f else TextFieldTokens.DisabledLabelOpacity

    val textFieldState = rememberTextFieldState(initialText.orEmpty())
    val textSelectionColors = TextSelectionColors(
        handleColor = KrailTheme.colors.onSurface,
        backgroundColor = KrailTheme.colors.onSurface.copy(alpha = TextSelectionBackgroundOpacity),
    )

    LaunchedEffect(textFieldState.text) {
        val filteredText = filter(textFieldState.text).take(maxLength)
        if (textFieldState.text != filteredText) {
            textFieldState.setTextAndPlaceCursorAtEnd(filteredText.toString())
        }
        onTextChange(filteredText)
    }

    CompositionLocalProvider(
        LocalTextColor provides KrailTheme.colors.onSurface,
        LocalTextStyle provides KrailTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal),
        LocalTextSelectionColors provides textSelectionColors,
        LocalContentAlpha provides contentAlpha,
    ) {
        BasicTextField(
            state = textFieldState,
            enabled = enabled,
            modifier = modifier
                .fillMaxWidth()
                .height(TextFieldHeight),
            // This will change the colors of the innerTextField() composable.
            textStyle = textStyle
                ?: LocalTextStyle.current.copy(
                    color = LocalTextColor.current.copy(alpha = contentAlpha),
                ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Text,
                imeAction = imeAction,
                hintLocales = LocaleList.current,
            ),
            lineLimits = TextFieldLineLimits.SingleLine,
            readOnly = readOnly,
            interactionSource = interactionSource,
            cursorBrush = SolidColor(KrailTheme.colors.onSurface),
            decorator = { innerTextField ->
                Row(
                    modifier = Modifier
                        .background(
                            shape = RoundedCornerShape(TextFieldHeight.div(2)),
                            color = KrailTheme.colors.surface,
                        )
                        .padding(vertical = 4.dp)
                        .padding(end = 16.dp, start = if (leadingIcon != null) 0.dp else 16.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    leadingIcon?.let { icon ->
                        icon.invoke()
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    if (textFieldState.text.isEmpty() && isFocused) {
                        /* Using a Box ensures that cursor and placeholder are displayed on top of
                     each other, so the cursor is always displayed at the start if the TextField.
                         */
                        Box {
                            innerTextField() // To display cursor
                            TextFieldPlaceholder(placeholder = placeholder)
                        }
                    } else if (textFieldState.text.isEmpty()) {
                        TextFieldPlaceholder(placeholder = placeholder)
                    } else {
                        innerTextField()
                        // add trailing icon here if required / e.g. Clear / Scan QR code
                    }
                }
            },
        )
    }
}

@Composable
fun ThemeTextFieldPlaceholderText(
    text: String,
    isActive: Boolean,
    activeColor: Color = themeColor(),
    inactiveColor: Color = KrailTheme.colors.labelPlaceholder,
    activeTextStyle: TextStyle = KrailTheme.typography.titleLarge,
    inactiveStyle: TextStyle = KrailTheme.typography.bodyLarge,
    maxLines: Int = 1,
) {
    CompositionLocalProvider(
        LocalTextColor provides if (isActive) activeColor else inactiveColor,
        LocalTextStyle provides if (isActive) activeTextStyle else inactiveStyle,
    ) {
        TextFieldPlaceholder(
            placeholder = text,
            color = LocalTextColor.current,
            maxLines = maxLines,
        )
    }
}

@Composable
private fun TextFieldPlaceholder(
    placeholder: String? = null,
    color: Color = KrailTheme.colors.labelPlaceholder,
    maxLines: Int = 1,
) {
    Text(
        text = placeholder.orEmpty(),
        color = color,
        maxLines = maxLines,
    )
}

// region Previews

@Preview(name = "Text Field Enabled - Light")
@Composable
private fun TextFieldEnabledPreviewLight() {
    PreviewTextFieldContent {
        TextField(placeholder = "Station", initialText = "Central")
        Spacer(Modifier.height(8.dp))
        TextField(placeholder = "Search here")
    }
}

@Preview(name = "Text Field Enabled - Dark")
@Composable
private fun TextFieldEnabledPreviewDark() {
    PreviewTextFieldContent {
        TextField(placeholder = "Station", initialText = "Central")
        Spacer(Modifier.height(8.dp))
        TextField(placeholder = "Search here")
    }
}

@Preview(name = "Text Field Disabled - Light")
@Composable
private fun TextFieldDisabledPreviewLight() {
    PreviewTextFieldContent {
        TextField(enabled = false, initialText = "Disabled TextField")
        Spacer(Modifier.height(8.dp))
        TextField(enabled = false, placeholder = "Disabled Placeholder")
    }
}

@Preview(name = "Text Field Disabled - Dark")
@Composable
private fun TextFieldDisabledPreviewDark() {
    PreviewTextFieldContent {
        TextField(enabled = false, initialText = "Disabled TextField")
        Spacer(Modifier.height(8.dp))
        TextField(enabled = false, placeholder = "Disabled Placeholder")
    }
}

@Composable
private fun PreviewTextFieldContent(content: @Composable () -> Unit) {
    PreviewTheme(themeStyle = KrailThemeStyle.PurpleDrip, darkTheme = false) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .background(KrailTheme.colors.onSurface),
        ) {
            content()
        }
    }
}

// endregion
