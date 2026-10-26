package xyz.ksharma.krail.trip.planner.ui.components.crowding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme

/**
 * "Want a seat? The next T1, at 8:59am from Platform 1, has seats available and is due at
 * Central at 9:26am." Informs, never instructs: the service gets the verb and the rider chooses.
 * Tapping opens the card it names. Data from a response the screen already has, so it has no
 * loading state of its own: present or absent.
 */
@Composable
internal fun NextServiceLine(text: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Text(
        text = text,
        style = KrailTheme.typography.bodyMedium,
        color = KrailTheme.colors.onSurface,
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(vertical = KrailTheme.dimensions.spacingS),
    )
}

// region Previews

@ScreenshotTest
@PreviewComponent
@Composable
private fun Preview_NextServiceLine_Train() {
    PreviewTheme {
        NextServiceLine(
            text = "Want a seat? The next T1, at 8:59am from Platform 1, has seats available and is due " +
                "at Central at 9:26am.",
            modifier = Modifier.padding(KrailTheme.dimensions.spacingL),
            onClick = {},
        )
    }
}

// endregion
