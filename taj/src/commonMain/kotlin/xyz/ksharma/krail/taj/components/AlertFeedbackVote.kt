package xyz.ksharma.krail.taj.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import xyz.ksharma.krail.taj.LocalTextColor
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.tokens.ContentAlphaTokens
import xyz.ksharma.krail.taj.tokens.IconSizeTokens

/**
 * Thumbs-up/thumbs-down row for feedback on AI-generated content. At most one choice is
 * selected at a time — tapping the already-selected choice does nothing (voting is not
 * togglable off, matching every other one-shot feedback control in the app).
 *
 * No standalone "thumbs down" icon exists in `material-icons-core`; down is the up icon
 * mirrored both axes rather than a second vector asset.
 *
 * @param selectedVote The rider's current vote, or `null` if they haven't voted yet.
 * @param onVoteClick Called with the tapped choice. Not called again for the choice
 * already in [selectedVote].
 */
@Composable
fun AlertFeedbackVote(
    selectedVote: AlertFeedbackVoteChoice?,
    onVoteClick: (AlertFeedbackVoteChoice) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(KrailTheme.dimensions.spacingXL),
    ) {
        VoteIconButton(
            choice = AlertFeedbackVoteChoice.UP,
            selected = selectedVote == AlertFeedbackVoteChoice.UP,
            contentDescription = "Helpful",
            onClick = { onVoteClick(AlertFeedbackVoteChoice.UP) },
        )
        VoteIconButton(
            choice = AlertFeedbackVoteChoice.DOWN,
            selected = selectedVote == AlertFeedbackVoteChoice.DOWN,
            contentDescription = "Not helpful",
            onClick = { onVoteClick(AlertFeedbackVoteChoice.DOWN) },
        )
    }
}

@Composable
private fun VoteIconButton(
    choice: AlertFeedbackVoteChoice,
    selected: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    // LocalTextColor, not LocalContentColor: the only color this component's callers
    // (e.g. CollapsibleAlert on its amber background) reliably provide is the one taj's
    // own Text() reads. LocalContentColor requires an explicit provider that isn't
    // guaranteed here, and silently resolves to Color.Unspecified (invisible) without one.
    val alpha = if (selected) ContentAlphaTokens.EnabledContentAlpha else ContentAlphaTokens.DisabledContentAlpha
    val tint = LocalTextColor.current.copy(alpha = alpha)
    Box(
        modifier = Modifier
            .size(IconSizeTokens.M)
            .klickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            imageVector = Icons.Default.ThumbUp,
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(tint),
            modifier = if (choice == AlertFeedbackVoteChoice.DOWN) {
                Modifier.graphicsLayer(scaleX = -1f, scaleY = -1f)
            } else {
                Modifier
            },
        )
    }
}

// region Previews

@PreviewComponent
@Composable
private fun PreviewAlertFeedbackVoteNoSelection() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        AlertFeedbackVote(selectedVote = null, onVoteClick = {})
    }
}

@PreviewComponent
@Composable
private fun PreviewAlertFeedbackVoteUpvoted() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        AlertFeedbackVote(selectedVote = AlertFeedbackVoteChoice.UP, onVoteClick = {})
    }
}

@PreviewComponent
@Composable
private fun PreviewAlertFeedbackVoteDownvoted() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        AlertFeedbackVote(selectedVote = AlertFeedbackVoteChoice.DOWN, onVoteClick = {})
    }
}

// endregion
