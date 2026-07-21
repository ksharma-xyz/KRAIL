package xyz.ksharma.krail.core.analytics.event

import xyz.ksharma.krail.core.analytics.AnalyticsScreen
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val PROP_STOP_ID = "stopId"
private const val PROP_ACTION = "action"
private const val PROP_VARIANT = "variant"
private const val PROP_SOURCE = "source"
private const val PROP_EXPAND = "expand"
private const val PROP_STOP_NAME = "stopName"
private const val PROP_SURFACE = "surface"
private const val PROP_LABEL_KIND = "labelKind"
private const val SURFACE_MANAGE_LABELS = "manage_labels"
private const val PROP_FROM_STOP_ID = "fromStopId"
private const val PROP_TO_STOP_ID = "toStopId"
private const val PROP_PREVIOUS_INDEX = "previousIndex"
private const val PROP_NEW_INDEX = "newIndex"
private const val PROP_TOTAL_COUNT = "totalCount"
private const val PROP_LEG_COUNT = "legCount"
private const val PROP_TRANSPORT_MODES = "transportModes"

sealed class AnalyticsEvent(val name: String, val properties: Map<String, Any>? = null) {

    data class ScreenViewEvent(val screen: AnalyticsScreen) : AnalyticsEvent(
        name = "view_screen",
        properties = mapOf("name" to screen.name),
    )

    // region SavedTrips

    data class SavedTripCardClickEvent(val fromStopId: String, val toStopId: String) :
        AnalyticsEvent(
            name = "saved_trip_card_click",
            properties = mapOf(PROP_FROM_STOP_ID to fromStopId, PROP_TO_STOP_ID to toStopId),
        )

    data class DeleteSavedTripClickEvent(val fromStopId: String, val toStopId: String) :
        AnalyticsEvent(
            name = "delete_saved_trip_card_click",
            properties = mapOf(PROP_FROM_STOP_ID to fromStopId, PROP_TO_STOP_ID to toStopId),
        )

    data object ReverseStopClickEvent : AnalyticsEvent(name = "reverse_stop_click")

    data class LoadTimeTableClickEvent(val fromStopId: String, val toStopId: String) :
        AnalyticsEvent(
            name = "load_timetable_click",
            properties = mapOf(PROP_FROM_STOP_ID to fromStopId, PROP_TO_STOP_ID to toStopId),
        )

    /**
     * User taps Retry after an API/load failure. Unified across surfaces to stay within the
     * Firebase 500-event budget: [source] identifies where the retry happened rather than
     * minting a per-screen event.
     */
    data class RetryApiEvent(val source: Source) :
        AnalyticsEvent(
            name = "retry_api",
            properties = mapOf(PROP_SOURCE to source.value),
        ) {
        enum class Source(val value: String) {
            TIMETABLE("timetable"),
        }
    }

    data object SettingsClickEvent : AnalyticsEvent(name = "settings_click")

    data object FromFieldClickEvent : AnalyticsEvent(name = "from_field_click")

    data object ToFieldClickEvent : AnalyticsEvent(name = "to_field_click")

    /**
     * Fired when the user reorders a saved-trip card by drag-and-drop in edit mode.
     * Use to measure whether reorder is actually used and how. Specifically:
     *  - "What % of users with saved trips ever reorder?" → distinct user count.
     *  - "Are users mostly promoting trips to the top, or shuffling middle slots?"
     *    → distribution of [newIndex], filter [newIndex] = 0.
     *  - "Does reorder usage scale with list size?" → bucket on [totalCount].
     *  - "Which trips get reordered most often?" → group by [PROP_FROM_STOP_ID]
     *    + [PROP_TO_STOP_ID]; can be joined with [SavedTripCardClickEvent].
     *
     * @param fromStopId    NSW Transport stop ID for the trip's origin — anchors
     *                      the moved card to a specific saved trip.
     * @param toStopId      Stop ID for the trip's destination.
     * @param previousIndex Position the card was at before the move.
     * @param newIndex      Position the card landed at after the move.
     * @param totalCount    Number of saved trips in the list at the time of the
     *                      reorder. Lets us split "casual user with 2 cards"
     *                      from "power user with 8".
     */
    data class SavedTripCardReorderedEvent(
        val fromStopId: String,
        val toStopId: String,
        val previousIndex: Int,
        val newIndex: Int,
        val totalCount: Int,
    ) : AnalyticsEvent(
        name = "saved_trip_card_reordered",
        properties = mapOf(
            PROP_FROM_STOP_ID to fromStopId,
            PROP_TO_STOP_ID to toStopId,
            PROP_PREVIOUS_INDEX to previousIndex,
            PROP_NEW_INDEX to newIndex,
            PROP_TOTAL_COUNT to totalCount,
        ),
    )

    // endregion

    // region SearchStop

    /**
     * @param searchSessionId      Joins the selection to its [SearchStopQuery] firings.
     *                             Null when the selection had no live query (recents,
     *                             empty-state stops, map picks).
     * @param displayedLocalCount  Bucketed count of local results on screen at
     *                             selection time; null without a live query.
     * @param displayedAddressCount Same for the address/POI section. Together these
     *                             answer "how many options were showing when the user
     *                             picked" without carrying any query text.
     */
    data class StopSelectedEvent(
        val stopId: String,
        val isRecentSearch: Boolean = false,
        val locationKind: LocationKind = LocationKind.TRANSIT_STOP,
        val addressType: AddressType? = null,
        val searchSessionId: String? = null,
        val displayedLocalCount: CountBucket? = null,
        val displayedAddressCount: CountBucket? = null,
    ) : AnalyticsEvent(
        name = "stop_selected",
        properties = buildMap {
            put(PROP_STOP_ID, stopId)
            put("isRecentSearch", isRecentSearch)
            put("locationKind", locationKind.value)
            addressType?.let { put("addressType", it.value) }
            searchSessionId?.let { put("searchSessionId", it) }
            displayedLocalCount?.let { put("displayedLocalCount", it.value) }
            displayedAddressCount?.let { put("displayedAddressCount", it.value) }
        },
    ) {
        /** Bounded so dashboards group cleanly; raw counts add cardinality without
         * answering anything the buckets can't. */
        enum class CountBucket(val value: String) {
            ZERO("0"),
            ONE_TO_THREE("1_3"),
            FOUR_TO_TEN("4_10"),
            ELEVEN_PLUS("11_plus"),
            ;

            companion object {
                private const val MAX_ONE_TO_THREE = 3
                private const val MAX_FOUR_TO_TEN = 10

                fun from(count: Int): CountBucket = when {
                    count <= 0 -> ZERO
                    count <= MAX_ONE_TO_THREE -> ONE_TO_THREE
                    count <= MAX_FOUR_TO_TEN -> FOUR_TO_TEN
                    else -> ELEVEN_PLUS
                }
            }
        }

        /** Mirrors `StopItem.LocationKind` (feature/trip-planner/state) - redefined here
         * so `core/analytics` doesn't depend on a feature-layer model. Map at the call
         * site instead of adding a cross-module dependency. */
        enum class LocationKind(val value: String) {
            TRANSIT_STOP("transit_stop"),
            ADDRESS("address"),
        }

        /** Allowlisted NSW `stop_finder` location types - never pass the raw API string
         * through; anything not in this set folds to [UNKNOWN]. Values match the
         * existing UI mapping in `AddressSearchListItem.kt`'s `addressTypeLabel`. */
        enum class AddressType(val value: String) {
            SINGLEHOUSE("singlehouse"),
            STREET("street"),
            POI("poi"),
            UNKNOWN("unknown"),
            ;

            companion object {
                fun from(rawType: String?): AddressType =
                    entries.find { it.value == rawType } ?: UNKNOWN
            }
        }
    }

    /**
     * Fired when a settled search query finishes resolving. Never carries the raw
     * typed text by default: a query can be a street address, which identifies a home
     * or workplace, and the privacy policy promises analytics hold no personally
     * identifiable data.
     *
     * [zeroResultQuery] is the single, deliberately narrow exception, kept for
     * fuzzy-matcher diagnostics (the "townhall returned nothing" workflow). Callers
     * must resolve it through `SearchQueryAnalyticsRedaction.zeroResultQueryOrNull`,
     * which requires zero results everywhere, no digits, and a short length. It lands
     * in the same "query" property historical dashboards already read.
     *
     * @param queryLength     Character count of the typed query - shape signal, no text.
     * @param searchSessionId Random ID minted per settled query; joins this event to
     *                        [StopSelectedEvent] so funnels work without query text.
     * @param resultsCount    Result count on success.
     * @param isError         True when the search pipeline threw.
     * @param zeroResultQuery Raw text only under the redaction carve-out, else null.
     * @param resultSource    Which pipeline resolved: local stop search or the remote
     *                        NSW address/POI pipeline. One firing per pipeline per
     *                        settled query; join on [searchSessionId].
     */
    data class SearchStopQuery(
        val queryLength: Int,
        val searchSessionId: String,
        val resultsCount: Int? = null,
        val isError: Boolean = false,
        val zeroResultQuery: String? = null,
        val resultSource: ResultSource = ResultSource.LOCAL,
    ) : AnalyticsEvent(
        name = "search_stop_query",
        properties = buildMap {
            put("queryLength", queryLength)
            put("searchSessionId", searchSessionId)
            put("resultSource", resultSource.value)
            if (isError) {
                put("isError", isError)
            } else if (resultsCount != null) {
                put("resultsCount", resultsCount)
            }
            zeroResultQuery?.let { put("query", it) }
        },
    ) {
        enum class ResultSource(val value: String) {
            LOCAL("local"),
            ADDRESS("address"),
        }
    }

    data class ClearRecentSearchClickEvent(
        val recentSearchCount: Int,
    ) : AnalyticsEvent(
        name = "clear_recent_search_stops",
        properties = mapOf(
            "recentSearchCount" to recentSearchCount,
        ),
    )

    // endregion

    // region StopLabels

    /**
     * Bounded classification of a label. Raw label names never leave the device:
     * a custom label ("Mum's", "Gym") plus a pinned stop can identify a person or
     * place, and the privacy policy promises analytics hold no PII.
     */
    enum class StopLabelKind(val value: String) {
        PROTECTED_DEFAULT("protected_default"),
        CUSTOM("custom"),
    }

    /** Which row kind hosted a label create/assign action. Registered values -
     * `address_result` is reserved for when address rows gain label support. */
    enum class StopLabelSurface(val value: String) {
        SEARCH_RESULT("search_result"),
        RECENT("recent"),
        EMPTY_STATE("empty_state"),
        ADDRESS_RESULT("address_result"),
    }

    /** How many labels the user has, bucketed - splits casual (1-2) from power users. */
    enum class StopLabelCountBucket(val value: String) {
        ONE("1"),
        TWO("2"),
        THREE_TO_FIVE("3_5"),
        SIX_PLUS("6_plus"),
        ;

        companion object {
            private const val MAX_THREE_TO_FIVE = 5

            fun from(count: Int): StopLabelCountBucket = when {
                count <= 1 -> ONE
                count == 2 -> TWO
                count <= MAX_THREE_TO_FIVE -> THREE_TO_FIVE
                else -> SIX_PLUS
            }
        }
    }

    /**
     * Fired when a new custom stop label is successfully persisted.
     *
     * Aggregations this enables:
     *  - "What % of users ever create a custom label?" - distinct user count on event name.
     *  - "Where do users create labels?" - group by [creationSurface].
     *  - "Are power users emerging?" - group by [labelCountBucket].
     *
     * Historical rows (before 2026-07-15) instead carried raw `labelName`/`emoji` and a
     * raw `totalLabelsCountAfter` int; treat missing new params as the pre-redesign era.
     *
     * @param creationSurface  Row kind whose "+ New label" chip opened the create sheet.
     * @param labelCountBucket Total labels the user has *after* this creation, bucketed.
     */
    data class StopLabelCreatedEvent(
        val creationSurface: StopLabelSurface,
        val labelCountBucket: StopLabelCountBucket,
    ) : AnalyticsEvent(
        name = "stop_label_created",
        properties = mapOf(
            "creationSurface" to creationSurface.value,
            "labelCountBucket" to labelCountBucket.value,
        ),
    )

    /**
     * Fired when the user pins a location to a label - the core "label is being used"
     * signal. This is the moment a label transitions from empty to actionable.
     *
     * Aggregations this enables:
     *  - "Which surface converts: search result, recent, or empty-state?" - group by
     *    [assignmentSurface].
     *  - "Do users create a label at the moment of assignment or fill Home/Work?" -
     *    group by [assignmentMode].
     *  - "Are users replacing existing pins or filling empty slots?" - filter
     *    [isReassignment].
     *  - "Does the protected Home label behave differently?" - filter [labelKind].
     *
     * Historical rows (before 2026-07-15) carried raw `labelName`/`stopId`/`stopName`
     * and a `source` param whose values (`choose_mode`, `star_sheet`) refer to deleted
     * v2/v3 flows - do not reinterpret them as the new surfaces.
     *
     * @param assignmentSurface Row kind hosting the assign action.
     * @param assignmentMode    [AssignmentMode.NEW_LABEL] when the assignment
     *                          immediately follows the New Label sheet, else
     *                          [AssignmentMode.EXISTING_LABEL].
     * @param locationKind      Transit stop or address/POI being pinned.
     * @param labelKind         Protected default (Home) vs custom label.
     * @param isReassignment    `true` if the label already had a different stop pinned.
     */
    data class StopLabelStopAssignedEvent(
        val assignmentSurface: StopLabelSurface,
        val assignmentMode: AssignmentMode,
        val locationKind: StopSelectedEvent.LocationKind,
        val labelKind: StopLabelKind,
        val isReassignment: Boolean,
    ) : AnalyticsEvent(
        name = "stop_label_stop_assigned",
        properties = mapOf(
            "assignmentSurface" to assignmentSurface.value,
            "assignmentMode" to assignmentMode.value,
            "locationKind" to locationKind.value,
            PROP_LABEL_KIND to labelKind.value,
            "isReassignment" to isReassignment,
        ),
    ) {
        enum class AssignmentMode(val value: String) {
            EXISTING_LABEL("existing_label"),
            NEW_LABEL("new_label"),
        }
    }

    /**
     * Fired when the user either deletes a label entirely or clears the stop attached
     * to it. One event with the [action] discriminator answers the unified
     * "label cleanup behaviour" question without joining two streams.
     *
     * Aggregations this enables:
     *  - "Are users removing labels or just clearing them?" - group by [action].
     *  - "Are users deleting labels they never filled?" - filter [action] = DELETE,
     *    [hadStop] = false.
     *
     * Note: protected Home label deletions are silently no-op'd by the handler, so this
     * event does not fire for them - keeps the data clean from defensive UI calls.
     * Historical rows (before 2026-07-15) carried a raw `labelName`.
     *
     * @param action    [Action.DELETE] when the entire label is removed,
     *                  [Action.CLEAR] when only the stop is unlinked.
     * @param hadStop   Whether the label had a stop attached at the moment of removal.
     * @param labelKind Protected default (Home) vs custom label.
     */
    data class StopLabelRemovedEvent(
        val action: Action,
        val hadStop: Boolean,
        val labelKind: StopLabelKind,
    ) : AnalyticsEvent(
        name = "stop_label_removed",
        properties = mapOf(
            PROP_ACTION to action.value,
            "hadStop" to hadStop,
            PROP_LABEL_KIND to labelKind.value,
            PROP_SURFACE to SURFACE_MANAGE_LABELS,
        ),
    ) {
        enum class Action(val value: String) {
            DELETE("delete"),
            CLEAR("clear"),
        }
    }

    /**
     * Fired once per completed drag in Manage Labels whose final order differs from
     * the starting order - never per intermediate swap, so one long drag is one event.
     *
     * Aggregations this enables:
     *  - "Are stop labels actually being reordered, or do users leave Home/Work in
     *    the seeded order?" - presence/count of this event.
     *  - "Are users moving the protected Home label around?" - filter [labelKind].
     *  - "Promotion vs shuffle behaviour" - [moveDistanceBucket] distribution.
     *
     * Historical rows (before 2026-07-15) fired once per swap mid-drag with raw
     * `labelName`/`previousIndex`/`newIndex`/`totalCount` - counts from that era are
     * inflated and not comparable.
     *
     * @param labelKind          Protected default (Home) vs custom label.
     * @param moveDistanceBucket How far the label travelled, bucketed.
     * @param setLabelCountBucket How many set (draggable) labels existed at drag time.
     */
    data class StopLabelReorderedEvent(
        val labelKind: StopLabelKind,
        val moveDistanceBucket: MoveDistanceBucket,
        val setLabelCountBucket: StopLabelCountBucket,
    ) : AnalyticsEvent(
        name = "stop_label_reordered",
        properties = mapOf(
            PROP_LABEL_KIND to labelKind.value,
            "moveDistanceBucket" to moveDistanceBucket.value,
            "setLabelCountBucket" to setLabelCountBucket.value,
            PROP_SURFACE to SURFACE_MANAGE_LABELS,
        ),
    ) {
        enum class MoveDistanceBucket(val value: String) {
            ONE("1"),
            TWO_TO_THREE("2_3"),
            FOUR_PLUS("4_plus"),
            ;

            companion object {
                private const val MAX_TWO_TO_THREE = 3

                fun from(distance: Int): MoveDistanceBucket = when {
                    distance <= 1 -> ONE
                    distance <= MAX_TWO_TO_THREE -> TWO_TO_THREE
                    else -> FOUR_PLUS
                }
            }
        }
    }

    // endregion

    // region Theme

    data class ThemeSelectedEvent(val themeId: String) : AnalyticsEvent(
        name = "theme_selected",
        properties = mapOf("themeId" to themeId),
    )

    // endregion

    // region PlanTripScreen / DateTimeSelection Screen

    data object ResetTimeClickEvent : AnalyticsEvent("reset_time_click")

    // endregion

    // region TimeTable Screen

    data class ReverseTimeTableClickEvent(val fromStopId: String, val toStopId: String) :
        AnalyticsEvent(
            name = "reverse_time_table_click",
            properties = mapOf(PROP_FROM_STOP_ID to fromStopId, PROP_TO_STOP_ID to toStopId),
        )

    /**
     * @param source Where the save was triggered from: [SOURCE_STAR] for the
     * title-bar star button, [SOURCE_PROMPT] for the "Save this trip?" prompt.
     * Historical events (pre-prompt) carry no `source` — treat null as star.
     */
    data class SaveTripClickEvent(
        val fromStopId: String,
        val toStopId: String,
        val source: String = SOURCE_STAR,
    ) : AnalyticsEvent(
        name = "save_trip_click",
        properties = mapOf(
            PROP_FROM_STOP_ID to fromStopId,
            PROP_TO_STOP_ID to toStopId,
            PROP_SOURCE to source,
        ),
    ) {
        companion object {
            const val SOURCE_STAR = "star"
            const val SOURCE_PROMPT = "prompt"
        }
    }

    /**
     * "Save this trip?" prompt shown on the timetable after loading an unsaved
     * origin-destination pair (the aha-moment save nudge).
     *
     * @param variant `plain` for the simple save prompt; `commute` for the
     * Home/Work label-assigning variant.
     */
    data class SaveTripPromptShownEvent(val variant: String) : AnalyticsEvent(
        name = "save_trip_prompt_shown",
        properties = mapOf(PROP_VARIANT to variant),
    ) {
        companion object {
            const val VARIANT_PLAIN = "plain"
        }
    }

    /**
     * User acted on the "Save this trip?" prompt. One event for both outcomes
     * (accept and dismiss) to stay within the Firebase 500-event budget.
     *
     * @param accepted true when the user tapped Save, false when dismissed.
     * @param dismissCount Total dismissals for this origin-destination pair
     * including this one — the prompt stops appearing for the pair at 2.
     * Always 0 when [accepted] is true.
     */
    data class SaveTripPromptActionEvent(
        val accepted: Boolean,
        val variant: String,
        val dismissCount: Int = 0,
    ) : AnalyticsEvent(
        name = "save_trip_prompt_action",
        properties = mapOf(
            "accepted" to accepted,
            PROP_VARIANT to variant,
            "dismissCount" to dismissCount,
        ),
    )

    data class PlanTripClickEvent(val fromStopId: String, val toStopId: String) : AnalyticsEvent(
        name = "plan_trip_click",
        properties = mapOf(PROP_FROM_STOP_ID to fromStopId, PROP_TO_STOP_ID to toStopId),
    )

    data class ModeClickEvent(
        val fromStopId: String,
        val toStopId: String,
        val displayModeSelectionRow: Boolean,
    ) : AnalyticsEvent(
        name = "mode_click",
        properties = mapOf(
            PROP_FROM_STOP_ID to fromStopId,
            PROP_TO_STOP_ID to toStopId,
            "displayModeSelectionRow" to displayModeSelectionRow,
        ),
    )

    data class ModeSelectionDoneEvent(
        val fromStopId: String,
        val toStopId: String,
        val unselectedProductClasses: Set<Int>,
    ) : AnalyticsEvent(
        name = "mode_selection_done",
        properties = mapOf(
            PROP_FROM_STOP_ID to fromStopId,
            PROP_TO_STOP_ID to toStopId,
            "unselected" to unselectedProductClasses.toString(),
        ),
    )

    data class DateTimeSelectEvent(
        val dayOfWeek: String,
        val time: String,
        val journeyOption: String,
        // User clicked on reset button and then selected the time
        val isReset: Boolean = false,
    ) : AnalyticsEvent(
        name = "date_time_select",
        properties = mapOf(
            "dayOfWeek" to dayOfWeek,
            "time" to time,
            "journeyOption" to journeyOption,
            "isReset" to isReset,
        ),
    )

    /**
     * Fired when the user taps the "Share with Friend" button on an expanded journey card.
     *
     * All values are pre-formatted by the caller using typed objects (e.g. [kotlin.time.Instant],
     * [kotlin.time.Duration]) so the format is guaranteed — no raw display strings are accepted.
     *
     * @param transportModes Comma-separated transport mode names (e.g. `"Train,Bus"`).
     * @param lines          Comma-separated line identifiers in journey order (e.g. `"T1,700"`).
     * @param legCount       Total number of legs in the journey.
     * @param totalTravelTime Duration formatted as `"30 mins"` or `"1h 5m"` — derived from
     *                        `Duration.toFormattedDurationTimeString()`.
     * @param originTime     Departure time formatted as `"8:25 AM"` (12-hour, uppercase AM/PM) —
     *                       derived from `Instant → toLocalDateTime(AEST) → toHHMM()`.
     * @param isPastDeparture `true` when the departure had already passed at share time.
     */
    data class ShareJourneyClickEvent(
        val transportModes: String,
        val lines: String,
        val legCount: Int,
        val totalTravelTime: String,
        val originTime: String,
        val isPastDeparture: Boolean,
    ) : AnalyticsEvent(
        name = "share_journey_click",
        properties = mapOf(
            PROP_TRANSPORT_MODES to transportModes,
            "lines" to lines,
            PROP_LEG_COUNT to legCount,
            "totalTravelTime" to totalTravelTime,
            "originTime" to originTime,
            "isPastDeparture" to isPastDeparture,
        ),
    )

    /**
     * @param transportModes Sorted comma-separated product-class integers of modes in this journey
     *                       (e.g. "1,5" = Train + Bus). Matches the encoding used by [MapOptionsSavedEvent].
     */
    data class JourneyCardToggleEvent(
        val expanded: Boolean,
        val hasStarted: Boolean,
        val legCount: Int,
        val transportModes: String,
    ) : AnalyticsEvent(
        name = "journey_card_toggle",
        properties = mapOf(
            "expanded" to expanded,
            "hasStarted" to hasStarted,
            PROP_LEG_COUNT to legCount,
            PROP_TRANSPORT_MODES to transportModes,
        ),
    )

    /**
     * Fired when the user taps a transport leg row inside an expanded journey card.
     *
     * @param expanded      true = stops list opened, false = collapsed.
     * @param transportMode Human-readable transport mode name (e.g. "Train", "Bus", "Ferry").
     * @param lineName      Line identifier displayed on the leg badge (e.g. "T1", "700", "F2").
     */
    data class JourneyLegClickEvent(
        val expanded: Boolean,
        val transportMode: String,
        val lineName: String,
    ) : AnalyticsEvent(
        name = "journey_leg_click",
        properties = mapOf(
            "expanded" to expanded,
            "transportMode" to transportMode,
            "lineName" to lineName,
        ),
    )

    data class JourneyAlertClickEvent(val fromStopId: String, val toStopId: String) :
        AnalyticsEvent(
            name = "journey_alert_click",
            properties = mapOf(PROP_FROM_STOP_ID to fromStopId, PROP_TO_STOP_ID to toStopId),
        )

    /**
     * Fired when the user taps either affordance on a stop row in the timetable
     * sticky header: the stop name (opens leg-scoped stop search, "Change
     * origin" / "Change destination") or the departures icon (opens the
     * departure-board sheet). One event, split by [action] — same surface and
     * identical params, so a second event name would only burn a slot of the
     * Firebase 500-event budget and force dashboards to UNION two names to
     * trace the departures sheet across the A3 change.
     *
     * The single most useful field is [isOrigin]: it answers
     * "are users more curious about where they're leaving from, or where they're
     * going to?". Combined with [PROP_FROM_STOP_ID] / [PROP_TO_STOP_ID] (which
     * anchor to the same trip referenced by [LoadTimeTableClickEvent] and
     * [PlanTripClickEvent]) the dashboard can also answer
     * "of users who plan a trip, what % drill into stop details before boarding?".
     *
     * @param stopId        The stop the user tapped.
     * @param stopName      Human-readable stop name for that tap.
     * @param isOrigin      `true` if the tapped label was the trip origin,
     *                      `false` if it was the destination.
     * @param tripFromStopId Origin of the trip the click happened inside —
     *                       always present, even when [isOrigin] is true (so
     *                       a single dashboard query can join on the trip pair
     *                       without re-deriving it from [stopId]).
     * @param tripToStopId   Destination of the trip the click happened inside.
     * @param action        What the tap did. Historical events (pre stop-edit)
     *                      carry no `action` and were departures-sheet opens,
     *                      so the sheet's full usage timeline is
     *                      `action IS NULL OR action = open_departures`.
     */
    data class TimeTableStopHeaderClickEvent(
        val stopId: String,
        val stopName: String,
        val isOrigin: Boolean,
        val tripFromStopId: String,
        val tripToStopId: String,
        val action: String,
    ) : AnalyticsEvent(
        name = "timetable_stop_header_click",
        properties = mapOf(
            PROP_STOP_ID to stopId,
            PROP_STOP_NAME to stopName,
            "isOrigin" to isOrigin,
            PROP_FROM_STOP_ID to tripFromStopId,
            PROP_TO_STOP_ID to tripToStopId,
            PROP_ACTION to action,
        ),
    ) {
        companion object {
            /** Stop-name tap opened stop search pre-scoped to the tapped leg. */
            const val ACTION_EDIT_SEARCH = "edit_search"

            /** Departures-icon tap opened the departure-board sheet. */
            const val ACTION_OPEN_DEPARTURES = "open_departures"
        }
    }

    /**
     * Fired when the user taps "Load more" at the bottom of the timetable list to
     * page in the next window of future trips.
     *
     * @param loadMoreCount Pre-increment page index — number of additional pages already
     *   successfully fetched at tap time. Lets the dashboard answer "what % of users go
     *   past N pages?" and tell us whether `MAX_LOAD_MORE_COUNT` is set sensibly.
     * @param isCustomDateTime `true` when the user picked a non-default departure/arrival
     *   time, `false` when they're viewing the default "now". Splits exploratory
     *   pagination from "I want a slightly later option" pagination.
     * @param latestVisibleDepartureMinutesFromNow Minutes between now and the latest
     *   user-visible (filtered) departure when the tap happened. Tells us how far ahead
     *   users are already looking when they reach for more.
     * @param visibleJourneyCount How many journeys were rendered (post-filter) at tap
     *   time. Sparse lists imply pagination is essential, not exploratory.
     */
    data class TimeTableLoadMoreClickEvent(
        val fromStopId: String,
        val toStopId: String,
        val loadMoreCount: Int,
        val isCustomDateTime: Boolean,
        val latestVisibleDepartureMinutesFromNow: Int,
        val visibleJourneyCount: Int,
    ) : AnalyticsEvent(
        name = "timetable_load_more_click",
        properties = mapOf(
            PROP_FROM_STOP_ID to fromStopId,
            PROP_TO_STOP_ID to toStopId,
            "loadMoreCount" to loadMoreCount,
            "isCustomDateTime" to isCustomDateTime,
            "latestVisibleDepartureMinutesFromNow" to latestVisibleDepartureMinutesFromNow,
            "visibleJourneyCount" to visibleJourneyCount,
        ),
    )

    /**
     * Fired when the user taps "Load previous" at the top of the timetable list to
     * page in trips that departed before the earliest currently shown departure.
     *
     * @param isCustomDateTime `true` when the user picked a non-default departure/arrival
     *   time. High `false` volume here means users land on the screen with the wrong
     *   default time and are reaching backwards.
     * @param earliestVisibleDepartureMinutesFromNow Minutes between now and the earliest
     *   user-visible (filtered) departure when the tap happened. Negative when the user
     *   is already looking at past trips, positive when they're scrolling backwards from
     *   a future-scheduled view.
     * @param visibleJourneyCount How many journeys were rendered (post-filter) at tap
     *   time.
     */
    data class TimeTableLoadPreviousClickEvent(
        val fromStopId: String,
        val toStopId: String,
        val isCustomDateTime: Boolean,
        val earliestVisibleDepartureMinutesFromNow: Int,
        val visibleJourneyCount: Int,
    ) : AnalyticsEvent(
        name = "timetable_load_previous_click",
        properties = mapOf(
            PROP_FROM_STOP_ID to fromStopId,
            PROP_TO_STOP_ID to toStopId,
            "isCustomDateTime" to isCustomDateTime,
            "earliestVisibleDepartureMinutesFromNow" to earliestVisibleDepartureMinutesFromNow,
            "visibleJourneyCount" to visibleJourneyCount,
        ),
    )

    // endregion

    // region Generic Events

    /**
     * Fired when the nav back stack produces zero entries, causing the NoEntriesUI fallback
     * to appear. Should never fire after the fixes to resetRoot() and the duplicate toEntries()
     * call. Monitor this event post-release — if it stays silent, remove the fallback.
     *
     * @param topLevelRoute Simple class name of the active top-level route at the time the
     *                      empty state was observed. Helps identify which navigation path
     *                      triggered the condition.
     */
    data class NoEntriesDetectedEvent(
        val topLevelRoute: String,
    ) : AnalyticsEvent(
        name = "no_entries_detected",
        properties = mapOf("topLevelRoute" to topLevelRoute),
    )

    data class BackClickEvent(
        val fromScreen: AnalyticsScreen,
    ) : AnalyticsEvent(
        name = "back_click",
        properties = mapOf(
            "fromScreen" to fromScreen.name,
        ),
    )

    @OptIn(ExperimentalTime::class)
    data class AppStart(
        val platformType: String,
        val appVersion: String,
        val osVersion: String,
        val deviceModel: String,
        val fontSize: String,
        val isDarkTheme: Boolean,
        val krailTheme: Int,
        val locale: String,
        val timeZone: String,
    ) : AnalyticsEvent(
        name = "app_start",
        properties = mapOf(
            "platformType" to platformType.trim(),
            "appVersion" to appVersion.trim(),
            "osVersion" to osVersion.trim(),
            "deviceModel" to deviceModel.trim(),
            "fontSize" to fontSize.trim(),
            "isDarkTheme" to isDarkTheme,
            "krailTheme" to krailTheme,
            "timeStamp" to Clock.System.now().toString(),
            "locale" to locale.trim(),
            "timeZone" to timeZone.trim(),
        ),
    )
    // endregion

    // region Settings

    /**
     * Analytics event for the refer friend button click.
     *
     * @param entryPoint The entry point from which the user referred a friend. E.g. Setting / Intro
     * screen etc.
     */
    data class ReferFriend(val entryPoint: EntryPoint) : AnalyticsEvent(
        name = "refer_friend",
        properties = mapOf(
            "entryPoint" to entryPoint.from,
        ),
    ) {
        enum class EntryPoint(val from: String) {
            SETTINGS("settings"),
            INTRO_BUTTON("intro_button"),
            INTRO_CONTENT_BUTTON("intro_content_button"),
            SAVED_TRIPS("saved_trips_tile"),
        }
    }

    /**
     * Analytics event for the refer friend button click in the intro screen.
     *
     * @param pageType The page number of the intro screen from which the user clicked Let's KRAIL button.
     * @param interactionPages Represents pages list where interaction was performed. with the
     * content inside the intro pages.
     */
    data class IntroLetsKrailClickEvent(
        val pageType: InteractionPage,
        val pageNumber: Int,
    ) : AnalyticsEvent(
        name = "intro_lets_krail",
        properties = mapOf(
            "completedOnPage" to pageType.name,
            "completedOnPageNumber" to pageNumber,
        ),
    ) {
        enum class InteractionPage {
            SAVE_TRIPS, REAL_TIME_ROUTES, ALERTS, PLAN_TRIP, SELECT_MODE, INVITE_FRIENDS, PARK_RIDE,
        }
    }

    data object SettingsHowToUseClickEvent : AnalyticsEvent(
        name = "how_to_krail",
    )

    data object OurStoryClick : AnalyticsEvent(
        name = "our_story",
    )

    data class SocialConnectionLinkClickEvent(
        val socialPlatformType: SocialPlatformType,
        val source: SocialConnectionSource,
    ) : AnalyticsEvent(
        name = "social_connection_link_click",
        properties = mapOf(
            "socialPlatform" to socialPlatformType.platformName,
            PROP_SOURCE to source.source,
        ),
    ) {
        enum class SocialPlatformType(val platformName: String) {
            LINKEDIN("linkedin"), REDDIT("reddit"), INSTAGRAM("instagram"), FACEBOOK("facebook"),
        }

        enum class SocialConnectionSource(val source: String) {
            SETTINGS("settings"), DISCOVER_CARD("discover_card"),
        }
    }

    // endregion

    // region Park and Ride

    /**
     * Analytics event for the Park and Ride card click.
     * @param stopId The ID of the stop associated with the Park and Ride facility.
     * @param facilityId The ID of the Park and Ride facility.
     * @param expand Indicates whether the card is being expanded or collapsed.
     * @param time - when the card was clicked, format - epoch time in seconds.
     */
    data class ParkRideCardClickEvent
    @OptIn(ExperimentalTime::class)
    constructor(
        val stopId: String,
        val facilityId: String,
        val expand: Boolean,
        val time: Long = Clock.System.now().epochSeconds,
    ) : AnalyticsEvent(
        name = "park_ride_card_click",
        properties = mapOf(
            PROP_STOP_ID to stopId.trim(),
            "facilityId" to facilityId.trim(),
            PROP_EXPAND to expand.toString().trim(),
            "time" to time.toString().trim(),
        ),
    )

    /**
     * Fired when a rider adds or removes a Park & Ride facility they picked themselves.
     *
     * Add and remove are two outcomes of one intent (managing your own Park & Ride list), so
     * they share a name and split on [action] rather than spending two event slots. [source]
     * is here so a second entry point (e.g. adding from search) folds into this event too.
     *
     * @param facilityId The ID of the Park and Ride facility.
     * @param stopId The ID of the stop associated with the facility.
     */
    data class ParkRideUserFacilityEvent(
        val facilityId: String,
        val stopId: String,
        val action: Action,
        val source: Source,
    ) : AnalyticsEvent(
        name = "park_ride_user_facility",
        properties = mapOf(
            "facilityId" to facilityId.trim(),
            PROP_STOP_ID to stopId.trim(),
            PROP_ACTION to action.value,
            PROP_SOURCE to source.value,
        ),
    ) {
        enum class Action(val value: String) {
            ADD("add"),
            REMOVE("remove"),
        }

        enum class Source(val value: String) {
            PICKER("picker"),
            HOME("home"),
        }
    }
    // endregion

    // region Discover

    data object DiscoverButtonClick : AnalyticsEvent(
        name = "discover_button_click",
    )

    data class DiscoverCardClick(
        val location: String = "SYD",
        val source: Source,
        val cardId: String,
        val cardType: CardType,
        val partnerSocialLink: PartnerSocialLink? = null,
    ) : AnalyticsEvent(
        name = "discover_card_click",
        properties = mutableMapOf(
            // "location" to location,
            PROP_SOURCE to source.actionName,
            "cardId" to cardId,
            "cardType" to cardType.displayName,
        ).apply {
            partnerSocialLink?.let { socialLink ->
                put("partnerSocialPlatformName", socialLink.type.platformName)
                put("partnerSocialPlatformUrl", socialLink.url)
            }
        },
    ) {
        data class PartnerSocialLink(
            val type: SocialConnectionLinkClickEvent.SocialPlatformType,
            val url: String,
        )

        enum class CardType(val displayName: String) {
            TRAVEL(displayName = "Travel"),
            EVENTS(displayName = "Events"),
            FOOD(displayName = "Food"),
            SPORTS(displayName = "Sports"),
            UNKNOWN(displayName = "unknown"),
        }

        enum class Source(val actionName: String) {
            CTA_CLICK("cta_click"),
            SHARE_CLICK("share"),
            PARTNER_SOCIAL_LINK("partner_social_link"),
        }
    }

    data class DiscoverCardSessionComplete(
        val cardSeenCount: Int,
        val location: String = "SYD",
    ) : AnalyticsEvent(
        name = "discover_session_complete",
        properties = mapOf(
            "cardSeenCount" to cardSeenCount,
            "location" to location,
        ),
    )

    data class DiscoverFilterChipSelected(
        val cardType: DiscoverCardClick.CardType,
    ) : AnalyticsEvent(
        name = "discover_filter_chip_selected",
        properties = mapOf(
            "cardType" to cardType.displayName,
        ),
    )

    // endregion

    // region SearchStopMap

    /**
     * Fired when the user taps the "Select on map" button in the SearchStop list.
     * Use to measure how many users discover and use the map entry point.
     */
    data object SelectOnMapButtonClickEvent : AnalyticsEvent(name = "select_on_map_button_click")

    /**
     * Fired when the user taps "Save" on the MapOptionsBottomSheet.
     *
     * This single event is the source of truth for all map-options analysis. One rich snapshot
     * replaces what would otherwise be 4+ separate events. Use it to answer:
     *  - "What % of users prefer 5km radius?" → aggregate on [radiusKm]
     *  - "What transport mode filter combos are most common?" → aggregate on [transportModes]
     *  - "Do users actually change the radius or just leave it at default?" → filter [radiusChanged]
     *  - "Do map control toggles matter?" → look at [showDistanceScale] / [showCompass] distributions
     *
     * @param radiusKm          The saved search radius: 1.0, 3.0, or 5.0.
     * @param transportModes    Sorted, comma-separated product-class integers of all *enabled* modes
     *                          (e.g. "1,2,5,9").
     * @param showDistanceScale Whether the distance scale overlay is enabled after saving.
     * @param showCompass       Whether the compass overlay is enabled after saving.
     * @param radiusChanged     Whether [radiusKm] differs from the value before the sheet opened.
     * @param modesChanged      Whether any transport mode filter changed from the previous state.
     */
    data class MapOptionsSavedEvent(
        val radiusKm: Double,
        val transportModes: String,
        val showDistanceScale: Boolean,
        val showCompass: Boolean,
        val radiusChanged: Boolean,
        val modesChanged: Boolean,
    ) : AnalyticsEvent(
        name = "search_stop_map_options_saved",
        properties = mapOf(
            "radiusKm" to radiusKm,
            PROP_TRANSPORT_MODES to transportModes,
            "showDistanceScale" to showDistanceScale,
            "showCompass" to showCompass,
            "radiusChanged" to radiusChanged,
            "modesChanged" to modesChanged,
        ),
    )

    /**
     * Fired when the user selects a stop via the map's StopDetailsBottomSheet.
     *
     * Captures the map configuration context at the moment of selection, so you can correlate
     * search settings with successful stop discovery:
     *  - Does a wider [searchRadiusKm] lead to more stop selections?
     *  - Do users with fewer [enabledModesCount] find stops faster?
     *  - Does having [hadUserLocation] improve selection rate?
     *
     * NOTE: GPS coordinates are intentionally NOT captured — neither user location nor map centre.
     * Logging lat/lon (even anonymously) requires declaring "Precise Location" data collection in
     * both the Apple App Privacy label and the Google Play Data Safety section. Use the boolean
     * [hadUserLocation] flag instead, which carries no privacy obligations.
     *
     * @param stopId             The ID of the stop the user selected.
     * @param searchRadiusKm     The active search radius at the moment of selection.
     * @param enabledModesCount  Number of transport modes currently enabled (not which ones —
     *                           use [MapOptionsSavedEvent.transportModes] for mode-level detail).
     * @param nearbyStopsCount   Number of stop markers visible on the map at time of selection.
     * @param hadUserLocation    Whether the device location was available (permission granted +
     *                           location resolved). Does NOT include coordinates.
     */
    data class StopSelectedFromMapEvent(
        val stopId: String,
        val searchRadiusKm: Double,
        val enabledModesCount: Int,
        val nearbyStopsCount: Int,
        val hadUserLocation: Boolean,
    ) : AnalyticsEvent(
        name = "stop_selected_from_map",
        properties = mapOf(
            PROP_STOP_ID to stopId,
            "searchRadiusKm" to searchRadiusKm,
            "enabledModesCount" to enabledModesCount,
            "nearbyStopsCount" to nearbyStopsCount,
            "hadUserLocation" to hadUserLocation,
        ),
    )

    /**
     * Fired when the user taps a stop marker on the SearchStopMap (the bottom sheet opens).
     *
     * This is the first step of the map selection funnel:
     *   nearby_stop_click → (user reviews sheet) → stop_selected_from_map
     *
     * Use the click→select ratio in BigQuery to understand drop-off:
     *   COUNT(stop_selected_from_map) / COUNT(nearby_stop_click) per user session.
     *
     * @param stopId             The stop that was tapped.
     * @param transportModesCount Number of transport modes served at this stop.
     *                            Stops with more modes may attract more exploratory taps.
     */
    data class NearbyStopClickEvent(
        val stopId: String,
        val transportModesCount: Int,
    ) : AnalyticsEvent(
        name = "nearby_stop_click",
        properties = mapOf(
            PROP_STOP_ID to stopId,
            "transportModesCount" to transportModesCount,
        ),
    )

    /**
     * Fired when the user taps the Options button on the map (SearchStopMap only).
     * Use to measure how often users discover and open map configuration.
     */
    data object MapOptionsOpenedEvent : AnalyticsEvent(name = "search_stop_map_options_opened")

    /**
     * Fired when the user taps "Go to Settings" on the location permission denied banner.
     *
     * Tells you how many users hit the permission wall and were motivated enough to open Settings.
     * Use this together with [MapLocationButtonClickEvent] (isLocationActive = false) to measure
     * the permission recovery funnel:
     *   location_button tap (no permission) → banner shown → settings click → (hopefully) grant
     *
     * [source] identifies which map screen the banner was shown on.
     */
    data class LocationPermissionSettingsClickEvent(
        val source: Source,
    ) : AnalyticsEvent(
        name = "location_permission_settings_click",
        properties = mapOf(PROP_SOURCE to source.value),
    ) {
        enum class Source(val value: String) {
            SEARCH_STOP_MAP("search_stop_map"),
            JOURNEY_MAP("journey_map"),
        }
    }

    /**
     * Fired when the user taps the User Location button on any map screen.
     *
     * [isLocationActive] tells you the state at tap time:
     *  - true  → user already had location and is re-centering the camera
     *  - false → user is attempting to start location (no permission yet, or was denied)
     *
     * [source] identifies which map screen the tap came from, so you can compare
     * location usage between the stop-search flow and the journey-viewing flow.
     *
     * In BigQuery, filter `isLocationActive = false` to identify users who want location
     * but can't get it. No coordinates are captured.
     */
    data class MapLocationButtonClickEvent(
        val isLocationActive: Boolean,
        val source: Source,
    ) : AnalyticsEvent(
        name = "user_location_button_click",
        properties = mapOf(
            "isLocationActive" to isLocationActive,
            PROP_SOURCE to source.value,
        ),
    ) {
        enum class Source(val value: String) {
            SEARCH_STOP_MAP("search_stop_map"),
            JOURNEY_MAP("journey_map"),
        }
    }

    // endregion

    // region JourneyMap

    // endregion

    // region DepartureBoard

    /**
     * Identifies which surface fired a departure board event.
     *
     *  - [TIMETABLE_SHEET]  — stop sheet opened via timetable header tap ([DeparturesViewModel])
     *  - [MAP_SHEET]        — stop sheet opened via map pin tap ([DeparturesViewModel])
     */
    enum class DepartureBoardSource(val value: String) {
        TIMETABLE_SHEET("timetable_sheet"),
        MAP_SHEET("map_sheet"),
    }

    /**
     * Fired when departure polling starts for a stop (i.e. the board is opened / becomes active).
     * Use to measure how often users check live departures and which stops are most viewed.
     *
     * @param stopId   NSW Transport stop ID being polled.
     * @param stopName Human-readable stop name shown in the UI.
     * @param source   Which surface opened the board.
     */
    data class DepartureBoardScreenViewEvent(
        val stopId: String,
        val stopName: String,
        val source: DepartureBoardSource,
    ) : AnalyticsEvent(
        name = "dep_board_screen_view",
        properties = mapOf(
            PROP_STOP_ID to stopId,
            PROP_STOP_NAME to stopName,
            PROP_SOURCE to source.value,
        ),
    )

    /**
     * Fired when the user taps a line chip to filter or clear departures.
     * Use to understand which lines users care about most at each stop and
     * how often they reset the filter.
     *
     * @param stopId        Stop where the chip was tapped.
     * @param selected      `true` = filter applied; `false` = filter cleared.
     * @param lineNumber    The affected line, e.g. "T1", "333". Always present — even when
     *                      [selected] is `false`, the previously selected line is passed so
     *                      we know which line was deselected.
     * @param transportMode Human-readable mode name, e.g. "Train". Always present alongside [lineNumber].
     * @param source        Which surface the tap came from.
     */
    data class DepartureBoardLineFilterClickEvent(
        val stopId: String,
        val selected: Boolean,
        val lineNumber: String? = null,
        val transportMode: String? = null,
        val source: DepartureBoardSource,
    ) : AnalyticsEvent(
        name = "dep_board_line_filter_click",
        properties = buildMap {
            put(PROP_STOP_ID, stopId)
            put("selected", selected)
            put(PROP_SOURCE, source.value)
            lineNumber?.let { put("lineNumber", it) }
            transportMode?.let { put("transportMode", it) }
        },
    )

    /**
     * Fired when the user toggles the "Show / Hide previous departures" panel.
     * Use to measure demand for past departure data and validate the UX of the toggle.
     *
     * @param stopId Stop whose previous departures the user wants to see.
     * @param show   `true` = user opened the panel; `false` = user closed it.
     * @param source Which surface the toggle was used on.
     */
    data class DepartureBoardShowPreviousEvent(
        val stopId: String,
        val show: Boolean,
        val source: DepartureBoardSource,
    ) : AnalyticsEvent(
        name = "dep_board_show_previous",
        properties = mapOf(
            PROP_STOP_ID to stopId,
            "show" to show,
            PROP_SOURCE to source.value,
        ),
    )

    /**
     * Fired when the user expands or collapses a departure board card on any surface.
     * Use to track open/close patterns and which stops users most frequently monitor.
     *
     * @param stopId   Stop whose board was toggled.
     * @param stopName Human-readable stop name.
     * @param expand   `true` = card is being expanded; `false` = card is being collapsed.
     * @param source   Which surface the toggle happened on.
     */
    data class DepartureBoardToggleEvent(
        val stopId: String,
        val stopName: String,
        val expand: Boolean,
        val source: DepartureBoardSource,
    ) : AnalyticsEvent(
        name = "dep_board_toggle",
        properties = mapOf(
            PROP_STOP_ID to stopId,
            PROP_STOP_NAME to stopName,
            PROP_EXPAND to expand,
            PROP_SOURCE to source.value,
        ),
    )

    /**
     * Fired for error/retry lifecycle events on the departure board.
     * Consolidates two states into one event to stay within the Firebase 500-event budget.
     * Compute retry rate as `retry_count / error_count` per stop/source.
     *
     * [Action.ERROR] — first transition to error state per polling session (auto-fired).
     * [Action.RETRY] — user taps Retry after a load failure (user-initiated).
     *
     * @param stopId Stop affected by the status change.
     * @param action What happened — error or user retry.
     * @param source Which surface the event came from.
     */
    data class DepartureBoardStatusEvent(
        val stopId: String,
        val action: Action,
        val source: DepartureBoardSource,
    ) : AnalyticsEvent(
        name = "dep_board_status",
        properties = mapOf(
            PROP_STOP_ID to stopId,
            PROP_ACTION to action.value,
            PROP_SOURCE to source.value,
        ),
    ) {
        enum class Action(val value: String) {
            ERROR("error"),
            RETRY("retry"),
        }
    }

    // endregion

    // region InfoTiles

    data class InfoTileInteraction(
        val key: String,
        val expand: Boolean? = null,
        val dismiss: Boolean? = null,
        val ctaUrl: String? = null,
    ) : AnalyticsEvent(
        name = "info_tile_interaction",
        properties = mutableMapOf<String, Any>(
            "key" to key,
        ).apply {
            dismiss?.let { put("dismiss", dismiss) }
            ctaUrl?.let { put("cta_click", ctaUrl) }
            expand?.let { put(PROP_EXPAND, expand) }
        },
    )

    // endregion
}
