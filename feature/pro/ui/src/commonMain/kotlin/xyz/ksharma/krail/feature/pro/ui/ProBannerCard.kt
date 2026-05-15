package xyz.ksharma.krail.feature.pro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme

/**
 * Full-width gradient card shown at the top of Settings.
 * Taps open the Pro upgrade screen.
 */
@Composable
fun ProBannerCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val themeColorHex by LocalThemeColor.current
    val themeColor = themeColorHex.hexToComposeColor()
    val dim = KrailTheme.dimensions

    Row(
        modifier = modifier
            .padding(horizontal = dim.pageHorizontalPadding, vertical = dim.spacingL)
            .fillMaxWidth()
            .clip(RoundedCornerShape(dim.radiusXL))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        themeColor.copy(alpha = 0.15f),
                        themeColor.copy(alpha = 0.08f),
                    ),
                ),
            )
            .klickable { onClick() }
            .padding(horizontal = dim.cardHorizontalPadding, vertical = dim.cardVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(dim.spacingXXS),
            modifier = Modifier.weight(1f),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(dim.spacingS),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "★",
                    style = KrailTheme.typography.titleMedium,
                    color = themeColor,
                )
                Text(
                    text = "KRAIL Pro",
                    style = KrailTheme.typography.titleMedium,
                )
            }
            Text(
                text = "Unlock the full experience",
                style = KrailTheme.typography.bodyMedium,
                color = KrailTheme.colors.softLabel,
            )
        }

        Text(
            text = "›",
            style = KrailTheme.typography.titleLarge,
            color = themeColor,
            modifier = Modifier.padding(start = dim.spacingM),
        )
    }
}
