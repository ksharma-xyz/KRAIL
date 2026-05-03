@file:Suppress("MatchingDeclarationName")

package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.taj.LocalContentAlpha
import xyz.ksharma.krail.taj.LocalTextColor
import xyz.ksharma.krail.taj.LocalTextStyle
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.tokens.ContentAlphaTokens

/**
 * Sizes available for [TransportModeBadge].
 *
 * - [Small] — default, used inside departure rows (tight padding, no min-width constraint).
 * - [Large] — ~1.5× larger, used in the "Services at this stop" filter row where the badge
 *   must also meet accessibility touch-target guidelines (min width 44 dp).
 */
enum class BadgeSize { Small, Large }

private val badgeShape = RoundedCornerShape(percent = 20)
private val BADGE_LARGE_VERTICAL_PADDING = 7.dp

/**
 * Coloured pill badge showing a transport line number (e.g. "T1", "333", "F1").
 *
 * When [selected] is `true` the badge spring-bounces to 1.15× scale so it "pops" into focus.
 *
 * @param badgeText       Short line identifier shown on the badge.
 * @param backgroundColor Hex-derived colour for the badge background.
 * @param size            [BadgeSize.Small] (default) or [BadgeSize.Large] for filter rows.
 * @param selected        When `true`, triggers the scale animation.
 * @param onClick         Optional tap handler. When non-null the badge is clickable.
 */
@Composable
fun TransportModeBadge(
    badgeText: String,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    size: BadgeSize = BadgeSize.Small,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val dim = KrailTheme.dimensions
    val paddingValues: PaddingValues = when (size) {
        BadgeSize.Small -> PaddingValues(horizontal = dim.spacingXS, vertical = dim.spacingXXS)
        BadgeSize.Large -> PaddingValues(horizontal = dim.spacingML, vertical = BADGE_LARGE_VERTICAL_PADDING)
    }

    // Scale: spring-bounced so selection "pops" with a satisfying physical feel.
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "badge-scale",
    )

    CompositionLocalProvider(
        LocalTextColor provides Color.White,
        LocalTextStyle provides KrailTheme.typography.titleMedium,
        LocalContentAlpha provides ContentAlphaTokens.EnabledContentAlpha,
    ) {
        Box(
            modifier = modifier
                .then(if (size == BadgeSize.Large) Modifier.widthIn(min = dim.iconXXL) else Modifier)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(badgeShape)
                .background(backgroundColor)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            indication = null,
                            interactionSource = null,
                            onClick = onClick,
                        )
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = badgeText,
                color = Color.White,
                modifier = Modifier
                    .padding(paddingValues)
                    .wrapContentWidth(),
            )
        }
    }
}

// region Previews

@ScreenshotTest
@Preview
@Composable
private fun TransportModeBadgeBusPreview() {
    PreviewTheme {
        TransportModeBadge(
            badgeText = "700",
            backgroundColor = "00B5EF".hexToComposeColor(),
        )
    }
}

@ScreenshotTest
@Preview
@Composable
private fun TransportModeBadgeTrainPreview() {
    PreviewTheme {
        TransportModeBadge(
            badgeText = "T1",
            backgroundColor = "#F6891F".hexToComposeColor(),
        )
    }
}

@ScreenshotTest
@PreviewComponent
@Composable
private fun TransportModeBadgeLargePreview() {
    PreviewTheme {
        TransportModeBadge(
            badgeText = "T1",
            backgroundColor = "#F99D1C".hexToComposeColor(),
            size = BadgeSize.Large,
        )
    }
}

@ScreenshotTest
@PreviewComponent
@Composable
private fun TransportModeBadgeLargeSelectedPreview() {
    PreviewTheme {
        TransportModeBadge(
            badgeText = "T1",
            backgroundColor = "#F99D1C".hexToComposeColor(),
            size = BadgeSize.Large,
            selected = true,
            onClick = {},
        )
    }
}

// endregion
