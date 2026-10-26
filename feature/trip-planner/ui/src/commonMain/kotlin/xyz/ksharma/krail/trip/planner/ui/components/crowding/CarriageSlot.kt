package xyz.ksharma.krail.trip.planner.ui.components.crowding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import xyz.ksharma.krail.feature.crowding.CarriageLevels

/**
 * Where the carriage section goes in an opened card.
 *
 * The slot is decided once, when the card opens: drawn only if the feed had already arrived,
 * so a card never grows under the rider's thumb. If the cars go away afterwards (a failed or
 * emptied refresh), the section fades for [FADE_MS] and folds to zero height over [FOLD_MS].
 * No placeholder, no error text. The decision is saveable so a rotation does not reopen it.
 *
 * [decideOnOpen] is false on the live trip screen, where the rider is watching a train they are
 * on or about to board: there the section may arrive with the first live reading.
 */
@Composable
internal fun CarriageSlot(
    cars: CarriageLevels?,
    sentence: String?,
    lineColor: Color,
    modifier: Modifier = Modifier,
    decideOnOpen: Boolean = true,
) {
    val slotOpen by rememberSaveable { mutableStateOf(!decideOnOpen || cars != null) }
    // Keep drawing the last cars while the section folds away.
    var lastCars by remember { mutableStateOf(cars) }
    var lastSentence by remember { mutableStateOf(sentence) }
    if (cars != null) {
        lastCars = cars
        lastSentence = sentence
    }
    AnimatedVisibility(
        visible = slotOpen && cars != null,
        modifier = modifier,
        enter = if (decideOnOpen) EnterTransition.None else fadeIn(tween(FADE_MS)) + expandVertically(),
        exit = fadeOut(tween(FADE_MS)) + shrinkVertically(tween(FOLD_MS, delayMillis = FADE_MS)),
    ) {
        lastCars?.let { CarriageSection(cars = it, sentence = lastSentence, lineColor = lineColor) }
    }
}

private const val FADE_MS = 150
private const val FOLD_MS = 300
