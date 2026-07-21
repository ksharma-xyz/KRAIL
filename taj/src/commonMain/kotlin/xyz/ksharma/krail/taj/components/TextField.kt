package xyz.ksharma.krail.taj.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
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
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.taj.LocalContentAlpha
import xyz.ksharma.krail.taj.LocalTextColor
import xyz.ksharma.krail.taj.LocalTextStyle
import xyz.ksharma.krail.taj.components.TextFieldPlaceholderDefaults.MAX_LINES
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.themeColor
import xyz.ksharma.krail.taj.tokens.SpacingTokens
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
    state: TextFieldState? = null,
    enabled: Boolean = true,
    textStyle: TextStyle? = null,
    readOnly: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    imeAction: ImeAction = ImeAction.Default,
    filter: (CharSequence) -> CharSequence = { it },
    maxLength: Int = Int.MAX_VALUE,
    // Same shape as ButtonDefaults: callers override the whole colour set rather than passing
    // one-off colours. Defaults keep every existing call site unchanged.
    colors: TextFieldColors = TextFieldDefaults.colors(),
    onTextChange: (CharSequence) -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val contentAlpha = if (enabled) 1f else TextFieldTokens.DisabledLabelOpacity

    // Hoisted state takes precedence — callers that need to mutate the text from
    // outside (e.g. selecting a suggestion chip that fills the field) must pass
    // their own state so the field is not rekeyed and focus / IME are preserved.
    val textFieldState = state ?: rememberTextFieldState(initialText.orEmpty())
    val textSelectionColors = TextSelectionColors(
        handleColor = colors.cursorColor,
        backgroundColor = colors.cursorColor.copy(alpha = TextSelectionBackgroundOpacity),
    )

    LaunchedEffect(textFieldState.text) {
        val filteredText = filter(textFieldState.text).take(maxLength)
        if (textFieldState.text != filteredText) {
            textFieldState.setTextAndPlaceCursorAtEnd(filteredText.toString())
        }
        onTextChange(filteredText)
    }

    CompositionLocalProvider(
        LocalTextColor provides colors.contentColor,
        LocalTextStyle provides KrailTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal),
        LocalTextSelectionColors provides textSelectionColors,
        LocalContentAlpha provides contentAlpha,
    ) {
        BasicTextField(
            state = textFieldState,
            enabled = enabled,
            modifier = modifier
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
            cursorBrush = SolidColor(colors.cursorColor),
            // Workaround: Using an anonymous object instead of a lambda
            // https://youtrack.jetbrains.com/projects/CMP/issues/CMP-9456/Reference-to-lambda-in-lambda-in-function-TextField-can-not-be-evaluated
            decorator = object : TextFieldDecorator {
                @Composable
                override fun Decoration(innerTextField: @Composable () -> Unit) {
                    val innerTextFieldContent = remember { movableContentOf { innerTextField() } }
                    Row(
                        modifier = Modifier
                            .background(
                                shape = RoundedCornerShape(TextFieldHeight.div(2)),
                                color = colors.containerColor,
                            )
                            .padding(vertical = SpacingTokens.XS)
                            .padding(
                                end = SpacingTokens.XL,
                                start = if (leadingIcon != null) 0.dp else SpacingTokens.XL,
                            ),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        leadingIcon?.let { icon ->
                            icon.invoke()
                            Spacer(modifier = Modifier.width(SpacingTokens.XS))
                        }

                        if (textFieldState.text.isEmpty() && isFocused) {
                            Box {
                                innerTextFieldContent() // Displays cursor
                                TextFieldPlaceholder(
                                    placeholder = placeholder,
                                    color = colors.placeholderColor,
                                )
                            }
                        } else if (textFieldState.text.isEmpty()) {
                            TextFieldPlaceholder(
                                placeholder = placeholder,
                                color = colors.placeholderColor,
                            )
                        } else {
                            innerTextFieldContent()
                        }
                    }
                }
            },
        )
    }
}

// region Placeholder

@Composable
fun ThemeTextFieldPlaceholderText(
    text: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    colors: TextFieldPlaceholderColors = TextFieldPlaceholderDefaults.colors(),
    textStyles: TextFieldPlaceholderTextStyles = TextFieldPlaceholderDefaults.textStyles(),
) {
    CompositionLocalProvider(
        LocalTextColor provides if (isActive) colors.activeColor else colors.inactiveColor,
        LocalTextStyle provides if (isActive) textStyles.activeTextStyle else textStyles.inactiveTextStyle,
    ) {
        TextFieldPlaceholder(
            placeholder = text,
            color = LocalTextColor.current,
            maxLines = MAX_LINES,
            modifier = modifier,
        )
    }
}

/**
 * Colour set for [TextField], overridable the same way [ButtonColors] is.
 */
@Immutable
data class TextFieldColors(
    val containerColor: Color,
    val contentColor: Color,
    val placeholderColor: Color,
    val cursorColor: Color,
)

object TextFieldDefaults {

    private const val INVERTED_PLACEHOLDER_ALPHA = 0.7f

    /** The field sits on the page surface and reads as part of it. */
    @Composable
    fun colors(): TextFieldColors = TextFieldColors(
        containerColor = KrailTheme.colors.surface,
        contentColor = KrailTheme.colors.onSurface,
        placeholderColor = KrailTheme.colors.softLabel,
        cursorColor = KrailTheme.colors.onSurface,
    )

    /**
     * Flips the surface: a dark bar on a light page, a light bar on a dark one.
     *
     * For search fields sitting directly on the page surface, where matching that surface
     * would leave the control with no visible edge at all. Built from `onSurface` / `surface`
     * rather than fixed black and white, so it inverts correctly in both themes and stays
     * legible by construction.
     */
    @Composable
    fun invertedColors(): TextFieldColors = TextFieldColors(
        containerColor = KrailTheme.colors.onSurface,
        contentColor = KrailTheme.colors.surface,
        placeholderColor = KrailTheme.colors.surface.copy(alpha = INVERTED_PLACEHOLDER_ALPHA),
        cursorColor = KrailTheme.colors.surface,
    )
}

@Immutable
data class TextFieldPlaceholderColors(
    val activeColor: Color,
    val inactiveColor: Color,
)

@Immutable
data class TextFieldPlaceholderTextStyles(
    val activeTextStyle: TextStyle,
    val inactiveTextStyle: TextStyle,
)

object TextFieldPlaceholderDefaults {

    const val MAX_LINES: Int = 1

    @Composable
    fun colors(
        activeColor: Color = themeColor(),
        inactiveColor: Color = KrailTheme.colors.onSurface,
    ): TextFieldPlaceholderColors {
        return TextFieldPlaceholderColors(
            activeColor = activeColor,
            inactiveColor = inactiveColor,
        )
    }

    @Composable
    fun textStyles(
        activeTextStyle: TextStyle = KrailTheme.typography.titleLarge,
        inactiveTextStyle: TextStyle = KrailTheme.typography.bodyLarge,
    ): TextFieldPlaceholderTextStyles {
        return TextFieldPlaceholderTextStyles(
            activeTextStyle = activeTextStyle,
            inactiveTextStyle = inactiveTextStyle,
        )
    }
}

@Composable
private fun TextFieldPlaceholder(
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    color: Color = KrailTheme.colors.labelPlaceholder,
    maxLines: Int = MAX_LINES,
) {
    Text(
        text = placeholder.orEmpty(),
        color = color,
        maxLines = maxLines,
        modifier = modifier,
    )
}

// endregion

// region Previews

@ScreenshotTest
@PreviewComponent
@Composable
private fun TextFieldEnabledPreviewLight() {
    PreviewTextFieldContent {
        TextField(placeholder = "Station", initialText = "Central")
        Spacer(Modifier.height(SpacingTokens.M))
        TextField(placeholder = "Search here")
    }
}

@ScreenshotTest
@PreviewComponent
@Composable
private fun TextFieldDisabledPreviewLight() {
    PreviewTextFieldContent {
        TextField(enabled = false, initialText = "Disabled TextField")
        Spacer(Modifier.height(SpacingTokens.M))
        TextField(enabled = false, placeholder = "Disabled Placeholder")
    }
}

@Composable
private fun PreviewTextFieldContent(content: @Composable () -> Unit) {
    PreviewTheme(themeStyle = KrailThemeStyle.PurpleDrip) {
        Column(
            modifier = Modifier
                .padding(SpacingTokens.XL)
                .background(KrailTheme.colors.onSurface),
        ) {
            content()
        }
    }
}

// endregion
