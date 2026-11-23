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

    /** From above To, two pills on one card. */
    ToAndFrom,
}

internal fun formShapeFor(fieldType: SearchStopFieldType, editTripLeg: Boolean): FormShape =
    if (fieldType == SearchStopFieldType.LABEL || editTripLeg) FormShape.Single else FormShape.ToAndFrom

/**
 * The prompt a bar shows while its field is empty, empty because nothing is typed or because it holds no stop. It is a
 * fixed phrase: From is [FROM_PLACEHOLDER] and To is [TO_PLACEHOLDER], and a single bar names what it is for, a label's
 * stop, or the end of the trip being changed. It is never a stop's name: a stop a bar holds is the bar's text while the
 * bar is at rest, and while the bar is edited the field starts empty with this prompt.
 */
internal fun barPlaceholderFor(
    field: ToFromField,
    fieldType: SearchStopFieldType,
    editTripLeg: Boolean,
    labelName: String? = null,
): String = when (formShapeFor(fieldType, editTripLeg)) {
    FormShape.ToAndFrom -> if (field == ToFromField.From) FROM_PLACEHOLDER else TO_PLACEHOLDER
    FormShape.Single -> when {
        fieldType == SearchStopFieldType.LABEL -> labelPromptFor(labelName)
        fieldType == SearchStopFieldType.FROM -> PLACEHOLDER_CHANGE_ORIGIN
        else -> PLACEHOLDER_CHANGE_DESTINATION
    }
}

/**
 * The prompt of the field that picks a stop for a label: Search and select Home, Search and select Work, or Search and
 * select followed by the label's saved name exactly as saved. A label with no name to read says a stop, so the prompt is
 * never blank. It is fixed like the From and To prompts and never becomes a stop's name.
 */
internal fun labelPromptFor(labelName: String?): String =
    if (labelName.isNullOrBlank()) PLACEHOLDER_LABEL_UNNAMED else "$PLACEHOLDER_LABEL_PREFIX$labelName"

private const val PLACEHOLDER_LABEL_PREFIX = "Search and select "
private const val PLACEHOLDER_LABEL_UNNAMED = "Search and select a stop"
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

/**
 * Whether the location is allowed, for the controls that only make sense with it (the Options button): the permission is
 * granted, or a fix is already in hand. Not looked up yet ([status] null) is not granted, so the button never flashes in
 * for a rider who has refused, and it is not the same as the pill being [LocationPill.Hidden], which it also is then.
 */
internal fun locationGrantedFor(status: PermissionStatus?, hasUserLocation: Boolean): Boolean =
    hasUserLocation || status is PermissionStatus.Granted
