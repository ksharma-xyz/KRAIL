package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import xyz.ksharma.krail.taj.theme.KrailTheme

/** The keys of the frame's own items, so every item of the list has an explicit key. */
internal const val INTRO_STICKY_HEADLINE_KEY = "intro-headline"
internal const val INTRO_STICKY_BAR_KEY = "intro-bar"
internal const val INTRO_STICKY_END_KEY = "intro-end"

/** Where the bar is in the list: the headline is the first item, and the bar, which sticks, is the second. */
internal const val INTRO_STICKY_BAR_INDEX = 1

/**
 * The frame of the destination and origin screens: one list that scrolls as a whole, the headline with its emoji, the bar
 * and then the rows [content] adds, with the bar as a sticky header. When the rider scrolls, the emoji and the headline go
 * off the top and the bar stays at the top of the screen, just under the top system inset, opaque in [ground], the colour of
 * the window behind the screen, so the rows slide under it and never show through.
 *
 * The headline keeps the room above it and under it that every Intro screen has ([introRhythm]) and is ordinary content here,
 * with no height kept for it. The bar is [bar]: the page padding on both sides and the room under it are the bar's own. The
 * decision bar the shell lays over the bottom of the screen is read from [LocalIntroBarInset]: the last row always has the
 * room of the rhythm's gap under it and, only while the bar is shown, the bar's height as well, so it scrolls clear of it.
 *
 * When the bar takes the focus the list scrolls once, with an animation, so the bar is at the top and the rows get the
 * room, and typing does not scroll it again. While the bar has the focus the list also keeps one window's height of room at
 * its end, so the bar can always reach the top, with a keyboard that is still on its way up and with a search that finds only
 * a row or two, and the bar stays where it is while the rider types. The room goes with the focus. A bar that is already
 * pinned, because the rider scrolled the rows, is left where it is.
 */
@Composable
internal fun IntroStickyStopFrame(
    headline: @Composable () -> Unit,
    bar: @Composable () -> Unit,
    ground: Color,
    modifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit,
) {
    val dim = KrailTheme.dimensions
    val rhythm = introRhythm(dim)
    val barRoom = rhythm.middleBottom + with(LocalDensity.current) { LocalIntroBarInset.current.scrollEndPx.toDp() }
    val listState = rememberLazyListState()
    var barFocused by remember { mutableStateOf(false) }
    LaunchedEffect(barFocused) {
        // A bar the rows have already scrolled up to is left alone: it is only a headline in view that scrolls away.
        if (barFocused && listState.firstVisibleItemIndex < INTRO_STICKY_BAR_INDEX) {
            listState.animateScrollToItem(INTRO_STICKY_BAR_INDEX)
        }
    }
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val endRoom = if (barFocused) barRoom + maxHeight else barRoom
        // The headline's room, which it reads to keep its widest word whole in the narrowest windows.
        val headlineRoom = maxWidth - dim.pageHorizontalPadding * 2
        CompositionLocalProvider(LocalHeadlineRoom provides headlineRoom) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                item(key = INTRO_STICKY_HEADLINE_KEY) {
                    Box(
                        modifier = Modifier.padding(
                            start = dim.pageHorizontalPadding,
                            end = dim.pageHorizontalPadding,
                            top = rhythm.top,
                            bottom = rhythm.titleToMiddle,
                        ),
                    ) {
                        headline()
                    }
                }
                stickyHeader(key = INTRO_STICKY_BAR_KEY) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ground)
                            .onFocusChanged { barFocused = it.hasFocus },
                    ) {
                        bar()
                    }
                }
                content()
                item(key = INTRO_STICKY_END_KEY) { Spacer(modifier = Modifier.height(endRoom)) }
            }
        }
    }
}
