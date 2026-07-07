package xyz.ksharma.krail.trip.planner.ui.timetable

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.krail.taj.resources.ic_close
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.modifier.CardShape
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.themeBackgroundColor
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableUiEvent
import app.krail.taj.resources.Res as TajRes

/**
 * Save nudge for an unsaved pair (story A2). A list item below the header,
 * so it never covers journey results.
 */
internal fun LazyListScope.saveTripPromptItem(
    showSaveTripPrompt: Boolean,
    onEvent: (TimeTableUiEvent) -> Unit,
) {
    if (!showSaveTripPrompt) return
    item(key = "save-trip-prompt") {
        SaveTripPromptCard(
            onSaveClick = { onEvent(TimeTableUiEvent.SaveTripPromptAccepted) },
            onDismissClick = { onEvent(TimeTableUiEvent.SaveTripPromptDismissed) },
            modifier = Modifier
                .fillParentMaxWidth()
                .padding(horizontal = KrailTheme.dimensions.spacingL)
                .padding(top = KrailTheme.dimensions.spacingL)
                .animateItem(),
        )
    }
}

/**
 * One-tap save nudge shown below the timetable header after loading an
 * unsaved origin-destination pair (story A2). Rendered as a list item so it
 * never covers journey results; frequency rules live in [TimeTableViewModel].
 */
@Composable
internal fun SaveTripPromptCard(
    onSaveClick: () -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(color = themeBackgroundColor(), shape = CardShape)
            .padding(horizontal = dim.spacingL, vertical = dim.spacingM),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dim.spacingM),
    ) {
        Text(
            text = "Save this trip?",
            style = KrailTheme.typography.titleSmall,
            color = KrailTheme.colors.onSurface,
            modifier = Modifier.weight(1f),
        )

        Button(
            onClick = onSaveClick,
            dimensions = ButtonDefaults.mediumButtonSize(),
        ) {
            Text(text = "Save")
        }

        Box(
            modifier = Modifier
                .size(dim.iconL)
                .clip(CircleShape)
                .klickable { onDismissClick() }
                .semantics(mergeDescendants = true) {
                    contentDescription = "Dismiss save trip prompt"
                },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(TajRes.drawable.ic_close),
                contentDescription = null,
                colorFilter = ColorFilter.tint(KrailTheme.colors.onSurface),
                modifier = Modifier.size(dim.iconS),
            )
        }
    }
}

// region Preview

@PreviewComponent
@Composable
private fun PreviewSaveTripPromptCard() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        SaveTripPromptCard(
            onSaveClick = {},
            onDismissClick = {},
        )
    }
}

// endregion
