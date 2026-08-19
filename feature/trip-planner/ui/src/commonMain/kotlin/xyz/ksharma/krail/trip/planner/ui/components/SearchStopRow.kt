package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_search
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.taj.LocalContentColor
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.components.AiWheelMark
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.RoundIconButton
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TextFieldButton
import xyz.ksharma.krail.taj.components.ThemeTextFieldPlaceholderText
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.tokens.AiThemeGradientTokens
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputEvent
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

private val SearchRowTopRadius = 36.dp
private val SearchRowVerticalPadding = 20.dp
private val SearchFieldSpacing = 20.dp

@Composable
fun SearchStopRow(
    fromButtonClick: () -> Unit,
    toButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
    fromStopItem: StopItem? = null,
    toStopItem: StopItem? = null,
    isExpanded: Boolean = false,
    isFromHighlighted: Boolean = false,
    onExpandRequest: () -> Unit = {},
    onCollapseRequest: (() -> Unit)? = null,
    onSearchButtonClick: () -> Unit = {},
    onAiEvent: (AiSearchInputEvent) -> Unit = {},
    isAiSearchAvailable: Boolean = false,
    // True for the beat after the Ask KRAIL dialog closes onto this row with an answer: the
    // wheel spins and settles, pointing the eye at the fields the dialog just filled.
    isAiHandoffSettling: Boolean = false,
    dateTimeSelectionText: String? = null,
    onDateTimeSelectionClear: () -> Unit = {},
) {
    val themeColorHex by LocalThemeColor.current
    val navBarPadding = with(LocalDensity.current) {
        WindowInsets.navigationBars.getBottom(this).toDp()
    }

    // When launched from a label pill tap (isFromHighlighted=true), From field is initially
    // hidden to let the "To is pre-filled" message land first, then animates in.
    var showFromField by rememberSaveable(isExpanded, isFromHighlighted) {
        mutableStateOf(isExpanded && !isFromHighlighted)
    }

    LaunchedEffect(isExpanded, isFromHighlighted) {
        if (isExpanded && isFromHighlighted && !showFromField) {
            delay(350)
            showFromField = true
        } else if (!isExpanded) {
            showFromField = false
        } else if (isExpanded && !isFromHighlighted) {
            showFromField = true
        }
    }

    AnimatedContent(
        targetState = isExpanded,
        transitionSpec = {
            if (targetState) {
                slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                ) + fadeIn(animationSpec = tween(400)) togetherWith
                    fadeOut(animationSpec = tween(150))
            } else {
                fadeIn(animationSpec = tween(100)) togetherWith
                    slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(350, easing = FastOutSlowInEasing),
                    ) + fadeOut(animationSpec = tween(250))
            }
        },
        modifier = modifier.testTag(TripPlannerTestTags.SEARCH_ROW),
        contentAlignment = Alignment.BottomCenter,
        label = "SearchStopRowContent",
    ) { expanded ->
        if (expanded) {
            ExpandedSearchRow(
                fromStopItem = fromStopItem,
                toStopItem = toStopItem,
                themeColorHex = themeColorHex,
                navBarPadding = navBarPadding.value,
                showFromField = showFromField,
                isFromHighlighted = isFromHighlighted,
                fromButtonClick = fromButtonClick,
                toButtonClick = toButtonClick,
                onCollapseRequest = onCollapseRequest,
                onSearchButtonClick = onSearchButtonClick,
                onAiEvent = onAiEvent,
                isAiSearchAvailable = isAiSearchAvailable,
                isAiHandoffSettling = isAiHandoffSettling,
                dateTimeSelectionText = dateTimeSelectionText,
                onDateTimeSelectionClear = onDateTimeSelectionClear,
            )
        } else {
            CollapsedPill(
                navBarPadding = navBarPadding.value,
                onClick = onExpandRequest,
            )
        }
    }
}

@Composable
private fun CollapsedPill(
    navBarPadding: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions

    // One-shot "hello" pulse — the pill lands at natural size via the parent's
    // scaleIn(0 → 1), waits for that to fully settle, then breathes up to a
    // small peak and back down. Stays at or above 1.0 the whole time, so the
    // text never collides with the parent's scaleIn clip rect (the previous
    // overshoot+dip approach showed visible cropping).
    //
    // Plays at most once per session — rememberSaveable survives navigation and
    // config changes, so coming back to this screen does not replay it.
    var hasPlayedEntryAnimation by rememberSaveable { mutableStateOf(false) }
    val scale = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        if (!hasPlayedEntryAnimation) {
            delay(PILL_PULSE_START_DELAY_MILLIS)
            scale.animateTo(
                targetValue = PILL_PULSE_PEAK_SCALE,
                animationSpec = tween(
                    durationMillis = PILL_PULSE_UP_DURATION_MILLIS,
                    easing = FastOutSlowInEasing,
                ),
            )
            scale.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = PILL_PULSE_DOWN_DURATION_MILLIS,
                    easing = FastOutSlowInEasing,
                ),
            )
            hasPlayedEntryAnimation = true
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
            .padding(
                bottom = with(LocalDensity.current) { navBarPadding.dp } + dim.spacingXL,
                start = dim.pageHorizontalPadding,
                end = dim.pageHorizontalPadding,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Button(
            dimensions = ButtonDefaults.mediumButtonSize(),
            onClick = onClick,
            modifier = Modifier
                .testTag(TripPlannerTestTags.SEARCH_ROW_COLLAPSED_PILL)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                },
        ) {
            Text(
                text = "Plan a trip",
            )
        }
    }
}

// Wait for the parent AnimatedVisibility's scaleIn (~600ms with bouncy spring)
// to fully settle before pulsing — competing scale animations against the
// parent's clip rect was what caused visible text cropping in the previous
// overshoot/dip design.
private const val PILL_PULSE_START_DELAY_MILLIS = 600L
private const val PILL_PULSE_PEAK_SCALE = 1.05f
private const val PILL_PULSE_UP_DURATION_MILLIS = 240
private const val PILL_PULSE_DOWN_DURATION_MILLIS = 360

@Composable
private fun ExpandedSearchRow(
    fromStopItem: StopItem?,
    toStopItem: StopItem?,
    themeColorHex: String,
    navBarPadding: Float,
    showFromField: Boolean,
    isFromHighlighted: Boolean,
    fromButtonClick: () -> Unit,
    toButtonClick: () -> Unit,
    onSearchButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
    onCollapseRequest: (() -> Unit)? = null,
    onAiEvent: (AiSearchInputEvent) -> Unit = {},
    isAiSearchAvailable: Boolean = false,
    isAiHandoffSettling: Boolean = false,
    dateTimeSelectionText: String? = null,
    onDateTimeSelectionClear: () -> Unit = {},
) {
    val dim = KrailTheme.dimensions

    // Cutout inset on the outer box so the background itself doesn't draw behind
    // the camera notch/punch-hole in landscape. Content and background are both
    // constrained to the safe horizontal area.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = themeColorHex.hexToComposeColor(),
                    shape = RoundedCornerShape(
                        topStart = SearchRowTopRadius,
                        topEnd = SearchRowTopRadius,
                    ),
                ),
        ) {
            if (onCollapseRequest != null) {
                CollapseHandle(onClick = onCollapseRequest)
            }

            // Only drawn when something set a time, which today only the AI sheet can do.
            // It has to be visible before Search is pressed: a time held in state that the
            // rider cannot see would quietly change what the button does.
            if (dateTimeSelectionText != null) {
                // Tapping clears it back to now. Opening the picker from here is the next step
                // and wants the selector route to be reachable from this screen first.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dim.pageHorizontalPadding)
                        .padding(top = dim.spacingM),
                ) {
                    WhenChip(text = dateTimeSelectionText, onClick = onDateTimeSelectionClear)
                }
            }

            val navBarPaddingDp = with(LocalDensity.current) { navBarPadding.dp }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = if (onCollapseRequest != null) 0.dp else SearchRowVerticalPadding + dim.spacingM,
                        bottom = SearchRowVerticalPadding + navBarPaddingDp,
                        start = dim.pageHorizontalPadding,
                        end = dim.pageHorizontalPadding,
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(SearchFieldSpacing),
                ) {
                    FromToFields(
                        fromStopItem = fromStopItem,
                        toStopItem = toStopItem,
                        showFromField = showFromField,
                        isFromHighlighted = isFromHighlighted,
                        fromButtonClick = fromButtonClick,
                        toButtonClick = toButtonClick,
                    )
                }

                // Action buttons (AI wheel + search) - same RoundIconButton, same size, for
                // both so the column stays visually symmetric.
                Column(
                    modifier = Modifier.padding(start = dim.spacingXL),
                    verticalArrangement = Arrangement.spacedBy(SearchFieldSpacing),
                ) {
                    if (isAiSearchAvailable) {
                        AiSheetEntryButton(
                            onAiEvent = onAiEvent,
                            spinning = isAiHandoffSettling,
                        )
                    }

                    SearchButton(onSearchButtonClick = onSearchButtonClick)
                }
            }
        }
    } // closes cutout Box
}

@Composable
private fun FromToFields(
    fromStopItem: StopItem?,
    toStopItem: StopItem?,
    showFromField: Boolean,
    isFromHighlighted: Boolean,
    fromButtonClick: () -> Unit,
    toButtonClick: () -> Unit,
) {
    val dim = KrailTheme.dimensions

    // Pulsing border alpha for the highlighted From field
    val infiniteTransition = rememberInfiniteTransition(label = "fromHighlight")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = InfiniteRepeatableSpec(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "fromBorderAlpha",
    )

    // From field — animates in when opened via a label pill tap
    AnimatedVisibility(
        visible = showFromField,
        enter = expandVertically(
            expandFrom = Alignment.Top,
            animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        ) + fadeIn(animationSpec = tween(350)),
    ) {
        TextFieldButton(
            onClick = fromButtonClick,
            modifier = Modifier.testTag(TripPlannerTestTags.SEARCH_ROW_FROM).then(
                if (isFromHighlighted) {
                    Modifier.border(
                        width = dim.strokeRegular,
                        color = KrailTheme.colors.surface.copy(alpha = borderAlpha),
                        shape = RoundedCornerShape(50),
                    )
                } else {
                    Modifier
                },
            ),
        ) {
            StopFieldText(
                text = fromStopItem?.stopName ?: "Starting from",
                isActive = fromStopItem != null,
                slideUp = true,
                label = "startingFromText",
            )
        }
    }

    // A small spacer replaces the From field gap when From is hidden,
    // so the To field doesn't jump when From animates in.
    if (!showFromField) {
        Spacer(modifier = Modifier.height(0.dp))
    }

    // To field — always visible when expanded
    TextFieldButton(
        onClick = toButtonClick,
        modifier = Modifier.testTag(TripPlannerTestTags.SEARCH_ROW_TO),
    ) {
        StopFieldText(
            text = toStopItem?.stopName ?: "Destination",
            isActive = toStopItem != null,
            slideUp = false,
            label = "destinationText",
        )
    }
}

@Composable
private fun StopFieldText(text: String, isActive: Boolean, slideUp: Boolean, label: String) {
    val enterOffset: (Int) -> Int = if (slideUp) ({ it / 2 }) else ({ -it / 2 })
    val exitOffset: (Int) -> Int = if (slideUp) ({ -it / 2 }) else ({ it / 2 })
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            fadeIn(animationSpec = tween(200)) +
                slideInVertically(
                    initialOffsetY = enterOffset,
                    animationSpec = tween(500, easing = FastOutSlowInEasing),
                ) togetherWith fadeOut(animationSpec = tween(200)) +
                slideOutVertically(targetOffsetY = exitOffset, animationSpec = tween(500))
        },
        contentAlignment = Alignment.CenterStart,
        label = label,
    ) { targetText ->
        ThemeTextFieldPlaceholderText(text = targetText, isActive = isActive)
    }
}

/**
 * The way into the AI search sheet. Opening is all it does: the sheet owns speaking, typing
 * and every failure they can hit, so this button has one job and one look. It used to change
 * into a mic and back, which meant the row had to explain a mode it was not showing.
 *
 * Not rendered at all when the feature is off. It used to render regardless, because the flag
 * stopped at the ViewModel — the sheet opened, the rider typed a sentence, and Continue did
 * nothing.
 */
@Composable
private fun AiSheetEntryButton(
    onAiEvent: (AiSearchInputEvent) -> Unit,
    spinning: Boolean = false,
) {
    val dim = KrailTheme.dimensions
    val themeColorHex by LocalThemeColor.current

    RoundIconButton(
        content = {
            // The theme's own pair, not the fixed cool gradient the mark defaults to. This
            // button is the door to a surface that is now painted in these exact colours, so a
            // wheel in somebody else's blue and violet reads as a different product's button.
            //
            // It spins exactly once outside that surface: the settle beat after the dialog
            // closes onto this row with an answer, so the motion lands where the answer did.
            AiWheelMark(
                spinning = spinning,
                markSize = dim.iconDefault,
                colors = AiThemeGradientTokens.stopsFor(themeColorHex),
            )
        },
        onClick = { onAiEvent(AiSearchInputEvent.OpenInput) },
        modifier = Modifier.testTag(TripPlannerTestTags.SEARCH_ROW_ASK_KRAIL),
    )
}

@Composable
private fun SearchButton(onSearchButtonClick: () -> Unit) {
    val dim = KrailTheme.dimensions

    RoundIconButton(
        content = {
            Image(
                painter = painterResource(Res.drawable.ic_search),
                contentDescription = "Search",
                colorFilter = ColorFilter.tint(LocalContentColor.current),
                modifier = Modifier.size(dim.iconDefault),
            )
        },
        onClick = onSearchButtonClick,
        modifier = Modifier.testTag(TripPlannerTestTags.SEARCH_ROW_SEARCH),
    )
}

@Composable
private fun CollapseHandle(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 4.dp)
                .background(
                    color = KrailTheme.colors.surface,
                    shape = RoundedCornerShape(2.dp),
                ),
        )
    }
}

// Previews live in SearchStopRowPreviews.kt (kept separate to stay under TooManyFunctions limit)
