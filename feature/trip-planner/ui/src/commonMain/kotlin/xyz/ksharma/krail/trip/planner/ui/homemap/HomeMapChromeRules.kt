package xyz.ksharma.krail.trip.planner.ui.homemap

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.core.connectivity.TransportState
import xyz.ksharma.krail.trip.planner.ui.searchstop.LocationPill

// What floats over the home map and where the map goes, `docs/MAP_HOME_UX.md` rules 1, 20 and 24 and the
// Home structure table. Plain functions, so every rule is a table test and none needs a layout.

/** The two whole items the notices row can hold, in the order they sit. */
internal enum class HomeNotice {
    /** The location status pill, rule 24. Tapping it asks for the permission or opens Settings. */
    LocationPermission,

    /** The Map offline chip, the same pill as the location one. */
    MapOffline,
}

/**
 * The notices under the top row, in order. Both can apply at once, and then they are two whole items
 * in one wrapping row. Neither is ever squeezed or hidden to make the other fit.
 */
internal fun homeNoticesFor(pill: LocationPill, mapOffline: Boolean): List<HomeNotice> = buildList {
    if (pill != LocationPill.Hidden) add(HomeNotice.LocationPermission)
    if (mapOffline) add(HomeNotice.MapOffline)
}

/**
 * Whether the map cannot draw new ground. Only the OS saying the transport is down says so: an unknown
 * state is no grounds to claim it (`docs/NETWORK_RELIABILITY.md`), and a transport that reports up says
 * nothing about a request that has not failed. The map keeps its flat ground, its pins and the rider
 * dot while the chip shows, because all three are local.
 */
internal fun mapOfflineFor(transport: TransportState): Boolean = transport == TransportState.Down

/** Where the home map is drawn, by the window and the maps flag. */
internal enum class HomeMapPlacement {
    /** No map: the maps flag is off on a single pane window, which is today's home. */
    None,

    /** Full screen behind the cards, the whole point of the screen. */
    Behind,

    /** The right pane of a wide window, the one map the dual pane already draws. Never a second one. */
    SidePane,
}

/**
 * Where the one map goes. A wide window always had a map pane and keeps it whatever the flag says, so
 * the flag only decides whether a single pane window gets one behind the cards. Never both, which is
 * how the dual pane and the full screen map stay one map.
 */
internal fun homeMapPlacementFor(mapsAvailable: Boolean, dualPane: Boolean): HomeMapPlacement = when {
    dualPane -> HomeMapPlacement.SidePane
    mapsAvailable -> HomeMapPlacement.Behind
    else -> HomeMapPlacement.None
}

/** What sits at the bottom of home. */
internal enum class HomeBottom {
    /** The chips and the one search pill. Home never shows a second bar. */
    Pill,

    /** A pin's floating card, which takes the room the chips and the pill held. */
    Card,

    /** Nothing: still loading, or reorder mode, which takes the chrome away. */
    Hidden,
}

/**
 * What the bottom of home shows. Reorder mode wins over everything, a card open on the map takes the
 * place of the chips and the pill so nothing is ever laid over content (rule 42), and
 * otherwise the pill is always the one search bar, whatever stops the rider holds elsewhere. A pick, a
 * pin card or an Ask KRAIL answer opens Search Stop, which holds the To and From form.
 */
internal fun homeBottomFor(
    loading: Boolean,
    reordering: Boolean,
    cardOpen: Boolean,
): HomeBottom = when {
    reordering -> HomeBottom.Hidden
    cardOpen -> HomeBottom.Card
    loading -> HomeBottom.Hidden
    else -> HomeBottom.Pill
}

/**
 * The height the card stack stops short of at the bottom: what the chips and the pill measure, or an
 * open card and its edge, or nothing while the bottom is empty. The stack scrolls inside what is left,
 * so a short stack leaves the map under it free to touch.
 */
internal fun stackBottomReserve(bottom: HomeBottom, blockHeight: Dp, cardHeight: Dp, cardEdge: Dp): Dp =
    when (bottom) {
        HomeBottom.Pill -> blockHeight
        HomeBottom.Card -> cardHeight + cardEdge
        HomeBottom.Hidden -> 0.dp
    }

/** The most height the card stack may take: what is left below the top row, never a negative one. */
internal fun stackMaxHeight(available: Dp, reserve: Dp): Dp = (available - reserve).coerceAtLeast(0.dp)
