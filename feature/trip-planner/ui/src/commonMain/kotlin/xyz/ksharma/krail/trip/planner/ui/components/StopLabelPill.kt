package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_location_on
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.taj.LocalContentColor
import xyz.ksharma.krail.taj.LocalTextColor
import xyz.ksharma.krail.taj.LocalTextStyle
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel

/**
 * Filled pill shown when a [StopLabel] has a stop assigned. Sits on the dark
 * `stopLabelSurface` token in both light and dark mode and pairs with the matching
 * `onStopLabelSurface` content colour. Styling is locked inside the pill via
 * composition locals, so callers cannot override the typography or colours from
 * the outside — by design.
 */
@Composable
internal fun SetLabelPill(
    label: StopLabel,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    val shape = RoundedCornerShape(dim.radiusFull)
    val pillBackground = KrailTheme.colors.stopLabelSurface
    val contentColor = KrailTheme.colors.onStopLabelSurface

    CompositionLocalProvider(
        LocalTextStyle provides KrailTheme.typography.labelLarge,
        LocalTextColor provides contentColor,
        LocalContentColor provides contentColor,
    ) {
        val icon = stopLabelIcon(label.label) ?: Res.drawable.ic_location_on
        Row(
            modifier = modifier
                .clip(shape)
                .background(pillBackground, shape)
                .padding(horizontal = dim.chipHorizontalPadding, vertical = dim.chipVerticalPadding),
            horizontalArrangement = Arrangement.spacedBy(dim.spacingXS),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                colorFilter = ColorFilter.tint(LocalContentColor.current),
                modifier = Modifier.size(dim.spacingXL),
            )
            Text(text = label.label)
        }
    }
}

/**
 * Outlined pill shown when a [StopLabel] has no stop assigned. Background stays
 * transparent so the screen's themed gradient shows through; the border uses
 * `onSurface` (full opacity) to match the weight of the OutlinedButton "+ Add" chip.
 *
 * Styling is locked inside the pill via composition locals.
 *
 * The [isAssigning] flag is intentionally unused here — the assigning visual is
 * a scale-up animation applied by the parent so the pill's own colours stay
 * stable during the transition.
 */
@Composable
internal fun UnsetLabelPill(
    label: StopLabel,
    @Suppress("UNUSED_PARAMETER") isAssigning: Boolean,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    val shape = RoundedCornerShape(dim.radiusFull)
    val borderColor = KrailTheme.colors.onSurface
    val contentColor = KrailTheme.colors.label

    CompositionLocalProvider(
        LocalTextStyle provides KrailTheme.typography.labelLarge,
        LocalTextColor provides contentColor,
        LocalContentColor provides contentColor,
    ) {
        val icon = stopLabelIcon(label.label) ?: Res.drawable.ic_location_on
        Row(
            modifier = modifier
                .clip(shape)
                .border(width = dim.strokeThin, color = borderColor, shape = shape)
                .padding(horizontal = dim.chipHorizontalPadding, vertical = dim.chipVerticalPadding),
            horizontalArrangement = Arrangement.spacedBy(dim.spacingXS),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                colorFilter = ColorFilter.tint(LocalContentColor.current),
                modifier = Modifier.size(dim.spacingXL),
            )
            Text(text = label.label)
        }
    }
}

// region Previews

// @ScreenshotTest disabled: missing baseline (recording timed out, see README)
@PreviewComponent
@Composable
private fun PreviewSetLabelPill_Train() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        SetLabelPill(label = StopLabel(emoji = "🏠", label = "Home"))
    }
}

// @ScreenshotTest disabled: missing baseline (recording timed out, see README)
@PreviewComponent
@Composable
private fun PreviewSetLabelPill_PurpleDrip() {
    PreviewTheme(themeStyle = KrailThemeStyle.PurpleDrip) {
        SetLabelPill(label = StopLabel(emoji = "🎓", label = "Uni"))
    }
}

// @ScreenshotTest disabled: missing baseline (recording timed out, see README)
@PreviewComponent
@Composable
private fun PreviewUnsetLabelPill_Idle() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        UnsetLabelPill(
            label = StopLabel(emoji = "🏠", label = "Home"),
            isAssigning = false,
        )
    }
}

// @ScreenshotTest disabled: missing baseline (recording timed out, see README)
@PreviewComponent
@Composable
private fun PreviewUnsetLabelPill_Assigning() {
    PreviewTheme(themeStyle = KrailThemeStyle.Bus) {
        UnsetLabelPill(
            label = StopLabel(emoji = "💼", label = "Work"),
            isAssigning = true,
        )
    }
}

// endregion
