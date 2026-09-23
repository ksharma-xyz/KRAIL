package xyz.ksharma.krail.trip.planner.ui.components.ai

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import xyz.ksharma.krail.core.appinfo.DevicePlatformType
import xyz.ksharma.krail.core.appinfo.getAppPlatformType
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.components.AiVoiceCloud
import xyz.ksharma.krail.taj.components.AiWheelMark
import xyz.ksharma.krail.taj.components.CloseIcon
import xyz.ksharma.krail.taj.components.CloudFieldSpec
import xyz.ksharma.krail.taj.components.CloudGradientBackground
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TitleBar
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.motion.isReduceMotionEnabled
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.isAppInDarkMode
import xyz.ksharma.krail.taj.tokens.AiThemeGradientTokens
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputEvent
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputPhase
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputUiState
import kotlin.math.max

private val DialogWidth = 420.dp

// The edit field's ring while the rider types. Quiet enough that the working state, when they
// send, still has somewhere to go.
private const val FIELD_BORDER_REST_ALPHA = 0.55f

// A new partial transcript swells the cloud this far, then lets go.
private const val PARTIAL_PULSE_PEAK = 0.7f
private const val PARTIAL_PULSE_MILLIS = 600

// One line of hint or one busy word, in a slot that never changes height so the field below
// never moves. Two lines of bodyMedium fit; anything longer ellipsises.
private val DialogStatusWheelSize = 18.dp
private const val STATUS_LINE_FADE_MILLIS = 200
private const val DIALOG_LISTENING_WORD = "Listening…"

// "Thinking", not "Working it out": shorter, and it reads as the app considering the sentence
// rather than labouring over it.
private const val DIALOG_WORKING_WORD = "Thinking…"

// Shown from the moment a trip resolves until the dialog closes onto the row. Without it the
// busy word faded out at the end of the border's beat and the "Try …" hint flashed back for
// the last half-second of the settle — three states in two seconds, ending on the one that
// invites another question just as the surface leaves.
private const val DIALOG_FOUND_WORD = "Found it"

// The field's fill: the rider's theme colour washed over the surface, never a grey. Grey next
// to a white card disappears in light mode and next to a near-black card disappears in dark;
// the tint keeps the box separable in both AND keeps it KRAIL's. Dark needs a heavier hand
// because a dark surface swallows most of a tint's chroma.
private const val FIELD_TINT_LIGHT_ALPHA = 0.08f
private const val FIELD_TINT_DARK_ALPHA = 0.16f

// The card resizing (a banner arriving, a sentence growing a line) eases rather than snaps.
private const val DIALOG_RESIZE_MILLIS = 400
private const val BANNER_ENTER_MILLIS = 350
private const val BANNER_EXIT_MILLIS = 250
private const val ENTER_SCALE = 0.94f
private const val EXIT_SCALE = 0.96f
private const val ENTER_MILLIS = 280
private const val EXIT_MILLIS = 220

/**
 * Where the AI input lives: a centred dialog wearing the theme's AI gradient as its border,
 * on every device.
 *
 * It was a full screen on portrait phones for most of this feature's life, on the modal-with-
 * a-keyboard argument from the platform guides. That argument fit a surface the rider READ —
 * the greeting, the working wheel, and for a while a result card all lived on it. The result
 * now lands on the home row behind this surface, so the surface's whole job is one sentence
 * and one send: a screen-sized page for that reads as a place, and this is not a place, it is
 * a question. The dialog also keeps the row it is about to fill visible around its scrim,
 * which is what makes the handoff legible when it closes onto it.
 *
 * The one exception is the largest font scales, where the dialog's content is a tall single
 * column that no longer fits in a floating card with the keyboard up: past
 * [ACTIONS_STACK_SCALE] the full screen comes back, because a dialog taller than the screen
 * it floats over is a worse answer than the screen itself.
 *
 * Both wrap the same [AiInputContent]. Neither knows anything about resolving a trip.
 */
@Composable
fun AskKrailScreen(
    state: AiSearchInputUiState,
    suggestion: String,
    onEvent: (AiSearchInputEvent) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    voiceLevel: () -> Float = { 0f },
) {
    val fontScale = LocalDensity.current.fontScale
    val asDialog = fontScale < ACTIONS_STACK_SCALE

    // Both presentations listen on open, so the effect lives above the split rather than in
    // each of them.
    ListenOnOpenEffect(state = state, onEvent = onEvent)

    val textFieldState = rememberTextFieldState()
    LaunchedEffect(state.typedText) {
        if (state.typedText != textFieldState.text.toString()) {
            textFieldState.setTextAndPlaceCursorAtEnd(state.typedText)
        }
    }

    if (asDialog) {
        AskKrailDialog(
            state = state,
            textFieldState = textFieldState,
            suggestion = suggestion,
            voiceLevel = voiceLevel,
            onEvent = onEvent,
            onDismiss = onDismiss,
            modifier = modifier,
        )
    } else {
        AskKrailFullScreen(
            state = state,
            textFieldState = textFieldState,
            suggestion = suggestion,
            onEvent = onEvent,
            onDismiss = onDismiss,
            modifier = modifier,
        )
    }
}

/**
 * Full screen, and deliberately NOT a Dialog.
 *
 * A Compose dialog opens its own window, and that window insets itself for the system bars,
 * so the gradient stopped short of the top and bottom edges and the system bar areas showed
 * whatever was behind. Drawing into the host window instead means this surface inherits the
 * app's own edge to edge setup, and `safeDrawingPadding` keeps the content clear of the bars
 * while the background runs to the edges. It also puts the keyboard inset on the same footing
 * as every other screen, rather than a second window's version of it.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun AskKrailFullScreen(
    state: AiSearchInputUiState,
    textFieldState: TextFieldState,
    suggestion: String,
    onEvent: (AiSearchInputEvent) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    val themeColorHex by LocalThemeColor.current

    // Without a dialog window there is no automatic back handling, and this surface covers the
    // screen it was opened from.
    BackHandler(enabled = true) { onDismiss() }

    Box(modifier = Modifier.fillMaxSize().background(KrailTheme.colors.surface)) {
        // The same drifting cloud field SearchStop wears, mirrored to hang from the bottom.
        //
        // It was a static gradient here, on the grounds that the cloud field puts its blobs in
        // the top half and this screen needs its light at the bottom. Mirroring is the cheaper
        // answer: one tuned field, one set of coprime periods, one dark-mode tint rule, shared
        // by both screens instead of a second thing to keep in step.
        //
        // imePadding, so the light rises with the input bar. Full screen instead, its brightest
        // band sat behind the keyboard and only the washed out middle reached the bar, which is
        // exactly when the bar needs something to sit against.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .graphicsLayer { scaleY = -1f },
        ) {
            CloudGradientBackground(spec = CloudFieldSpec.aiPair(themeColorHex))
        }
        Column(
            modifier = modifier
                .fillMaxSize()
                // One inset authority, and it is here: the column has to END where the keyboard
                // starts. The surface is a sibling of the home screen's ime-padded box rather
                // than a child, so nothing above it claims the keyboard as well.
                //
                // This only holds because MainActivity declares
                // android:windowSoftInputMode="adjustResize". Without it the mode defaults to
                // ADJUST_UNSPECIFIED, the system resolves it to pan, and the window slides up
                // by the keyboard's height on top of the padding applied here: the bar ended
                // up a keyboard's height above the keyboard and the content above it was
                // clipped off the top of the screen. Never let that attribute be removed.
                .safeDrawingPadding(),
        ) {
            // Named, like every other screen in the app. It was briefly title-less on the
            // grounds that the field said Ask KRAIL too, but the field no longer does: it asks
            // "Where to, and when?". The three lines now do three different jobs rather than
            // repeating one instruction, which was the actual problem.
            TitleBar(
                title = { Text(text = AI_INPUT_QUESTION) },
                onNavActionClick = onDismiss,
            )
            // Scrolling belongs to the content and only to the content. This column used to
            // scroll too at large font scales, which measured a scrolling child with an
            // infinite height and threw. One scroller, owned by the thing that knows when it
            // has overflowed.
            AiInputContent(
                state = state,
                textFieldState = textFieldState,
                suggestion = suggestion,
                onEvent = onEvent,
                // weight(1f) states that this takes whatever the title bar leaves. It is not
                // load bearing over fillMaxSize(): a Column hands a non-weighted child the
                // remaining bounded height, so both measure the same. Measured, not assumed,
                // after the opposite was claimed here and turned out to be false. See
                // docs/learning/2026-08-16-ime-pan-and-unbounded-column.md.
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = dim.pageHorizontalPadding)
                    // Enough to lift the bar off the keyboard without leaving a band of empty
                    // colour under it. 16dp read as resting on the keyboard, 32dp as floating
                    // away from it.
                    .padding(top = dim.spacingM, bottom = dim.spacingXXL),
            )
        }
    }
}

/**
 * The dialog keeps one size from open to close. `decorFitsSystemWindows = false` stops the
 * dialog's own window from resizing itself for the keyboard, leaving `imePadding` as the single
 * authority; without it the two mechanisms run at once, which is the jumping this pattern is
 * known for.
 *
 * A cloud of the AI colours around a steady core ([AiVoiceCloud]), not a card with a border.
 * The surface listens as it opens, and a card's border had nothing to say while it did; rings
 * that swell with the rider's voice say "I can hear you" without a word. They orbit while a
 * sentence is worked out, which is the job the spinning border did, and go quiet while a
 * problem is on screen. Only one working surface runs at a time: once the rider is editing, the
 * field wears the spinning border and the rings stay out of it.
 */
@Composable
private fun AskKrailDialog(
    state: AiSearchInputUiState,
    textFieldState: TextFieldState,
    suggestion: String,
    voiceLevel: () -> Float,
    onEvent: (AiSearchInputEvent) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    val themeColorHex by LocalThemeColor.current
    val workingBorder = rememberWorkingBorder(isWorking = state.isWorking)
    // Saveable, so a rotation mid-edit keeps the keyboard's field rather than folding it back
    // to text. Reset by leaving composition, which is what closing the dialog does.
    var editing by rememberSaveable { mutableStateOf(false) }
    val partialPulse = rememberPartialPulse(transcript = state.speechTranscript)

    Dialog(
        onDismissRequest = onDismiss,
        // usePlatformDefaultWidth is the only inset-adjacent flag in the common
        // DialogProperties: decorFitsSystemWindows is Android-only and usePlatformInsets is
        // not in this version's common surface, so the content applies its own insets and the
        // dialog window is left with its defaults.
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        // Enter and exit animate scale and alpha only. Both are draw properties, so the cloud is
        // measured once at its final size and nothing reflows mid animation.
        val visibleState = remember { MutableTransitionState(false).apply { targetState = true } }

        // The scrim tap, done here rather than left to the Dialog.
        //
        // Compose Multiplatform decides "outside" geometrically: the dialog layer's
        // boundsInWindow is set to the MEASURED SIZE OF THIS CONTENT, and only pointers
        // landing outside that rectangle reach the outside-pointer listener that calls
        // onDismissRequest. This content fills the window (it has to, to centre the cloud and
        // own the keyboard inset), so the bounds were the whole screen and no tap was ever
        // outside anything. Android got away with it because a Compose dialog there is a real
        // platform window with a working back press; iOS has neither, so the card was a room
        // with no door and the only way out was a sentence that resolved.
        //
        // No indication and no interaction source: this is a scrim, not a button, and a ripple
        // spreading across the whole screen on the way out is not what a dismissal looks like.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                )
                .imePadding(),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(
                visibleState = visibleState,
                enter = fadeIn(tween(ENTER_MILLIS)) + scaleIn(
                    initialScale = ENTER_SCALE,
                    animationSpec = tween(ENTER_MILLIS, easing = FastOutSlowInEasing),
                ),
                exit = fadeOut(tween(EXIT_MILLIS)) + scaleOut(
                    targetScale = EXIT_SCALE,
                    animationSpec = tween(EXIT_MILLIS),
                ),
            ) {
                AiVoiceCloud(
                    colors = AiThemeGradientTokens.stopsFor(themeColorHex),
                    voiceLevel = { max(voiceLevel(), partialPulse.value) },
                    orbiting = workingBorder.spinning && !editing,
                    quiet = state.problemMessage() != null && !state.isListening,
                    reduceMotion = isReduceMotionEnabled(),
                    modifier = modifier
                        // The cloud is not the scrim. Without this, a tap on the rings or the
                        // core's padding travels up to the dismiss handler on the box behind it
                        // and closes the surface the rider was reaching for. detectTapGestures
                        // rather than a no-op clickable so nothing announces the cloud itself
                        // as a control.
                        .pointerInput(Unit) { detectTapGestures { } }
                        // Fixed on a tablet, edge-margined on a phone: the padding narrows the
                        // incoming constraint first, then widthIn caps what fillMaxWidth may
                        // take.
                        .padding(horizontal = dim.pageHorizontalPadding)
                        .widthIn(max = DialogWidth)
                        .fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            // Inside the core, so the core follows the eased size when a
                            // banner arrives or the sentence gains a line.
                            .animateContentSize(
                                animationSpec = tween(
                                    durationMillis = DIALOG_RESIZE_MILLIS,
                                    easing = FastOutSlowInEasing,
                                ),
                            )
                            .padding(dim.spacingXL),
                        verticalArrangement = Arrangement.spacedBy(dim.spacingL),
                    ) {
                        AiDialogContent(
                            state = state,
                            textFieldState = textFieldState,
                            suggestion = suggestion,
                            busyVisible = state.isListening || workingBorder.spinning,
                            editing = editing,
                            onStartEditing = { editing = true },
                            onEvent = onEvent,
                            onDismiss = onDismiss,
                        )
                    }
                }
            }
        }
    }
}

/**
 * A short swell each time new words arrive, for platforms that cannot report a voice level.
 * Words only ever arrive while someone is speaking, so the cloud still answers the rider when
 * [SpeechToTextService.voiceLevel][xyz.ksharma.krail.core.speechtotext.SpeechToTextService]
 * stays at zero, which it does for the iOS SpeechAnalyzer path.
 */
@Composable
private fun rememberPartialPulse(transcript: String): Animatable<Float, AnimationVector1D> {
    val pulse = remember { Animatable(0f) }
    LaunchedEffect(transcript) {
        if (transcript.isNotBlank()) {
            pulse.snapTo(PARTIAL_PULSE_PEAK)
            pulse.animateTo(0f, animationSpec = tween(durationMillis = PARTIAL_PULSE_MILLIS))
        }
    }
    return pulse
}

/**
 * The dialog's content: an alert-sized column, not the full-screen stage.
 *
 * A centred title names the surface (there is no title bar to do it), one small line under it
 * carries the suggestion or the busy word, and the bar holds the sentence with mic and send
 * folded into it. The line is a fixed-height slot for the same reason the full screen's stage
 * is: swapping the suggestion for the busy word must not move the field mid-thought.
 *
 * No greeting latch here. On the full screen the suggestion is a welcome on an otherwise empty
 * page and leaves once the rider starts; in a card this small it is a caption-sized hint, and
 * a hint that vanishes on the first keystroke is a hint nobody finishes reading.
 */
@Composable
internal fun AiDialogContent(
    state: AiSearchInputUiState,
    textFieldState: TextFieldState,
    suggestion: String,
    busyVisible: Boolean,
    onEvent: (AiSearchInputEvent) -> Unit,
    modifier: Modifier = Modifier,
    editing: Boolean = false,
    onStartEditing: () -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    val dim = KrailTheme.dimensions
    val themeColorHex by LocalThemeColor.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dim.spacingL),
    ) {
        // No title. "Ask KRAIL" named a surface the rider had just opened with its own mic, and
        // it was the largest text on a cloud whose job is one sentence.
        //
        // The close button stays, on iOS only, and that is the whole reason it exists. A Compose
        // dialog on iOS has no back press and no swipe, so before the scrim tap there was no way
        // out of this surface at all; a drawn control makes the way out discoverable. Android
        // has the system back gesture, and a second control there would be clutter.
        if (getAppPlatformType() == DevicePlatformType.IOS) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Image(
                    imageVector = CloseIcon,
                    contentDescription = "Close",
                    colorFilter = ColorFilter.tint(KrailTheme.colors.onSurface),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .klickable(onClick = onDismiss)
                        // The padding is the touch target, not decoration.
                        .padding(dim.spacingL)
                        .size(dim.iconSmall),
                )
            }
        }

        // The line's three states, resolved in one place so their handover is auditable:
        // resolved beats busy (the border's beat outlives the answer, and "Found it" is what
        // that tail is FOR), busy beats the hint, and the hint only holds the stage when
        // nothing is happening. Success stays visible until the dialog closes onto the row —
        // it is the last thing the rider reads here, never the hint flashing back.
        val resolvedShowing = state.phase == AiSearchInputPhase.RESOLVED
        // One short line or nothing. The example lives under "Listening" in the sentence slot
        // (AiSpokenSentence) for the moment before words arrive; repeating it here once the
        // rider has stopped was a third line saying what they had already done.
        AnimatedVisibility(
            visible = busyVisible || resolvedShowing,
            enter = fadeIn(tween(STATUS_LINE_FADE_MILLIS)),
            exit = fadeOut(tween(STATUS_LINE_FADE_MILLIS)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            // The wheel beside the word, spinning in the shared rhythm. On resolve the same
            // wheel decelerates to a stop: found is a wheel settling, not one switched off.
            Row(
                horizontalArrangement = Arrangement.spacedBy(dim.spacingS, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AiWheelMark(
                    spinning = busyVisible && !resolvedShowing,
                    markSize = DialogStatusWheelSize,
                    colors = AiThemeGradientTokens.stopsFor(themeColorHex),
                )
                val word = when {
                    state.isListening -> DIALOG_LISTENING_WORD
                    resolvedShowing -> DIALOG_FOUND_WORD
                    else -> DIALOG_WORKING_WORD
                }
                AnimatedContent(
                    targetState = word,
                    transitionSpec = {
                        fadeIn(tween(STATUS_LINE_FADE_MILLIS)) togetherWith
                            fadeOut(tween(STATUS_LINE_FADE_MILLIS))
                    },
                ) { targetWord ->
                    Text(
                        text = targetWord,
                        style = KrailTheme.typography.bodyLarge,
                        color = KrailTheme.colors.onSurface,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        // The banner grows in and folds away rather than popping. The last message is held
        // through the exit so the fold shows words, not a collapsing empty box — clearing a
        // problem by typing is the rider moving on, and the message should leave the way it
        // came instead of vanishing mid-read.
        val problemMessage = state.problemMessage()
        val heldProblem = remember { mutableStateOf(problemMessage) }
        SideEffect {
            if (problemMessage != null) heldProblem.value = problemMessage
        }
        AnimatedVisibility(
            visible = problemMessage != null,
            enter = expandVertically(
                animationSpec = tween(BANNER_ENTER_MILLIS, easing = FastOutSlowInEasing),
            ) + fadeIn(tween(BANNER_ENTER_MILLIS)),
            exit = shrinkVertically(
                animationSpec = tween(BANNER_EXIT_MILLIS, easing = FastOutSlowInEasing),
            ) + fadeOut(tween(BANNER_EXIT_MILLIS)),
        ) {
            (problemMessage ?: heldProblem.value)?.let {
                // Bottom padding inside the animated block, not the column's gap: it doubles
                // the banner-to-field distance while the banner is up, and folds away with it
                // instead of leaving a dead gap behind.
                AiProblemBanner(
                    message = it,
                    modifier = Modifier.padding(bottom = dim.spacingL),
                )
            }
        }
        AiSpeechProblemAction(state = state, onEvent = onEvent)

        if (editing) {
            // Theme wash, not grey: see FIELD_TINT_*'s comment for why grey fails in both modes.
            val fieldTintAlpha = if (isAppInDarkMode()) FIELD_TINT_DARK_ALPHA else FIELD_TINT_LIGHT_ALPHA
            AiInputBar(
                state = state,
                textFieldState = textFieldState,
                placeholder = AI_INPUT_PLACEHOLDER,
                onEvent = onEvent,
                // The rider asked for the keyboard by tapping their words, so it comes up.
                autoFocus = true,
                // The field is the working surface while it is showing: a quiet ring while
                // they type, full strength and turning after Send. The rings leave it alone.
                showWorkingBorder = true,
                restBorderAlpha = FIELD_BORDER_REST_ALPHA,
                containerColor = themeColorHex.hexToComposeColor()
                    .copy(alpha = fieldTintAlpha)
                    .compositeOver(KrailTheme.colors.surface),
            )
        } else {
            AiSpokenSentence(
                state = state,
                text = textFieldState.text.toString(),
                suggestion = suggestion,
                onStartEditing = onStartEditing,
                onEvent = onEvent,
            )
        }
    }
}
