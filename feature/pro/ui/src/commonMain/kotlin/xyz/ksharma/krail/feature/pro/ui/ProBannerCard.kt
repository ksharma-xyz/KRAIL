package xyz.ksharma.krail.feature.pro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme

/**
 * Compact settings-row entry point for the Pro upgrade screen.
 */
@Composable
fun ProBannerCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isProActive: Boolean = false,
) {
    val themeColorHex by LocalThemeColor.current
    val themeColor = themeColorHex.hexToComposeColor()
    val dim = KrailTheme.dimensions

    Row(
        modifier = modifier
            .fillMaxWidth()
            .klickable { onClick() }
            .padding(horizontal = dim.pageHorizontalPadding, vertical = dim.spacingM),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dim.spacingM),
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFE0218A))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "PRO",
                    style = TextStyle(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                        color = Color.White,
                    ),
                )
            }
            Text(
                text = "KRAIL Pro",
                style = KrailTheme.typography.titleMedium,
            )
        }

        if (isProActive) {
            Text(
                text = "Active",
                style = KrailTheme.typography.labelMedium,
                color = Color(0xFFE0218A),
            )
        } else {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(themeColor.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            ) {
                Text(
                    text = "Go Pro",
                    style = KrailTheme.typography.labelMedium,
                    color = themeColor,
                )
            }
        }
    }
}
