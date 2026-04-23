package xyz.ksharma.krail.taj.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.toAdaptiveSize
import xyz.ksharma.krail.taj.tokens.SpacingTokens

@Composable
fun SeparatorIcon(modifier: Modifier = Modifier, color: Color = KrailTheme.colors.onSurface) {
    Box(
        modifier = modifier
            .size(SpacingTokens.XS.toAdaptiveSize())
            .clip(CircleShape)
            .background(color = color)
            .padding(end = SpacingTokens.M),
    )
}

// region Previews

@Composable
private fun SeparatorIconPreview() {
    PreviewTheme {
        Box(
            modifier = Modifier
                .background(KrailTheme.colors.surface)
                .padding(SpacingTokens.ML),
        ) {
            SeparatorIcon()
        }
    }
}

// endregion
