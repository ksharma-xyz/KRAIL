package xyz.ksharma.krail.taj.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import xyz.ksharma.krail.taj.LocalContainerColor
import xyz.ksharma.krail.taj.LocalTextColor
import xyz.ksharma.krail.taj.LocalTextStyle
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.tokens.IconSizeTokens
import xyz.ksharma.krail.taj.tokens.SpacingTokens

@Composable
fun SheetTitleBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = IconSizeTokens.FAB)
            .padding(end = SpacingTokens.XL, start = SpacingTokens.M)
            .padding(vertical = SpacingTokens.XS),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(start = SpacingTokens.ML),
        ) {
            CompositionLocalProvider(
                LocalTextColor provides KrailTheme.colors.onSurface,
                LocalTextStyle provides KrailTheme.typography.headlineMedium,
            ) {
                title()
            }
        }
        actions?.let {
            Row(
                modifier = Modifier.padding(start = SpacingTokens.XL),
                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.M),
            ) {
                CompositionLocalProvider(
                    LocalContainerColor provides KrailTheme.colors.onSurface,
                ) {
                    actions()
                }
            }
        }
    }
}
