package xyz.ksharma.krail.taj.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
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
import xyz.ksharma.krail.taj.theme.getForegroundColor
import xyz.ksharma.krail.taj.theme.krailRipple
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
    content: @Composable () -> Unit, // RowScope and place
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
                .clip(dimensions.shape)
                .background(
                    color = Color.Transparent,
                    shape = dimensions.shape,
                ).klickable(
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
        ButtonDefaults.mediumButtonSize() -> KrailTheme.typography.bodyMedium
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
        val hexContainerColor: String by rememberSaveable(hexThemeColor) {
            mutableStateOf(hexThemeColor)
        }
        val defaultContainerColor: Color by remember(hexContainerColor) {
            mutableStateOf(hexContainerColor.hexToComposeColor())
        }
        val containerColor = customContainerColor ?: defaultContainerColor
        val defaultContentColor: Color by remember { mutableStateOf(getForegroundColor(containerColor)) }
        val contentColor = customContentColor ?: defaultContentColor

        return ButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = DisabledContentAlpha),
            disabledContentColor = contentColor.copy(alpha = DisabledContentAlpha),
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
