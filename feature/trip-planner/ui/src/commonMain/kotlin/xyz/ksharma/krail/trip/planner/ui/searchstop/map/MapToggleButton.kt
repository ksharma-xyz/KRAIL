package xyz.ksharma.krail.trip.planner.ui.searchstop.map

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.LocalTextColor
import xyz.ksharma.krail.taj.LocalTextStyle
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.theme.getForegroundColor
import xyz.ksharma.krail.taj.themeColor

@Composable
fun MapToggleButton(
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    horizontalPadding: Dp = 24.dp,
    verticalPadding: Dp = 4.dp,
    content: @Composable () -> Unit,
) {
    val content = remember { movableContentOf { content() } }
    val animatedBackgroundColor by animateColorAsState(
        targetValue = if (selected) KrailTheme.colors.surface else Color.Transparent,
        animationSpec = tween(durationMillis = 600, easing = LinearEasing),
    )

    val animatedBorderColor by animateColorAsState(
        targetValue = if (selected) Color.Transparent else KrailTheme.colors.surface,
        animationSpec = tween(durationMillis = 400),
    )

    val animatedTextColor by animateColorAsState(
        targetValue = if (selected) {
            KrailTheme.colors.onSurface
        } else {
            getForegroundColor(themeColor())
        },
        animationSpec = tween(durationMillis = 400),
    )

    val textStyle = KrailTheme.typography.bodyMedium

    // Use SubcomposeLayout to measure content at both font weights and determine max width
    SubcomposeLayout(modifier = modifier.requiredHeight(48.dp)) { constraints ->
        // Measure with Bold font weight
        val boldPlaceables = subcompose("bold") {
            CompositionLocalProvider(
                LocalTextColor provides animatedTextColor,
                LocalTextStyle provides textStyle.copy(fontWeight = FontWeight.Bold),
            ) {
                content()
            }
        }.map { it.measure(Constraints()) }

        // Measure with SemiBold font weight
        val semiBoldPlaceables = subcompose("semiBold") {
            CompositionLocalProvider(
                LocalTextColor provides animatedTextColor,
                LocalTextStyle provides textStyle.copy(fontWeight = FontWeight.SemiBold),
            ) {
                content()
            }
        }.map { it.measure(Constraints()) }

        // Get max width from both measurements
        val maxContentWidth = maxOf(
            boldPlaceables.maxOfOrNull { it.width } ?: 0,
            semiBoldPlaceables.maxOfOrNull { it.width } ?: 0,
        )

        // Add padding and border compensation to get total width
        val totalWidth = maxContentWidth + (horizontalPadding * 2).roundToPx() + 2.dp.roundToPx()

        // Now create the actual button with the measured width
        val actualPlaceables = subcompose("actual") {
            Box(
                modifier = Modifier
                    .width(totalWidth.toDp())
                    .background(
                        color = animatedBackgroundColor,
                        shape = RoundedCornerShape(24.dp),
                    )
                    .then(
                        if (selected) {
                            Modifier.padding(1.dp)
                        } else {
                            Modifier.border(
                                shape = RoundedCornerShape(24.dp),
                                color = animatedBorderColor,
                                width = 2.dp,
                            )
                        },
                    )
                    .padding(vertical = verticalPadding, horizontal = horizontalPadding)
                    .klickable(
                        indication = null,
                        onClick = onClick,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                CompositionLocalProvider(
                    LocalTextColor provides animatedTextColor,
                    LocalTextStyle provides textStyle.copy(
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    ),
                ) {
                    content()
                }
            }
        }.map { it.measure(constraints) }

        layout(totalWidth, constraints.maxHeight) {
            actualPlaceables.forEach { it.placeRelative(0, 0) }
        }
    }
}

@PreviewComponent
@Composable
private fun PreviewMapToggleButtonSelected() {
    PreviewTheme(themeStyle = KrailThemeStyle.Metro) {
        MapToggleButton(
            selected = true,
        ) {
            Text("Map")
        }
    }
}

@PreviewComponent
@Composable
private fun PreviewMapToggleButtonUnselected() {
    PreviewTheme(themeStyle = KrailThemeStyle.Bus) {
        MapToggleButton(
            selected = false,
        ) {
            Text("Map")
        }
    }
}
