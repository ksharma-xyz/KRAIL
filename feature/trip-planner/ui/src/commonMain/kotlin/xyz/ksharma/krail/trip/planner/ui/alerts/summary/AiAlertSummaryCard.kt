package xyz.ksharma.krail.trip.planner.ui.alerts.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.components.AiWheelMark
import xyz.ksharma.krail.taj.components.AlertFeedbackVote
import xyz.ksharma.krail.taj.components.AlertFeedbackVoteChoice
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.modifier.aiGradientBorder
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.tokens.AiThemeGradientTokens

private val SkeletonLineHeight = 12.dp
private val SkeletonLineCornerRadius = 6.dp
private const val SKELETON_LINE_ONE_WIDTH_FRACTION = 0.9f
private const val SKELETON_LINE_TWO_WIDTH_FRACTION = 0.65f

/**
 * The trip's single aggregate AI summary card, shown atop `ServiceAlertScreen` above the
 * list of individual (unmodified) alert cards. Gradient border + wheel mark both rotate
 * while [AlertSummaryUiState.Generating], then decelerate to a stop once
 * [AlertSummaryUiState.Resolved] — see `Modifier.aiGradientBorder` / `AiWheelMark`.
 */
@Composable
fun AiAlertSummaryCard(
    state: AlertSummaryUiState,
    onVoteClick: (AlertFeedbackVoteChoice) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    val spinning = state is AlertSummaryUiState.Generating
    // The rider's theme, not the fixed cool gradient both of these default to. Every other AI
    // surface in the app is painted from this pair now, and a card in somebody else's blue and
    // violet reads as a different product's card. Passed to the border and the mark from one
    // value so the two cannot drift: the doc's rule is that they read as a single object.
    val themeColorHex by LocalThemeColor.current
    val aiColors = remember(themeColorHex) { AiThemeGradientTokens.stopsFor(themeColorHex) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = KrailTheme.colors.surface,
                shape = RoundedCornerShape(dim.cardCornerRadius),
            )
            .aiGradientBorder(
                spinning = spinning,
                cornerRadius = dim.cardCornerRadius,
                colors = aiColors,
            )
            .padding(horizontal = dim.spacingXL, vertical = dim.spacingXL),
        verticalArrangement = Arrangement.spacedBy(dim.spacingL),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dim.spacingM),
        ) {
            AiWheelMark(spinning = spinning, colors = aiColors)
            Text(
                text = if (spinning) "Summarizing" else "AI Summary",
                style = KrailTheme.typography.labelSmall,
                color = KrailTheme.colors.softLabel,
            )
        }

        when (state) {
            is AlertSummaryUiState.Generating -> {
                SkeletonLine(widthFraction = SKELETON_LINE_ONE_WIDTH_FRACTION)
                SkeletonLine(widthFraction = SKELETON_LINE_TWO_WIDTH_FRACTION)
            }

            is AlertSummaryUiState.Resolved -> {
                Text(text = state.text, style = KrailTheme.typography.body)
                AlertFeedbackVote(selectedVote = state.vote, onVoteClick = onVoteClick)
            }
        }
    }
}

@Composable
private fun SkeletonLine(widthFraction: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(SkeletonLineHeight)
            .background(
                color = KrailTheme.colors.outlineSubtle,
                shape = RoundedCornerShape(SkeletonLineCornerRadius),
            ),
    )
}

// region Previews

@PreviewComponent
@Composable
private fun PreviewAiAlertSummaryCardGenerating() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        AiAlertSummaryCard(state = AlertSummaryUiState.Generating, onVoteClick = {})
    }
}

@PreviewComponent
@Composable
private fun PreviewAiAlertSummaryCardResolved() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        AiAlertSummaryCard(
            state = AlertSummaryUiState.Resolved(
                text = "Buses on the T1 line are replaced by coaches between Central and " +
                    "Strathfield until Sunday due to trackwork.",
            ),
            onVoteClick = {},
        )
    }
}

@PreviewComponent
@Composable
private fun PreviewAiAlertSummaryCardResolvedVoted() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        AiAlertSummaryCard(
            state = AlertSummaryUiState.Resolved(
                text = "Buses on the T1 line are replaced by coaches between Central and " +
                    "Strathfield until Sunday due to trackwork.",
                vote = AlertFeedbackVoteChoice.UP,
            ),
            onVoteClick = {},
        )
    }
}

// endregion
