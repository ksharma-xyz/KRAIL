package xyz.ksharma.krail.trip.planner.ui.searchstop

import xyz.ksharma.aagya.permission.PermissionStatus
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.trip.planner.ui.components.FROM_PLACEHOLDER
import xyz.ksharma.krail.trip.planner.ui.components.TO_PLACEHOLDER
import xyz.ksharma.krail.trip.planner.ui.components.ToFromField
import xyz.ksharma.krail.trip.planner.ui.navigation.SearchStopFieldType
import xyz.ksharma.krail.trip.planner.ui.savedtrips.LABEL_TAP_RADIUS_KM
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.NearbyStopFeature
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

// The form at the top of Search Stop, rules 35 and 36 of `docs/MAP_HOME_UX.md`, and the location
// status pill under it, rule 24. Each decision is a pure function so none needs a layout.

/** How many bars the form has. */
internal enum class FormShape {
    /** A label, or one leg of a trip: one stop, one bar. */
    Single,

    /** To above From, the shape of the home block. */
    ToAndFrom,
}

internal fun formShapeFor(fieldType: SearchStopFieldType, editTripLeg: Boolean): FormShape =
    if (fieldType == SearchStopFieldType.LABEL || editTripLeg) FormShape.Single else FormShape.ToAndFrom

/**
 * The placeholder a bar shows while it is empty. The To and From bars always read the same two
 * phrases, except that a bar holding a stop ([current]) reads that stop while it is the one being
 * edited and nothing is typed, so the rider still sees what they are changing. A single bar names
 * what it is for: a label's stop, or the end of the trip being changed.
 */
internal fun barPlaceholderFor(
    field: ToFromField,
    fieldType: SearchStopFieldType,
    editTripLeg: Boolean,
    current: StopItem? = null,
): String = when (formShapeFor(fieldType, editTripLeg)) {
    FormShape.ToAndFrom -> current?.stopName
        ?: if (field == ToFromField.From) FROM_PLACEHOLDER else TO_PLACEHOLDER
    FormShape.Single -> when {
        fieldType == SearchStopFieldType.LABEL -> PLACEHOLDER_LABEL
        fieldType == SearchStopFieldType.FROM -> PLACEHOLDER_CHANGE_ORIGIN
        else -> PLACEHOLDER_CHANGE_DESTINATION
    }
}

private const val PLACEHOLDER_LABEL = "Choose a stop"
private const val PLACEHOLDER_CHANGE_ORIGIN = "Change origin"
private const val PLACEHOLDER_CHANGE_DESTINATION = "Change destination"

/**
 * The go circle shows as soon as the trip can start: From has a start (rule 36, search section 5) and
 * so does To, and they are not the same stop. With either missing it is not drawn, and the From bar
 * ends where its text does.
 */
internal fun goVisible(from: StopItem?, to: StopItem?): Boolean =
    from != null && to != null && from.stopId != to.stopId

/**
 * Which field of the trip a pick is for. To and From edit the bar the rider is on, whichever field
 * the screen was opened for, and the destination when nothing is being edited. A single bar is always
 * the field the route named.
 */
internal fun pickFieldFor(
    shape: FormShape,
    editing: ToFromField?,
    fieldType: SearchStopFieldType,
): SearchStopFieldType = when {
    shape == FormShape.Single -> fieldType
    editing == ToFromField.From -> SearchStopFieldType.FROM
    else -> SearchStopFieldType.TO
}

/**
 * The stop that fills the From bar by itself: the nearest of [stops] to [location] within
 * [LABEL_TAP_RADIUS_KM], the radius Home and Work use to plan from the rider (A18). Null when none
 * is that near, so a rider far from every stop is asked for a start and never given a long walk.
 */
internal fun autoFromStop(location: LatLng, stops: List<NearbyStopFeature>): StopItem? =
    nearestStopRows(location, stops, limit = 1, radiusKm = LABEL_TAP_RADIUS_KM)
        .firstOrNull()
        ?.let { StopItem(stopId = it.stopId, stopName = it.stopName) }

/** What the location pill is and does, rule 24. */
internal enum class LocationPill {
    /** Nothing: location is on, or the permission has not been looked up yet, so the pill never flashes in. */
    Hidden,

    /** The rider has not been asked, so tapping asks. */
    Ask,

    /** The rider refused, or cannot be asked, so tapping opens Settings. */
    OpenSettings,
}

/**
 * The pill for the permission [status] (null until it has been looked up). A fix in hand means
 * location is on whatever the last lookup said. A refusal opens Settings, the way the location
 * button and the old banner did, and anything not yet decided asks.
 */
internal fun locationPillFor(status: PermissionStatus?, hasUserLocation: Boolean): LocationPill = when {
    hasUserLocation || status == null || status is PermissionStatus.Granted -> LocationPill.Hidden
    status is PermissionStatus.Denied || status is PermissionStatus.Restricted -> LocationPill.OpenSettings
    else -> LocationPill.Ask
}
