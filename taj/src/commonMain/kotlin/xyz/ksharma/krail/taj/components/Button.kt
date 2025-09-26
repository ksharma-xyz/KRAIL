package xyz.ksharma.krail.taj.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
                    }
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
                    }
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
                    }
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
        customContentColor: Color? = null
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
                    containerColor
                )
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

// region Previews Button

@Composable
@Preview(name = "Primary Button - Light")
fun PreviewPrimaryButtonLight() {
    PreviewTheme(themeStyle = KrailThemeStyle.PurpleDrip, darkTheme = false) {
        Button(onClick = {}) { Text("Button") }
    }
}

@Composable
@Preview(name = "Primary Button - Dark")
fun PreviewPrimaryButtonDark() {
    PreviewTheme(themeStyle = KrailThemeStyle.Metro, darkTheme = true) {
        Button(onClick = {}) { Text("Button") }
    }
}

@Composable
@Preview(name = "Primary Button Dimensions - Light")
fun PreviewPrimaryButtonDimensionsLight() {
    PreviewTheme(themeStyle = KrailThemeStyle.PurpleDrip, darkTheme = false) {
        Column(modifier = Modifier.padding(16.dp)) {
            Button(onClick = {}, dimensions = ButtonDefaults.extraSmallButtonSize()) { Text("Extra Small") }
            Spacer(Modifier.height(8.dp))
            Button(onClick = {}, dimensions = ButtonDefaults.smallButtonSize()) { Text("Small") }
            Spacer(Modifier.height(8.dp))
            Button(onClick = {}, dimensions = ButtonDefaults.mediumButtonSize()) { Text("Medium") }
            Spacer(Modifier.height(8.dp))
            Button(onClick = {}, dimensions = ButtonDefaults.largeButtonSize()) { Text("Large") }
        }
    }
}

@Composable
@Preview(name = "Primary Button Dimensions - Dark")
fun PreviewPrimaryButtonDimensionsDark() {
    PreviewTheme(themeStyle = KrailThemeStyle.Metro, darkTheme = true) {
        Column(modifier = Modifier.padding(16.dp)) {
            Button(onClick = {}, dimensions = ButtonDefaults.extraSmallButtonSize()) { Text("Extra Small") }
            Spacer(Modifier.height(8.dp))
            Button(onClick = {}, dimensions = ButtonDefaults.smallButtonSize()) { Text("Small") }
            Spacer(Modifier.height(8.dp))
            Button(onClick = {}, dimensions = ButtonDefaults.mediumButtonSize()) { Text("Medium") }
            Spacer(Modifier.height(8.dp))
            Button(onClick = {}, dimensions = ButtonDefaults.largeButtonSize()) { Text("Large") }
        }
    }
}

@Composable
@Preview(name = "Primary Button Disabled - Light")
fun PreviewPrimaryButtonDisabledLight() {
    PreviewTheme(themeStyle = KrailThemeStyle.PurpleDrip, darkTheme = false) {
        Button(onClick = {}, enabled = false) { Text("Disabled Button") }
    }
}

@Composable
@Preview(name = "Primary Button Disabled - Dark")
fun PreviewPrimaryButtonDisabledDark() {
    PreviewTheme(themeStyle = KrailThemeStyle.Metro, darkTheme = true) {
        Button(onClick = {}, enabled = false) { Text("Disabled Button") }
    }
}

// endregion

// region Previews SubtleButton

@Composable
@Preview(name = "Subtle Button - Light")
fun PreviewSubtleButtonLight() {
    PreviewTheme(themeStyle = KrailThemeStyle.Bus, darkTheme = false) {
        SubtleButton(onClick = {}) { Text("Subtle") }
    }
}

@Composable
@Preview(name = "Subtle Button - Dark")
fun PreviewSubtleButtonDark() {
    PreviewTheme(themeStyle = KrailThemeStyle.PurpleDrip, darkTheme = true) {
        SubtleButton(onClick = {}) { Text("Subtle") }
    }
}

@Composable
@Preview(name = "Subtle Button Dimensions - Light")
fun PreviewSubtleButtonDimensionsLight() {
    PreviewTheme(themeStyle = KrailThemeStyle.Bus, darkTheme = false) {
        Column(modifier = Modifier.padding(16.dp)) {
            SubtleButton(onClick = {}, dimensions = ButtonDefaults.extraSmallButtonSize()) { Text("Extra Small") }
            Spacer(Modifier.height(8.dp))
            SubtleButton(onClick = {}, dimensions = ButtonDefaults.smallButtonSize()) { Text("Small") }
            Spacer(Modifier.height(8.dp))
            SubtleButton(onClick = {}, dimensions = ButtonDefaults.mediumButtonSize()) { Text("Medium") }
            Spacer(Modifier.height(8.dp))
            SubtleButton(onClick = {}, dimensions = ButtonDefaults.largeButtonSize()) { Text("Large") }
        }
    }
}

@Composable
@Preview(name = "Subtle Button Dimensions - Dark")
fun PreviewSubtleButtonDimensionsDark() {
    PreviewTheme(themeStyle = KrailThemeStyle.Ferry, darkTheme = true) {
        Column(modifier = Modifier.padding(16.dp)) {
            SubtleButton(onClick = {}, dimensions = ButtonDefaults.extraSmallButtonSize()) { Text("Extra Small") }
            Spacer(Modifier.height(8.dp))
            SubtleButton(onClick = {}, dimensions = ButtonDefaults.smallButtonSize()) { Text("Small") }
            Spacer(Modifier.height(8.dp))
            SubtleButton(onClick = {}, dimensions = ButtonDefaults.mediumButtonSize()) { Text("Medium") }
            Spacer(Modifier.height(8.dp))
            SubtleButton(onClick = {}, dimensions = ButtonDefaults.largeButtonSize()) { Text("Large") }
        }
    }
}

@Composable
@Preview(name = "Subtle Button Disabled - Light")
fun PreviewSubtleButtonDisabledLight() {
    PreviewTheme(themeStyle = KrailThemeStyle.Bus, darkTheme = false) {
        SubtleButton(onClick = {}, enabled = false) { Text("Disabled Subtle") }
    }
}

@Composable
@Preview(name = "Subtle Button Disabled - Dark")
fun PreviewSubtleButtonDisabledDark() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train, darkTheme = true) {
        SubtleButton(onClick = {}, enabled = false) { Text("Disabled Subtle") }
    }
}
// endregion

// region Previews TextButton

@Composable
@Preview(name = "Text Button - Light")
fun PreviewTextButtonLight() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train, darkTheme = false) {
        TextButton(onClick = {}) { Text("Text Button") }
    }
}

@Composable
@Preview(name = "Text Button - Dark")
fun PreviewTextButtonDark() {
    PreviewTheme(themeStyle = KrailThemeStyle.Metro, darkTheme = true) {
        TextButton(onClick = {}) { Text("Text Button") }
    }
}

@Composable
@Preview(name = "Text Button Dimensions - Light")
fun PreviewTextButtonDimensionsLight() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train, darkTheme = false) {
        Column(modifier = Modifier.padding(16.dp)) {
            TextButton(onClick = {}, dimensions = ButtonDefaults.extraSmallButtonSize()) { Text("Extra Small") }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = {}, dimensions = ButtonDefaults.smallButtonSize()) { Text("Small") }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = {}, dimensions = ButtonDefaults.mediumButtonSize()) { Text("Medium") }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = {}, dimensions = ButtonDefaults.largeButtonSize()) { Text("Large") }
        }
    }
}

@Composable
@Preview(name = "Text Button Dimensions - Dark")
fun PreviewTextButtonDimensionsDark() {
    PreviewTheme(themeStyle = KrailThemeStyle.Ferry, darkTheme = true) {
        Column(modifier = Modifier.padding(16.dp)) {
            TextButton(onClick = {}, dimensions = ButtonDefaults.extraSmallButtonSize()) { Text("Extra Small") }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = {}, dimensions = ButtonDefaults.smallButtonSize()) { Text("Small") }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = {}, dimensions = ButtonDefaults.mediumButtonSize()) { Text("Medium") }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = {}, dimensions = ButtonDefaults.largeButtonSize()) { Text("Large") }
        }
    }
}

@Composable
@Preview(name = "Text Button Disabled - Light")
fun PreviewTextButtonDisabledLight() {
    PreviewTheme(themeStyle = KrailThemeStyle.Metro, darkTheme = false) {
        TextButton(onClick = {}, enabled = false) { Text("Disabled Text") }
    }
}

@Composable
@Preview(name = "Text Button Disabled - Dark")
fun PreviewTextButtonDisabledDark() {
    PreviewTheme(themeStyle = KrailThemeStyle.Bus, darkTheme = true) {
        TextButton(onClick = {}, enabled = false) { Text("Disabled Text") }
    }
}

// endregion

// region Previews AlertButton

@Composable
@Preview(name = "Alert Button - Light")
fun PreviewAlertButtonLight() {
    PreviewTheme(themeStyle = KrailThemeStyle.Bus, darkTheme = false) {
        AlertButton(onClick = {}) { Text("Alert Button") }
    }
}

@Composable
@Preview(name = "Alert Button - Dark")
fun PreviewAlertButtonDark() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train, darkTheme = true) {
        AlertButton(onClick = {}) { Text("Alert Button") }
    }
}

@Composable
@Preview(name = "Alert Button Dimensions - Light")
fun PreviewAlertButtonDimensionsLight() {
    PreviewTheme(themeStyle = KrailThemeStyle.BarbiePink, darkTheme = false) {
        Column(modifier = Modifier.padding(16.dp)) {
            AlertButton(onClick = {}, dimensions = ButtonDefaults.extraSmallButtonSize()) { Text("Extra Small") }
            Spacer(Modifier.height(8.dp))
            AlertButton(onClick = {}, dimensions = ButtonDefaults.smallButtonSize()) { Text("Small") }
            Spacer(Modifier.height(8.dp))
            AlertButton(onClick = {}, dimensions = ButtonDefaults.mediumButtonSize()) { Text("Medium") }
            Spacer(Modifier.height(8.dp))
            AlertButton(onClick = {}, dimensions = ButtonDefaults.largeButtonSize()) { Text("Large") }
        }
    }
}

@Composable
@Preview(name = "Alert Button Dimensions - Dark")
fun PreviewAlertButtonDimensionsDark() {
    PreviewTheme(themeStyle = KrailThemeStyle.Bus, darkTheme = true) {
        Column(modifier = Modifier.padding(16.dp)) {
            AlertButton(onClick = {}, dimensions = ButtonDefaults.extraSmallButtonSize()) { Text("Extra Small") }
            Spacer(Modifier.height(8.dp))
            AlertButton(onClick = {}, dimensions = ButtonDefaults.smallButtonSize()) { Text("Small") }
            Spacer(Modifier.height(8.dp))
            AlertButton(onClick = {}, dimensions = ButtonDefaults.mediumButtonSize()) { Text("Medium") }
            Spacer(Modifier.height(8.dp))
            AlertButton(onClick = {}, dimensions = ButtonDefaults.largeButtonSize()) { Text("Large") }
        }
    }
}

@Composable
@Preview(name = "Alert Button Disabled - Light")
fun PreviewAlertButtonDisabledLight() {
    PreviewTheme(themeStyle = KrailThemeStyle.PurpleDrip, darkTheme = false) {
        AlertButton(onClick = {}, enabled = false) { Text("Disabled Alert") }
    }
}

@Composable
@Preview(name = "Alert Button Disabled - Dark")
fun PreviewAlertButtonDisabledDark() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train, darkTheme = true) {
        AlertButton(onClick = {}, enabled = false) { Text("Disabled Alert") }
    }
}

// endregion AlertButton
