package xyz.ksharma.krail.trip.planner.ui.components.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.components.TextField
import xyz.ksharma.krail.taj.components.TextFieldDefaults
import xyz.ksharma.krail.taj.modifier.aiGradientBorder
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.isAppInDarkMode
import xyz.ksharma.krail.taj.tokens.AiThemeGradientTokens
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputEvent
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputUiState

private val BarCornerRadius = 28.dp
private const val BAR_DARKEN_DARK = 0.45f
private const val SEND_ENTER_SCALE = 0.5f
private const val SEND_EXIT_SCALE = 0.35f
private const val SEND_FADE_MILLIS = 140

// Slower than the entry fade, so the shrink is still visible as the button goes.
private const val SEND_EXIT_FADE_MILLIS = 200

/**
 * Opaque, and a different shade from the screen in each theme for a different reason.
 *
 * Light mode is plain `surface`: white. The colour behind the bar is the drifting cloud field,
 * so a white bar reads as an object sitting on top of the light, which is exactly what it is.
 * It was greyed for a while, from when the background was flat and white-on-white had no edge
 * at all; against moving colour the grey only made it look dirty.
 *
 * Dark mode still darkens, because the opposite is true there: `surface` is already near black
 * and the field's light passes straight through anything that does not sit below it.
 *
 * Either way the result is one opaque colour rather than a translucent tint. A tint takes its
 * appearance from whatever happens to be behind it, and behind this is a gradient that moves.
 */
@Composable
private fun barColor(): Color = if (isAppInDarkMode()) {
    Color.Black.copy(alpha = BAR_DARKEN_DARK).compositeOver(KrailTheme.colors.surface)
} else {
    KrailTheme.colors.surface
}

/**
 * The whole input, as one object.
 *
 * The text and its controls used to be two things: a field, then a row of buttons floating
 * below it with a gap between. They read as a form. Folding the controls into the bottom of the
 * field's own container makes it one thing the rider acts on, which is what every message field
 * on a phone is and what riders already know how to use.
 *
 * Mic sits at the start and never leaves, because speaking is the other way in rather than a
 * fallback for an empty field. Send sits at the end and exists only when there is something to
 * send. Both are the same round button at the same size, differing only in fill: one is an
 * option, the other finishes the job.
 *
 * The listening animation is deliberately NOT in here. It belongs in the middle of the screen
 * where there is room for it, and putting it in the bar would mean the bar changing height the
 * moment a rider started speaking.
 */
@Composable
internal fun AiInputBar(
    state: AiSearchInputUiState,
    textFieldState: TextFieldState,
    placeholder: String,
    onEvent: (AiSearchInputEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    val themeColorHex by LocalThemeColor.current
    val workingBorder = rememberWorkingBorder(isWorking = state.isWorking)
    val hasText = textFieldState.text.isNotBlank()
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    // Opens with the caret already in the field and the keyboard up, the way SearchStopScreen
    // does. This surface exists to be typed or spoken into: making a rider tap a field that is
    // the only thing on the screen is a step that asks them to confirm what they already said
    // by opening it.
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BarCornerRadius))
            // Opaque, and DARKER than the surface in both themes.
            //
            // Three attempts, and the direction is the point. Painting `surface` gave the bar
            // no edge at all. Lightening it towards `onSurface` gave it one on a plain
            // background, but the field behind it is now moving colour, and a pale bar sitting
            // on light reads as part of the light. Darkening is what separates them: the
            // colour is behind, the bar is in front, and the sentence inside it gets the
            // contrast rather than competing for it.
            //
            // Same move in light mode, where it lands as a soft grey on white. Less of it,
            // because white needs far less shift to read as a distinct object than near black
            // does.
            .background(barColor())
            // Only ever while it is working out what was said. The border is the strongest
            // signal this surface has and spending it anywhere else would waste it.
            .aiGradientBorder(
                spinning = workingBorder.spinning,
                cornerRadius = BarCornerRadius,
                colors = AiThemeGradientTokens.stopsFor(themeColorHex),
                alpha = workingBorder.alpha,
            )
            .padding(horizontal = dim.spacingM, vertical = dim.spacingM),
        // The controls are a separate band from the sentence, and at 6dp they read as one
        // crowded block. This gap is what separates "what I am writing" from "what I can do
        // with it".
        verticalArrangement = Arrangement.spacedBy(dim.spacingXL),
    ) {
        // The container draws its own background and shape, so the field inside it draws
        // neither. Two backgrounds would show as a box inside a box, and the field's own
        // padding on top of the container's is the doubled inset a rider sees as soon as they
        // tap in.
        TextField(
            state = textFieldState,
            placeholder = placeholder,
            lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 1, maxHeightInLines = 6),
            minHeight = 0.dp,
            colors = TextFieldDefaults.colors().copy(containerColor = Color.Transparent),
            // Vertical inset is deliberately the larger token. The text sits in a container
            // with no fill of its own, so the only thing holding it off the bar's edges is this
            // padding: at the small token the sentence looked pushed against the top of the bar
            // as soon as it ran to a second line.
            contentPadding = PaddingValues(horizontal = dim.spacingM, vertical = dim.spacingL),
            // Default, NOT Search. Asking for a Search action makes the IME replace its enter
            // key with a magnifying glass, so a rider writing more than one line had no way to
            // start a second one. This is a multi-line field: enter belongs to the text, and
            // send belongs to the send button, which is where every message field on a phone
            // puts it.
            // Never disabled. A disabled field is drawn at the theme's disabled opacity, which
            // turned the rider's own sentence pale grey the moment it was being read. The rule
            // for this surface is that the text colour never changes: what they said has to
            // stay readable while it is being worked out. Submitting twice is already guarded
            // in the ViewModel, so nothing needs the field to be inert.
            enabled = true,
            // weight, with fill = false. The controls below are NOT weighted, so the Column
            // measures them first and the sentence gets only what is left. Without this the
            // field took its natural height up to six lines, and when the bar itself had been
            // squeezed - keyboard up, a result above it, or landscape - the controls were laid
            // out past the bar's own bottom edge and clipped INSIDE it. The rider saw a sentence
            // with no way to send it, while the buttons' unclipped bounds still reported as on
            // screen, which is why a bounds assertion missed this and only assertIsDisplayed
            // caught it. fill = false so a short sentence still makes a short bar.
            // See docs/learning/2026-08-16-clipped-inside-its-own-parent.md.
            modifier = Modifier
                .fillMaxWidth()
                .weight(weight = 1f, fill = false)
                .focusRequester(focusRequester),
            onTextChange = { onEvent(AiSearchInputEvent.TypedTextChanged(it.toString())) },
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AiVoiceControl(state = state, onEvent = onEvent, labelled = false)

            Spacer(modifier = Modifier.weight(1f))

            // Springs in from half size and settles, rather than fading. A send button
            // appearing is the app answering the rider's first character, and a fade reads as
            // something that was always there and is only now catching up.
            AnimatedVisibility(
                visible = hasText,
                enter = scaleIn(
                    initialScale = SEND_ENTER_SCALE,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                ) + fadeIn(animationSpec = tween(durationMillis = SEND_FADE_MILLIS)),
                // Leaves on the same spring it arrived on, rather than a flat tween. An exit
                // that only fades reads as the button having been switched off; the spring
                // makes it pull back as deliberately as it appeared, and shrinking further
                // (0.35 rather than 0.6) means it reads as leaving rather than as dimming.
                exit = scaleOut(
                    targetScale = SEND_EXIT_SCALE,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                ) + fadeOut(animationSpec = tween(durationMillis = SEND_EXIT_FADE_MILLIS)),
            ) {
                AiSendButton(
                    enabled = !state.isBusy,
                    onClick = { onEvent(AiSearchInputEvent.Submit) },
                )
            }
        }
    }
}
