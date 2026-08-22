package xyz.ksharma.krail.taj.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.Dp
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
import xyz.ksharma.krail.taj.themeInkColor
import xyz.ksharma.krail.taj.tokens.RadiusTokens
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
    // Sits at the field's trailing edge, top aligned on a multiline field. The field reserves
    // room for it rather than letting it float on top, so text can never run underneath.
    trailingIcon: (@Composable () -> Unit)? = null,
    imeAction: ImeAction = ImeAction.Default,
    filter: (CharSequence) -> CharSequence = { it },
    maxLength: Int = Int.MAX_VALUE,
    // Same shape as ButtonDefaults: callers override the whole colour set rather than passing
    // one-off colours. Defaults keep every existing call site unchanged.
    colors: TextFieldColors = TextFieldDefaults.colors(),
    // SingleLine keeps the pill shape and the fixed [TextFieldHeight]; a multiline limit
    // drops both so the caller's own height/shape apply (see [shape]).
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.SingleLine,
    shape: Shape? = null,
    // Overrides the multiline branch's own minimum. A caller that wants a field which starts
    // as a one line pill and grows only when the words need it passes the single line height
    // here; without it a multiline field is 128dp tall the moment it is composed, which is a
    // box rather than a pill no matter what [shape] says.
    minHeight: Dp? = null,
    // Overrides the inset between the box's edge and its text. Defaults keep every existing
    // field exactly as it was; a field that is the whole point of its screen wants a roomier
    // one than a field in a list of them.
    contentPadding: PaddingValues? = null,
    // Fires on the IME action key (e.g. Send) - null means the platform's default
    // behaviour (usually just dismissing the keyboard) applies.
    onSubmit: (() -> Unit)? = null,
    // Kept as the last parameter (existing call sites use trailing-lambda syntax for it).
    onTextChange: (CharSequence) -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val contentAlpha = if (enabled) 1f else TextFieldTokens.DisabledLabelOpacity
    val layout = textFieldLayout(lineLimits = lineLimits, shape = shape, minHeight = minHeight)

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
            modifier = modifier.then(layout.heightModifier),
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
            lineLimits = lineLimits,
            readOnly = readOnly,
            interactionSource = interactionSource,
            cursorBrush = SolidColor(colors.cursorColor),
            onKeyboardAction = onSubmit?.let { submit -> KeyboardActionHandler { submit() } },
            // Workaround: Using an anonymous object instead of a lambda
            // https://youtrack.jetbrains.com/projects/CMP/issues/CMP-9456/Reference-to-lambda-in-lambda-in-function-TextField-can-not-be-evaluated
            decorator = object : TextFieldDecorator {
                @Composable
                override fun Decoration(innerTextField: @Composable () -> Unit) {
                    val innerTextFieldContent = remember { movableContentOf { innerTextField() } }
                    Row(
                        modifier = layout.containerModifier
                            .background(
                                shape = layout.shape,
                                color = colors.containerColor,
                            )
                            // A trailing control is inset by the smaller step and the text
                            // makes up the difference below, so the control sits an equal
                            // gap from the field's top and end while the text keeps its own
                            // roomier inset. Unequal insets on a control read as bolted on.
                            .then(
                                if (contentPadding != null) {
                                    Modifier.padding(contentPadding)
                                } else {
                                    Modifier
                                        .padding(
                                            vertical = if (trailingIcon != null) {
                                                SpacingTokens.M
                                            } else {
                                                layout.verticalPadding
                                            },
                                        )
                                        .padding(
                                            end = if (trailingIcon != null) SpacingTokens.M else SpacingTokens.XL,
                                            start = if (leadingIcon != null) 0.dp else SpacingTokens.XL,
                                        )
                                },
                            ),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = layout.verticalAlignment,
                    ) {
                        leadingIcon?.let { icon ->
                            icon.invoke()
                            Spacer(modifier = Modifier.width(SpacingTokens.XS))
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .then(
                                    if (trailingIcon != null) {
                                        Modifier.padding(
                                            top = SpacingTokens.M,
                                            bottom = SpacingTokens.M,
                                            end = SpacingTokens.M,
                                        )
                                    } else {
                                        Modifier
                                    },
                                ),
                        ) {
                            if (textFieldState.text.isEmpty() && isFocused) {
                                Box {
                                    innerTextFieldContent() // Displays cursor
                                    TextFieldPlaceholder(
                                        placeholder = placeholder,
                                        color = colors.placeholderColor,
                                        maxLines = layout.placeholderMaxLines,
                                    )
                                }
                            } else if (textFieldState.text.isEmpty()) {
                                TextFieldPlaceholder(
                                    placeholder = placeholder,
                                    color = colors.placeholderColor,
                                    maxLines = layout.placeholderMaxLines,
                                )
                            } else {
                                innerTextFieldContent()
                            }
                        }

                        trailingIcon?.invoke()
                    }
                }
            },
        )
    }
}

/**
 * The handful of layout values that differ between a single-line pill and a multiline box,
 * resolved in one place so [TextField] itself stays branch-free over [TextFieldLineLimits].
 */
@Immutable
private class TextFieldLayout(
    val heightModifier: Modifier,
    val containerModifier: Modifier,
    val shape: Shape,
    val verticalPadding: Dp,
    val verticalAlignment: Alignment.Vertical,
    val placeholderMaxLines: Int,
)

private fun textFieldLayout(
    lineLimits: TextFieldLineLimits,
    shape: Shape?,
    minHeight: Dp? = null,
): TextFieldLayout =
    if (lineLimits == TextFieldLineLimits.SingleLine) {
        TextFieldLayout(
            heightModifier = Modifier.height(TextFieldHeight),
            containerModifier = Modifier,
            shape = shape ?: RoundedCornerShape(TextFieldHeight.div(2)),
            verticalPadding = SpacingTokens.XS,
            verticalAlignment = Alignment.CenterVertically,
            placeholderMaxLines = MAX_LINES,
        )
    } else {
        // Grows with its own text between the caller's line limits, rather than filling
        // whatever box it is dropped into. Filling was right when the only multiline caller
        // handed it a fixed height; in a bottom sheet with no height of its own it swallowed
        // the whole sheet and pushed every other control off screen.
        //
        // The minimum height is not redundant with the caller's own minHeightInLines: an empty
        // unfocused field composes only its placeholder, never the inner text field, so the
        // line limits have nothing to size and the box collapses to one line until tapped.
        TextFieldLayout(
            heightModifier = Modifier,
            containerModifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight ?: TextFieldDefaults.MultiLineMinHeight),
            shape = shape ?: RoundedCornerShape(RadiusTokens.XL),
            verticalPadding = SpacingTokens.M,
            verticalAlignment = Alignment.Top,
            placeholderMaxLines = Int.MAX_VALUE,
        )
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

    /**
     * Roughly three lines of body text plus the box's own vertical padding: enough that an
     * empty multiline field reads as somewhere to write a sentence rather than a search bar.
     *
     * Public because anything that swaps itself in for a multiline field has to be exactly
     * this tall, or the swap resizes the layout around it.
     */
    val MultiLineMinHeight = 128.dp

    /**
     * The height a single line field is, published so a multiline caller can ask to start at
     * exactly one line and grow from there. Passed as `minHeight`, it is what makes a field a
     * pill on open rather than a box, without giving up multiline once the words need it.
     */
    val SingleLineHeight = TextFieldHeight

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
        activeColor: Color = themeInkColor(),
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
