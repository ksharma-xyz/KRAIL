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
import org.jetbrains.compose.ui.tooling.preview.Preview
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

// ═══════════════════════════════════════════════════════════════════════════
// COMPOSITE PREVIEWS - For Visual Design Review in IDE
// ═══════════════════════════════════════════════════════════════════════════

// region Primary Button - Composite Previews

@Preview(name = "Primary Button Showcase Light", group = "Design Review")
@Composable
private fun PrimaryButtonShowcaseLight() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train, darkTheme = false) {
        Column(
            modifier = Modifier
                .background(KrailTheme.colors.surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("All Sizes", style = KrailTheme.typography.titleMedium)
            Button(onClick = {}, dimensions = ButtonDefaults.extraSmallButtonSize()) {
                Text("Extra Small")
            }
            Button(onClick = {}, dimensions = ButtonDefaults.smallButtonSize()) {
                Text("Small")
            }
            Button(onClick = {}, dimensions = ButtonDefaults.mediumButtonSize()) {
                Text("Medium")
            }
            Button(onClick = {}, dimensions = ButtonDefaults.largeButtonSize()) {
                Text("Large")
            }

            Spacer(Modifier.height(8.dp))
            Text("States", style = KrailTheme.typography.titleMedium)
            Button(onClick = {}, enabled = true, dimensions = ButtonDefaults.mediumButtonSize()) {
                Text("Enabled")
            }
            Button(onClick = {}, enabled = false, dimensions = ButtonDefaults.mediumButtonSize()) {
                Text("Disabled")
            }
        }
    }
}

@Preview(name = "Primary Button Showcase Dark", group = "Design Review")
@Composable
private fun PrimaryButtonShowcaseDark() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train, darkTheme = true) {
        Column(
            modifier = Modifier
                .background(KrailTheme.colors.surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("All Sizes", style = KrailTheme.typography.titleMedium)
            Button(onClick = {}, dimensions = ButtonDefaults.extraSmallButtonSize()) {
                Text("Extra Small")
            }
            Button(onClick = {}, dimensions = ButtonDefaults.smallButtonSize()) {
                Text("Small")
            }
            Button(onClick = {}, dimensions = ButtonDefaults.mediumButtonSize()) {
                Text("Medium")
            }
            Button(onClick = {}, dimensions = ButtonDefaults.largeButtonSize()) {
                Text("Large")
            }

            Spacer(Modifier.height(8.dp))
            Text("States", style = KrailTheme.typography.titleMedium)
            Button(onClick = {}, enabled = true, dimensions = ButtonDefaults.mediumButtonSize()) {
                Text("Enabled")
            }
            Button(onClick = {}, enabled = false, dimensions = ButtonDefaults.mediumButtonSize()) {
                Text("Disabled")
            }
        }
    }
}

// endregion

// region Subtle Button - Composite Previews

@Preview(name = "Subtle Button Showcase Light", group = "Design Review")
@Composable
private fun SubtleButtonShowcaseLight() {
    PreviewTheme(themeStyle = KrailThemeStyle.Bus, darkTheme = false) {
        Column(
            modifier = Modifier
                .background(KrailTheme.colors.surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("All Sizes", style = KrailTheme.typography.titleMedium)
            SubtleButton(onClick = {}, dimensions = ButtonDefaults.extraSmallButtonSize()) {
                Text("Extra Small")
            }
            SubtleButton(onClick = {}, dimensions = ButtonDefaults.smallButtonSize()) {
                Text("Small")
            }
            SubtleButton(onClick = {}, dimensions = ButtonDefaults.mediumButtonSize()) {
                Text("Medium")
            }
            SubtleButton(onClick = {}, dimensions = ButtonDefaults.largeButtonSize()) {
                Text("Large")
            }

            Spacer(Modifier.height(8.dp))
            Text("States", style = KrailTheme.typography.titleMedium)
            SubtleButton(
                onClick = {},
                enabled = true,
                dimensions = ButtonDefaults.mediumButtonSize()
            ) {
                Text("Enabled")
            }
            SubtleButton(
                onClick = {},
                enabled = false,
                dimensions = ButtonDefaults.mediumButtonSize()
            ) {
                Text("Disabled")
            }
        }
    }
}

@Preview(name = "Subtle Button Showcase Dark", group = "Design Review")
@Composable
private fun SubtleButtonShowcaseDark() {
    PreviewTheme(themeStyle = KrailThemeStyle.Bus, darkTheme = true) {
        Column(
            modifier = Modifier
                .background(KrailTheme.colors.surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("All Sizes", style = KrailTheme.typography.titleMedium)
            SubtleButton(onClick = {}, dimensions = ButtonDefaults.extraSmallButtonSize()) {
                Text("Extra Small")
            }
            SubtleButton(onClick = {}, dimensions = ButtonDefaults.smallButtonSize()) {
                Text("Small")
            }
            SubtleButton(onClick = {}, dimensions = ButtonDefaults.mediumButtonSize()) {
                Text("Medium")
            }
            SubtleButton(onClick = {}, dimensions = ButtonDefaults.largeButtonSize()) {
                Text("Large")
            }

            Spacer(Modifier.height(8.dp))
            Text("States", style = KrailTheme.typography.titleMedium)
            SubtleButton(
                onClick = {},
                enabled = true,
                dimensions = ButtonDefaults.mediumButtonSize()
            ) {
                Text("Enabled")
            }
            SubtleButton(
                onClick = {},
                enabled = false,
                dimensions = ButtonDefaults.mediumButtonSize()
            ) {
                Text("Disabled")
            }
        }
    }
}

// endregion

// region Text Button - Composite Previews

@Preview(name = "Text Button Showcase Light", group = "Design Review")
@Composable
private fun TextButtonShowcaseLight() {
    PreviewTheme(themeStyle = KrailThemeStyle.Metro, darkTheme = false) {
        Column(
            modifier = Modifier
                .background(KrailTheme.colors.surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("All Sizes", style = KrailTheme.typography.titleMedium)
            TextButton(onClick = {}, dimensions = ButtonDefaults.extraSmallButtonSize()) {
                Text("Extra Small")
            }
            TextButton(onClick = {}, dimensions = ButtonDefaults.smallButtonSize()) {
                Text("Small")
            }
            TextButton(onClick = {}, dimensions = ButtonDefaults.mediumButtonSize()) {
                Text("Medium")
            }
            TextButton(onClick = {}, dimensions = ButtonDefaults.largeButtonSize()) {
                Text("Large")
            }

            Spacer(Modifier.height(8.dp))
            Text("States", style = KrailTheme.typography.titleMedium)
            TextButton(
                onClick = {},
                enabled = true,
                dimensions = ButtonDefaults.mediumButtonSize()
            ) {
                Text("Enabled")
            }
            TextButton(
                onClick = {},
                enabled = false,
                dimensions = ButtonDefaults.mediumButtonSize()
            ) {
                Text("Disabled")
            }
        }
    }
}

@Preview(name = "Text Button Showcase Dark", group = "Design Review")
@Composable
private fun TextButtonShowcaseDark() {
    PreviewTheme(themeStyle = KrailThemeStyle.Metro, darkTheme = true) {
        Column(
            modifier = Modifier
                .background(KrailTheme.colors.surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("All Sizes", style = KrailTheme.typography.titleMedium)
            TextButton(onClick = {}, dimensions = ButtonDefaults.extraSmallButtonSize()) {
                Text("Extra Small")
            }
            TextButton(onClick = {}, dimensions = ButtonDefaults.smallButtonSize()) {
                Text("Small")
            }
            TextButton(onClick = {}, dimensions = ButtonDefaults.mediumButtonSize()) {
                Text("Medium")
            }
            TextButton(onClick = {}, dimensions = ButtonDefaults.largeButtonSize()) {
                Text("Large")
            }

            Spacer(Modifier.height(8.dp))
            Text("States", style = KrailTheme.typography.titleMedium)
            TextButton(
                onClick = {},
                enabled = true,
                dimensions = ButtonDefaults.mediumButtonSize()
            ) {
                Text("Enabled")
            }
            TextButton(
                onClick = {},
                enabled = false,
                dimensions = ButtonDefaults.mediumButtonSize()
            ) {
                Text("Disabled")
            }
        }
    }
}

// endregion

// region Alert Button - Composite Previews

@Preview(name = "Alert Button Showcase Light", group = "Design Review")
@Composable
private fun AlertButtonShowcaseLight() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train, darkTheme = false) {
        Column(
            modifier = Modifier
                .background(KrailTheme.colors.surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("All Sizes", style = KrailTheme.typography.titleMedium)
            AlertButton(onClick = {}, dimensions = ButtonDefaults.extraSmallButtonSize()) {
                Text("Extra Small")
            }
            AlertButton(onClick = {}, dimensions = ButtonDefaults.smallButtonSize()) {
                Text("Small")
            }
            AlertButton(onClick = {}, dimensions = ButtonDefaults.mediumButtonSize()) {
                Text("Medium")
            }
            AlertButton(onClick = {}, dimensions = ButtonDefaults.largeButtonSize()) {
                Text("Large")
            }

            Spacer(Modifier.height(8.dp))
            Text("States", style = KrailTheme.typography.titleMedium)
            AlertButton(
                onClick = {},
                enabled = true,
                dimensions = ButtonDefaults.mediumButtonSize()
            ) {
                Text("Enabled")
            }
            AlertButton(
                onClick = {},
                enabled = false,
                dimensions = ButtonDefaults.mediumButtonSize()
            ) {
                Text("Disabled")
            }
        }
    }
}

@Preview(name = "Alert Button Showcase Dark", group = "Design Review")
@Composable
private fun AlertButtonShowcaseDark() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train, darkTheme = true) {
        Column(
            modifier = Modifier
                .background(KrailTheme.colors.surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("All Sizes", style = KrailTheme.typography.titleMedium)
            AlertButton(onClick = {}, dimensions = ButtonDefaults.extraSmallButtonSize()) {
                Text("Extra Small")
            }
            AlertButton(onClick = {}, dimensions = ButtonDefaults.smallButtonSize()) {
                Text("Small")
            }
            AlertButton(onClick = {}, dimensions = ButtonDefaults.mediumButtonSize()) {
                Text("Medium")
            }
            AlertButton(onClick = {}, dimensions = ButtonDefaults.largeButtonSize()) {
                Text("Large")
            }

            Spacer(Modifier.height(8.dp))
            Text("States", style = KrailTheme.typography.titleMedium)
            AlertButton(
                onClick = {},
                enabled = true,
                dimensions = ButtonDefaults.mediumButtonSize()
            ) {
                Text("Enabled")
            }
            AlertButton(
                onClick = {},
                enabled = false,
                dimensions = ButtonDefaults.mediumButtonSize()
            ) {
                Text("Disabled")
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
@Preview(name = "Primary Train Light", group = "Snapshot Tests - Primary Themes")
@Composable
private fun PrimaryButtonTrainLight() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train, darkTheme = false) {
        Button(onClick = {}, dimensions = ButtonDefaults.mediumButtonSize()) {
            Text("Train")
        }
    }
}

@ScreenshotTest
@Preview(name = "Primary Train Dark", group = "Snapshot Tests - Primary Themes")
@Composable
private fun PrimaryButtonTrainDark() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train, darkTheme = true) {
        Button(onClick = {}, dimensions = ButtonDefaults.mediumButtonSize()) {
            Text("Train")
        }
    }
}

@ScreenshotTest
@Preview(name = "Primary Metro Light", group = "Snapshot Tests - Primary Themes")
@Composable
private fun PrimaryButtonMetroLight() {
    PreviewTheme(themeStyle = KrailThemeStyle.Metro, darkTheme = false) {
        Button(onClick = {}, dimensions = ButtonDefaults.mediumButtonSize()) {
            Text("Metro")
        }
    }
}

@ScreenshotTest
@Preview(name = "Primary Metro Dark", group = "Snapshot Tests - Primary Themes")
@Composable
private fun PrimaryButtonMetroDark() {
    PreviewTheme(themeStyle = KrailThemeStyle.Metro, darkTheme = true) {
        Button(onClick = {}, dimensions = ButtonDefaults.mediumButtonSize()) {
            Text("Metro")
        }
    }
}

@ScreenshotTest
@Preview(name = "Primary Bus Light", group = "Snapshot Tests - Primary Themes")
@Composable
private fun PrimaryButtonBusLight() {
    PreviewTheme(themeStyle = KrailThemeStyle.Bus, darkTheme = false) {
        Button(onClick = {}, dimensions = ButtonDefaults.mediumButtonSize()) {
            Text("Bus")
        }
    }
}

@ScreenshotTest
@Preview(name = "Primary Bus Dark", group = "Snapshot Tests - Primary Themes")
@Composable
private fun PrimaryButtonBusDark() {
    PreviewTheme(themeStyle = KrailThemeStyle.Bus, darkTheme = true) {
        Button(onClick = {}, dimensions = ButtonDefaults.mediumButtonSize()) {
            Text("Bus")
        }
    }
}

@ScreenshotTest
@Preview(name = "Primary PurpleDrip Light", group = "Snapshot Tests - Primary Themes")
@Composable
private fun PrimaryButtonPurpleDripLight() {
    PreviewTheme(themeStyle = KrailThemeStyle.PurpleDrip, darkTheme = false) {
        Button(onClick = {}, dimensions = ButtonDefaults.mediumButtonSize()) {
            Text("PurpleDrip")
        }
    }
}

@ScreenshotTest
@Preview(name = "Primary PurpleDrip Dark", group = "Snapshot Tests - Primary Themes")
@Composable
private fun PrimaryButtonPurpleDripDark() {
    PreviewTheme(themeStyle = KrailThemeStyle.PurpleDrip, darkTheme = true) {
        Button(onClick = {}, dimensions = ButtonDefaults.mediumButtonSize()) {
            Text("PurpleDrip")
        }
    }
}

@ScreenshotTest
@Preview(name = "Primary Ferry Light", group = "Snapshot Tests - Primary Themes")
@Composable
private fun PrimaryButtonFerryLight() {
    PreviewTheme(themeStyle = KrailThemeStyle.Ferry, darkTheme = false) {
        Button(onClick = {}, dimensions = ButtonDefaults.mediumButtonSize()) {
            Text("Ferry")
        }
    }
}

@ScreenshotTest
@Preview(name = "Primary Ferry Dark", group = "Snapshot Tests - Primary Themes")
@Composable
private fun PrimaryButtonFerryDark() {
    PreviewTheme(themeStyle = KrailThemeStyle.Ferry, darkTheme = true) {
        Button(onClick = {}, dimensions = ButtonDefaults.mediumButtonSize()) {
            Text("Ferry")
        }
    }
}

@ScreenshotTest
@Preview(name = "Primary BarbiePink Light", group = "Snapshot Tests - Primary Themes")
@Composable
private fun PrimaryButtonBarbiePinkLight() {
    PreviewTheme(themeStyle = KrailThemeStyle.BarbiePink, darkTheme = false) {
        Button(onClick = {}, dimensions = ButtonDefaults.mediumButtonSize()) {
            Text("BarbiePink")
        }
    }
}

@ScreenshotTest
@Preview(name = "Primary BarbiePink Dark", group = "Snapshot Tests - Primary Themes")
@Composable
private fun PrimaryButtonBarbiePinkDark() {
    PreviewTheme(themeStyle = KrailThemeStyle.BarbiePink, darkTheme = true) {
        Button(onClick = {}, dimensions = ButtonDefaults.mediumButtonSize()) {
            Text("BarbiePink")
        }
    }
}

// endregion

// region Other Button Types - Default Theme (Medium Size)

@ScreenshotTest
@Preview(name = "Subtle Medium Light", group = "Snapshot Tests - Other Buttons")
@Composable
private fun SubtleButtonMediumLight() {
    PreviewTheme(themeStyle = KrailThemeStyle.Bus, darkTheme = false) {
        SubtleButton(onClick = {}, dimensions = ButtonDefaults.mediumButtonSize()) {
            Text("Subtle")
        }
    }
}

@ScreenshotTest
@Preview(name = "Subtle Medium Dark", group = "Snapshot Tests - Other Buttons")
@Composable
private fun SubtleButtonMediumDark() {
    PreviewTheme(themeStyle = KrailThemeStyle.Bus, darkTheme = true) {
        SubtleButton(onClick = {}, dimensions = ButtonDefaults.mediumButtonSize()) {
            Text("Subtle")
        }
    }
}

@ScreenshotTest
@Preview(name = "Text Medium Light", group = "Snapshot Tests - Other Buttons")
@Composable
private fun TextButtonMediumLight() {
    PreviewTheme(themeStyle = KrailThemeStyle.Metro, darkTheme = false) {
        TextButton(onClick = {}, dimensions = ButtonDefaults.mediumButtonSize()) {
            Text("Text")
        }
    }
}

@ScreenshotTest
@Preview(name = "Text Medium Dark", group = "Snapshot Tests - Other Buttons")
@Composable
private fun TextButtonMediumDark() {
    PreviewTheme(themeStyle = KrailThemeStyle.Metro, darkTheme = true) {
        TextButton(onClick = {}, dimensions = ButtonDefaults.mediumButtonSize()) {
            Text("Text")
        }
    }
}

@ScreenshotTest
@Preview(name = "Alert Medium Light", group = "Snapshot Tests - Other Buttons")
@Composable
private fun AlertButtonMediumLight() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train, darkTheme = false) {
        AlertButton(onClick = {}, dimensions = ButtonDefaults.mediumButtonSize()) {
            Text("Alert")
        }
    }
}

@ScreenshotTest
@Preview(name = "Alert Medium Dark", group = "Snapshot Tests - Other Buttons")
@Composable
private fun AlertButtonMediumDark() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train, darkTheme = true) {
        AlertButton(onClick = {}, dimensions = ButtonDefaults.mediumButtonSize()) {
            Text("Alert")
        }
    }
}

// endregion

// region Disabled States

@ScreenshotTest
@Preview(name = "Primary Disabled Light", group = "Snapshot Tests - Disabled States")
@Composable
private fun PrimaryButtonDisabledLight() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train, darkTheme = false) {
        Button(onClick = {}, enabled = false, dimensions = ButtonDefaults.mediumButtonSize()) {
            Text("Disabled")
        }
    }
}

@ScreenshotTest
@Preview(name = "Primary Disabled Dark", group = "Snapshot Tests - Disabled States")
@Composable
private fun PrimaryButtonDisabledDark() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train, darkTheme = true) {
        Button(onClick = {}, enabled = false, dimensions = ButtonDefaults.mediumButtonSize()) {
            Text("Disabled")
        }
    }
}

@ScreenshotTest
@Preview(name = "Subtle Disabled Light", group = "Snapshot Tests - Disabled States")
@Composable
private fun SubtleButtonDisabledLight() {
    PreviewTheme(themeStyle = KrailThemeStyle.Bus, darkTheme = false) {
        SubtleButton(
            onClick = {},
            enabled = false,
            dimensions = ButtonDefaults.mediumButtonSize()
        ) {
            Text("Disabled")
        }
    }
}

@ScreenshotTest
@Preview(name = "Subtle Disabled Dark", group = "Snapshot Tests - Disabled States")
@Composable
private fun SubtleButtonDisabledDark() {
    PreviewTheme(themeStyle = KrailThemeStyle.Bus, darkTheme = true) {
        SubtleButton(
            onClick = {},
            enabled = false,
            dimensions = ButtonDefaults.mediumButtonSize()
        ) {
            Text("Disabled")
        }
    }
}

@ScreenshotTest
@Preview(name = "Text Disabled Light", group = "Snapshot Tests - Disabled States")
@Composable
private fun TextButtonDisabledLight() {
    PreviewTheme(themeStyle = KrailThemeStyle.Metro, darkTheme = false) {
        TextButton(onClick = {}, enabled = false, dimensions = ButtonDefaults.mediumButtonSize()) {
            Text("Disabled")
        }
    }
}

@ScreenshotTest
@Preview(name = "Text Disabled Dark", group = "Snapshot Tests - Disabled States")
@Composable
private fun TextButtonDisabledDark() {
    PreviewTheme(themeStyle = KrailThemeStyle.Metro, darkTheme = true) {
        TextButton(onClick = {}, enabled = false, dimensions = ButtonDefaults.mediumButtonSize()) {
            Text("Disabled")
        }
    }
}

@ScreenshotTest
@Preview(name = "Alert Disabled Light", group = "Snapshot Tests - Disabled States")
@Composable
private fun AlertButtonDisabledLight() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train, darkTheme = false) {
        AlertButton(onClick = {}, enabled = false, dimensions = ButtonDefaults.mediumButtonSize()) {
            Text("Disabled")
        }
    }
}

@ScreenshotTest
@Preview(name = "Alert Disabled Dark", group = "Snapshot Tests - Disabled States")
@Composable
private fun AlertButtonDisabledDark() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train, darkTheme = true) {
        AlertButton(onClick = {}, enabled = false, dimensions = ButtonDefaults.mediumButtonSize()) {
            Text("Disabled")
        }
    }
}

// endregion
