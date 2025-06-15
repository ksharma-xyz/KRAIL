package xyz.ksharma.krail.taj.modifier

import androidx.compose.foundation.Indication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import xyz.ksharma.krail.taj.theme.krailRipple
import xyz.ksharma.krail.taj.themeColor

/**
 * Adds a click listener to the Modifier with a custom ripple effect.
 * This method is used when you need a custom ripple effect with a clickable element.
 *
 * @param role The role of the clickable element.
 * @param enabled Whether the clickable element is enabled.
 * @param onClick The callback to be invoked when the element is clicked.

 * @return The modified Modifier with the click listener and custom ripple effect.
 */
@Composable
fun Modifier.klickable(
    role: Role = Role.Button,
    enabled: Boolean = true,
    indication: Indication? = krailRipple(color = themeColor()),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onClick: () -> Unit,
): Modifier {
    return this.clickable(
        role = role,
        interactionSource = interactionSource,
        enabled = enabled,
        indication = indication,
        onClick = onClick,
    )
}

@Composable
fun Modifier.scalingKlickable(
    role: Role = Role.Button,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onClick: () -> Unit,
): Modifier {
    return this.clickable(
        role = role,
        interactionSource = interactionSource,
        enabled = enabled,
        indication = ScalingIndication,
        onClick = onClick,
    )
}
