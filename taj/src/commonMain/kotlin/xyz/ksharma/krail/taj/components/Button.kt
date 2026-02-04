package xyz.ksharma.krail.taj.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.taj.LocalContainerColor
import xyz.ksharma.krail.taj.LocalContentAlpha
import xyz.ksharma.krail.taj.LocalContentColor
import xyz.ksharma.krail.taj.LocalTextColor
import xyz.ksharma.krail.taj.LocalTextStyle
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.modifier.scalingKlickable
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.theme.getForegroundColor
import xyz.ksharma.krail.taj.themeBackgroundColor
import xyz.ksharma.krail.taj.tokens.ContentAlphaTokens.DisabledContentAlpha
import xyz.ksharma.krail.taj.tokens.ContentAlphaTokens.EnabledContentAlpha

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    dimensions: ButtonDimensions = ButtonDefaults.largeButtonSize(),
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val contentAlphaProvider =
        rememberSaveable(enabled) { if (enabled) EnabledContentAlpha else DisabledContentAlpha }

    CompositionLocalProvider(
        LocalContentAlpha provides contentAlphaProvider,
        LocalContainerColor provides colors.containerColor,
        LocalTextStyle provides buttonTextStyle(dimensions),
        LocalTextColor provides colors.contentColor,
    ) {
        Box(
            modifier = modifier
                .then(
                    when (dimensions) {
                        ButtonDefaults.largeButtonSize() -> Modifier.fillMaxWidth()
                        else -> Modifier
                    },
                )
                .scalingKlickable(
                    enabled = enabled,
                    onClick = onClick,
                )
                .heightIn(dimensions.height)
                .background(
                    color = LocalContainerColor.current.copy(alpha = LocalContentAlpha.current),
                    shape = dimensions.shape,
                )
                .padding(dimensions.padding),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

@Composable
fun SubtleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dimensions: ButtonDimensions = ButtonDefaults.largeButtonSize(),
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val contentAlphaProvider =
        rememberSaveable(enabled) { if (enabled) EnabledContentAlpha else DisabledContentAlpha }

    CompositionLocalProvider(
        LocalContentAlpha provides contentAlphaProvider,
        LocalTextStyle provides buttonTextStyle(dimensions),
        LocalContainerColor provides ButtonDefaults.subtleButtonColors().containerColor,
        LocalTextColor provides ButtonDefaults.subtleButtonColors().contentColor,
    ) {
        Box(
            modifier = modifier
                .then(
                    when (dimensions) {
                        ButtonDefaults.largeButtonSize() -> Modifier.fillMaxWidth()
                        else -> Modifier
                    },
                )
                .heightIn(dimensions.height)
                .clip(dimensions.shape)
                .background(
                    color = LocalContainerColor.current,
                    shape = dimensions.shape,
                )
                .klickable(
                    enabled = enabled,
                    onClick = onClick,
                )
                .padding(dimensions.padding),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dimensions: ButtonDimensions = ButtonDefaults.smallButtonSize(),
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val contentAlphaProvider =
        rememberSaveable(enabled) { if (enabled) EnabledContentAlpha else DisabledContentAlpha }

    CompositionLocalProvider(
        LocalContentAlpha provides contentAlphaProvider,
        LocalTextStyle provides buttonTextStyle(dimensions),
        LocalContainerColor provides ButtonDefaults.textButtonColors().containerColor,
        LocalContentColor provides ButtonDefaults.textButtonColors().contentColor,
        LocalTextColor provides ButtonDefaults.textButtonColors().contentColor,
    ) {
        Box(
            modifier = modifier
                .heightIn(dimensions.height)
                .background(
                    color = Color.Transparent,
                    shape = dimensions.shape,
                )
                .clip(dimensions.shape)
                .klickable(
                    enabled = enabled,
                    onClick = onClick,
                )
                .padding(dimensions.padding),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

@Composable
fun AlertButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dimensions: ButtonDimensions = ButtonDefaults.smallButtonSize(),
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val contentAlphaProvider =
        rememberSaveable(enabled) { if (enabled) EnabledContentAlpha else DisabledContentAlpha }

    CompositionLocalProvider(
        LocalContentAlpha provides contentAlphaProvider,
        LocalTextStyle provides buttonTextStyle(dimensions),
        LocalContainerColor provides ButtonDefaults.alertButtonColors().containerColor,
        LocalTextColor provides ButtonDefaults.alertButtonColors().contentColor,
    ) {
        Box(
            modifier = modifier
                .then(
                    when (dimensions) {
                        ButtonDefaults.largeButtonSize() -> Modifier.fillMaxWidth()
                        else -> Modifier
                    },
                )
                .clickable(
                    role = Role.Button,
                    interactionSource = remember { MutableInteractionSource() },
                    enabled = enabled,
                    indication = null,
                    onClick = onClick,
                )
                .heightIn(dimensions.height)
                .background(
                    color = LocalContainerColor.current.copy(alpha = LocalContentAlpha.current),
                    shape = dimensions.shape,
                )
                .padding(dimensions.padding),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

@Composable
private fun buttonTextStyle(dimensions: ButtonDimensions) =
    when (dimensions) {
        ButtonDefaults.extraSmallButtonSize() -> KrailTheme.typography.bodySmall
        ButtonDefaults.smallButtonSize() -> KrailTheme.typography.titleSmall
        ButtonDefaults.mediumButtonSize() -> KrailTheme.typography.titleMedium
        ButtonDefaults.largeButtonSize() -> KrailTheme.typography.titleLarge
        else -> KrailTheme.typography.titleSmall
    }

object ButtonDefaults {

    @Composable
    fun textButtonColors(): ButtonColors {
        val hexThemeColor: String by LocalThemeColor.current
        val themeColor: Color by remember(hexThemeColor) {
            mutableStateOf(hexThemeColor.hexToComposeColor())
        }
        return ButtonColors(
            containerColor = Color.Transparent,
            contentColor = themeColor,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = themeColor.copy(alpha = DisabledContentAlpha),
        )
    }

    @Composable
    fun alertButtonColors(): ButtonColors {
        val containerColor = KrailTheme.colors.alert

        return ButtonColors(
            containerColor = containerColor,
            contentColor = getForegroundColor(containerColor),
            disabledContainerColor = containerColor.copy(alpha = DisabledContentAlpha),
            disabledContentColor = getForegroundColor(containerColor)
                .copy(alpha = DisabledContentAlpha),
        )
    }

    @Composable
    fun subtleButtonColors(): ButtonColors {
        val containerColor = themeBackgroundColor()
        val contentColor: Color = KrailTheme.colors.onSurface

        return ButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = DisabledContentAlpha),
            disabledContentColor = contentColor.copy(alpha = DisabledContentAlpha),
        )
    }

    // Kotlin
    @Composable
    fun buttonColors(
        customContainerColor: Color? = null,
        customContentColor: Color? = null,
    ): ButtonColors {
        val hexThemeColor: String by LocalThemeColor.current
        val hexContainerColor: String by remember(hexThemeColor) {
            mutableStateOf(hexThemeColor)
        }
        val defaultContainerColor: Color by remember(hexContainerColor) {
            mutableStateOf(hexContainerColor.hexToComposeColor())
        }
        val containerColor = customContainerColor ?: defaultContainerColor
        val defaultContentColor: Color by remember {
            mutableStateOf(
                getForegroundColor(
                    containerColor,
                ),
            )
        }
        val contentColor = customContentColor ?: defaultContentColor

        return ButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = DisabledContentAlpha),
            disabledContentColor = contentColor.copy(alpha = DisabledContentAlpha),
        )
    }

    @Composable
    fun monochromeButtonColors(): ButtonColors {
        return buttonColors(
            customContainerColor = KrailTheme.colors.onSurface,
            customContentColor = KrailTheme.colors.surface,
        )
    }

    fun extraSmallButtonSize(): ButtonDimensions {
        return ButtonDimensions(
            height = 18.dp,
            padding = PaddingValues(vertical = 2.dp, horizontal = 8.dp),
        )
    }

    fun smallButtonSize(): ButtonDimensions {
        return ButtonDimensions(
            height = 20.dp,
            padding = PaddingValues(vertical = 4.dp, horizontal = 10.dp),
        )
    }

    fun mediumButtonSize(): ButtonDimensions {
        return ButtonDimensions(
            height = 32.dp,
            padding = PaddingValues(vertical = 6.dp, horizontal = 12.dp),
        )
    }

    fun largeButtonSize(): ButtonDimensions {
        return ButtonDimensions(
            height = 32.dp,
            padding = PaddingValues(vertical = 10.dp, horizontal = 16.dp),
        )
    }
}

@Immutable
data class ButtonDimensions(
    val height: Dp,
    val padding: PaddingValues,
    val shape: RoundedCornerShape = RoundedCornerShape(50),
)

// ============================================================================
// PREVIEW ORGANIZATION - HYBRID APPROACH
// ============================================================================
// Strategy:
// 1. COMPOSITE PREVIEWS: Show all variations in one view (for visual design review)
// 2. SNAPSHOT TESTS: Individual previews only for critical variations
//
// This reduces preview count from 88 → ~40 while maintaining:
// ✓ Visual overview (composite previews)
// ✓ Precise testing (individual snapshots)
// ✓ Easier maintenance
// ============================================================================

// ═══════════════════════════════════════════════════════════════════════════
// COMPOSITE PREVIEWS - For Visual Design Review in IDE
// ═══════════════════════════════════════════════════════════════════════════

// Preview string constants
private const val PREVIEW_TEXT_ALL_SIZES = "All Sizes"
private const val PREVIEW_TEXT_STATES = "States"
private const val PREVIEW_TEXT_EXTRA_SMALL = "Extra Small"
private const val PREVIEW_TEXT_SMALL = "Small"
private const val PREVIEW_TEXT_MEDIUM = "Medium"
private const val PREVIEW_TEXT_LARGE = "Large"
private const val PREVIEW_TEXT_ENABLED = "Enabled"
private const val PREVIEW_TEXT_DISABLED = "Disabled"

// region Primary Button - Composite Previews

@PreviewComponent
@Composable
private fun PrimaryButtonShowcase() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        Column(
            modifier = Modifier
                .background(KrailTheme.colors.surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(PREVIEW_TEXT_ALL_SIZES, style = KrailTheme.typography.titleMedium)
            Button(onClick = {}, dimensions = ButtonDefaults.extraSmallButtonSize()) {
                Text(PREVIEW_TEXT_EXTRA_SMALL)
            }
            Button(onClick = {}, dimensions = ButtonDefaults.smallButtonSize()) {
                Text(PREVIEW_TEXT_SMALL)
            }
            Button(onClick = {}, dimensions = ButtonDefaults.mediumButtonSize()) {
                Text(PREVIEW_TEXT_MEDIUM)
            }
            Button(onClick = {}, dimensions = ButtonDefaults.largeButtonSize()) {
                Text(PREVIEW_TEXT_LARGE)
            }

            Spacer(Modifier.height(8.dp))
            Text(PREVIEW_TEXT_STATES, style = KrailTheme.typography.titleMedium)
            Button(onClick = {}, enabled = true, dimensions = ButtonDefaults.mediumButtonSize()) {
                Text(PREVIEW_TEXT_ENABLED)
            }
            Button(onClick = {}, enabled = false, dimensions = ButtonDefaults.mediumButtonSize()) {
                Text(PREVIEW_TEXT_DISABLED)
            }
        }
    }
}

// endregion

// region Subtle Button - Composite Previews

@PreviewComponent
@Composable
private fun SubtleButtonShowcase() {
    PreviewTheme(themeStyle = KrailThemeStyle.Bus) {
        Column(
            modifier = Modifier
                .background(KrailTheme.colors.surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(PREVIEW_TEXT_ALL_SIZES, style = KrailTheme.typography.titleMedium)
            SubtleButton(onClick = {}, dimensions = ButtonDefaults.extraSmallButtonSize()) {
                Text(PREVIEW_TEXT_EXTRA_SMALL)
            }
            SubtleButton(onClick = {}, dimensions = ButtonDefaults.smallButtonSize()) {
                Text(PREVIEW_TEXT_SMALL)
            }
            SubtleButton(onClick = {}, dimensions = ButtonDefaults.mediumButtonSize()) {
                Text(PREVIEW_TEXT_MEDIUM)
            }
            SubtleButton(onClick = {}, dimensions = ButtonDefaults.largeButtonSize()) {
                Text(PREVIEW_TEXT_LARGE)
            }

            Spacer(Modifier.height(8.dp))
            Text(PREVIEW_TEXT_STATES, style = KrailTheme.typography.titleMedium)
            SubtleButton(
                onClick = {},
                enabled = true,
                dimensions = ButtonDefaults.mediumButtonSize(),
            ) {
                Text(PREVIEW_TEXT_ENABLED)
            }
            SubtleButton(
                onClick = {},
                enabled = false,
                dimensions = ButtonDefaults.mediumButtonSize(),
            ) {
                Text(PREVIEW_TEXT_DISABLED)
            }
        }
    }
}

// endregion

// region Text Button - Composite Previews

@PreviewComponent
@Composable
private fun TextButtonShowcase() {
    PreviewTheme(themeStyle = KrailThemeStyle.Metro) {
        Column(
            modifier = Modifier
                .background(KrailTheme.colors.surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(PREVIEW_TEXT_ALL_SIZES, style = KrailTheme.typography.titleMedium)
            TextButton(onClick = {}, dimensions = ButtonDefaults.extraSmallButtonSize()) {
                Text(PREVIEW_TEXT_EXTRA_SMALL)
            }
            TextButton(onClick = {}, dimensions = ButtonDefaults.smallButtonSize()) {
                Text(PREVIEW_TEXT_SMALL)
            }
            TextButton(onClick = {}, dimensions = ButtonDefaults.mediumButtonSize()) {
                Text(PREVIEW_TEXT_MEDIUM)
            }
            TextButton(onClick = {}, dimensions = ButtonDefaults.largeButtonSize()) {
                Text(PREVIEW_TEXT_LARGE)
            }

            Spacer(Modifier.height(8.dp))
            Text(PREVIEW_TEXT_STATES, style = KrailTheme.typography.titleMedium)
            TextButton(
                onClick = {},
                enabled = true,
                dimensions = ButtonDefaults.mediumButtonSize(),
            ) {
                Text(PREVIEW_TEXT_ENABLED)
            }
            TextButton(
                onClick = {},
                enabled = false,
                dimensions = ButtonDefaults.mediumButtonSize(),
            ) {
                Text(PREVIEW_TEXT_DISABLED)
            }
        }
    }
}

// endregion

// region Alert Button - Composite Previews

@PreviewComponent
@Composable
private fun AlertButtonShowcase() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        Column(
            modifier = Modifier
                .background(KrailTheme.colors.surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(PREVIEW_TEXT_ALL_SIZES, style = KrailTheme.typography.titleMedium)
            AlertButton(onClick = {}, dimensions = ButtonDefaults.extraSmallButtonSize()) {
                Text(PREVIEW_TEXT_EXTRA_SMALL)
            }
            AlertButton(onClick = {}, dimensions = ButtonDefaults.smallButtonSize()) {
                Text(PREVIEW_TEXT_SMALL)
            }
            AlertButton(onClick = {}, dimensions = ButtonDefaults.mediumButtonSize()) {
                Text(PREVIEW_TEXT_MEDIUM)
            }
            AlertButton(onClick = {}, dimensions = ButtonDefaults.largeButtonSize()) {
                Text(PREVIEW_TEXT_LARGE)
            }

            Spacer(Modifier.height(8.dp))
            Text(PREVIEW_TEXT_STATES, style = KrailTheme.typography.titleMedium)
            AlertButton(
                onClick = {},
                enabled = true,
                dimensions = ButtonDefaults.mediumButtonSize(),
            ) {
                Text(PREVIEW_TEXT_ENABLED)
            }
            AlertButton(
                onClick = {},
                enabled = false,
                dimensions = ButtonDefaults.mediumButtonSize(),
            ) {
                Text(PREVIEW_TEXT_DISABLED)
            }
        }
    }
}

// endregion

// ═══════════════════════════════════════════════════════════════════════════
// SNAPSHOT TEST PREVIEWS - For Automated Testing (Add @ScreenshotTest as needed)
// ═══════════════════════════════════════════════════════════════════════════

// region Primary Button - Theme Variations (Medium Size)

@ScreenshotTest
@PreviewComponent
@Composable
private fun PrimaryButtonTrain() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        Button(onClick = {}, dimensions = ButtonDefaults.mediumButtonSize()) {
            Text("Train")
        }
    }
}

@ScreenshotTest
@PreviewComponent
@Composable
private fun PrimaryButtonMetro() {
    PreviewTheme(themeStyle = KrailThemeStyle.Metro) {
        Button(onClick = {}, dimensions = ButtonDefaults.mediumButtonSize()) {
            Text("Metro")
        }
    }
}

@ScreenshotTest
@PreviewComponent
@Composable
private fun PrimaryButtonBus() {
    PreviewTheme(themeStyle = KrailThemeStyle.Bus) {
        Button(onClick = {}, dimensions = ButtonDefaults.mediumButtonSize()) {
            Text("Bus")
        }
    }
}

@ScreenshotTest
@PreviewComponent
@Composable
private fun PrimaryButtonPurpleDrip() {
    PreviewTheme(themeStyle = KrailThemeStyle.PurpleDrip) {
        Button(onClick = {}, dimensions = ButtonDefaults.mediumButtonSize()) {
            Text("PurpleDrip")
        }
    }
}

@ScreenshotTest
@PreviewComponent
@Composable
private fun PrimaryButtonFerry() {
    PreviewTheme(themeStyle = KrailThemeStyle.Ferry) {
        Button(onClick = {}, dimensions = ButtonDefaults.mediumButtonSize()) {
            Text("Ferry")
        }
    }
}

@ScreenshotTest
@PreviewComponent
@Composable
private fun PrimaryButtonBarbiePink() {
    PreviewTheme(themeStyle = KrailThemeStyle.BarbiePink) {
        Button(onClick = {}, dimensions = ButtonDefaults.mediumButtonSize()) {
            Text("BarbiePink")
        }
    }
}

// endregion

// region Other Button Types - Default Theme (Medium Size)

@ScreenshotTest
@PreviewComponent
@Composable
private fun SubtleButtonMedium() {
    PreviewTheme(themeStyle = KrailThemeStyle.Bus) {
        SubtleButton(onClick = {}, dimensions = ButtonDefaults.mediumButtonSize()) {
            Text("Subtle")
        }
    }
}

@ScreenshotTest
@PreviewComponent
@Composable
private fun TextButtonMedium() {
    PreviewTheme(themeStyle = KrailThemeStyle.Metro) {
        TextButton(onClick = {}, dimensions = ButtonDefaults.mediumButtonSize()) {
            Text("Text")
        }
    }
}

@ScreenshotTest
@PreviewComponent
@Composable
private fun AlertButtonMedium() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        AlertButton(onClick = {}, dimensions = ButtonDefaults.mediumButtonSize()) {
            Text("Alert")
        }
    }
}

// endregion

// region Disabled States

@ScreenshotTest
@PreviewComponent
@Composable
private fun PrimaryButtonDisabled() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        Button(onClick = {}, enabled = false, dimensions = ButtonDefaults.mediumButtonSize()) {
            Text(PREVIEW_TEXT_DISABLED)
        }
    }
}

@ScreenshotTest
@PreviewComponent
@Composable
private fun SubtleButtonDisabled() {
    PreviewTheme(themeStyle = KrailThemeStyle.Bus) {
        SubtleButton(
            onClick = {},
            enabled = false,
            dimensions = ButtonDefaults.mediumButtonSize(),
        ) {
            Text(PREVIEW_TEXT_DISABLED)
        }
    }
}

@ScreenshotTest
@PreviewComponent
@Composable
private fun TextButtonDisabled() {
    PreviewTheme(themeStyle = KrailThemeStyle.Metro) {
        TextButton(onClick = {}, enabled = false, dimensions = ButtonDefaults.mediumButtonSize()) {
            Text(PREVIEW_TEXT_DISABLED)
        }
    }
}

@ScreenshotTest
@PreviewComponent
@Composable
private fun AlertButtonDisabled() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        AlertButton(onClick = {}, enabled = false, dimensions = ButtonDefaults.mediumButtonSize()) {
            Text(PREVIEW_TEXT_DISABLED)
        }
    }
}

// endregion
