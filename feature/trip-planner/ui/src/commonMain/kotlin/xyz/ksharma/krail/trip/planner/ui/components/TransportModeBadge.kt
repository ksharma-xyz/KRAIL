@file:Suppress("MatchingDeclarationName")

package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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

// Match InfoTile glow parameters for visual consistency across the app.
private val GLOW_RADIUS = 3.dp
private val GLOW_SPREAD = 1.dp

/**
 * Coloured pill badge showing a transport line number (e.g. "T1", "333", "F1").
 *
 * When [selected] is `true` the badge animates with:
 *  - A spring-bounced scale-up (1.0 → 1.15) so it "pops" into focus.
 *  - A [dropShadow] glow whose colour matches the line's own colour — same technique
 *    used by InfoTile, so the effect is consistent across the app.
 *    The glow alpha fades in/out with a short tween.
 *
 * @param badgeText       Short line identifier shown on the badge.
 * @param backgroundColor Hex-derived colour for the badge background.
 * @param size            [BadgeSize.Small] (default) or [BadgeSize.Large] for filter rows.
 * @param selected        When `true`, triggers the scale + glow animation.
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
    val paddingValues: PaddingValues = when (size) {
        BadgeSize.Small -> PaddingValues(horizontal = 4.dp, vertical = 2.dp)
        BadgeSize.Large -> PaddingValues(horizontal = 10.dp, vertical = 7.dp)
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

    // Glow alpha: fades the coloured dropShadow in/out smoothly.
    val glowAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "badge-glow-alpha",
    )

    CompositionLocalProvider(
        LocalTextColor provides Color.White,
        LocalTextStyle provides KrailTheme.typography.titleMedium,
        LocalContentAlpha provides ContentAlphaTokens.EnabledContentAlpha,
    ) {
        Box(
            modifier = modifier
                .then(if (size == BadgeSize.Large) Modifier.widthIn(min = 44.dp) else Modifier)
                // Scale outermost so glow and clip both scale with the badge.
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                // dropShadow before clip — same pattern as InfoTile.
                // Uses the line's own colour as the glow colour for an on-brand feel.
                .dropShadow(
                    shape = badgeShape,
                    shadow = Shadow(
                        radius = GLOW_RADIUS,
                        color = backgroundColor,
                        spread = GLOW_SPREAD,
                        alpha = glowAlpha,
                    ),
                )
                .clip(badgeShape)
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = glowAlpha * 0.45f),
                    shape = badgeShape,
                )
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
