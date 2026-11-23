package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.lifecycle.SavedStateHandle
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.testing.coroutines.KrailTestScope
import xyz.ksharma.krail.io.gtfs.readiness.StopsReadiness
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroSearchPhase
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.LocationResult
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideAvailabilityState
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardPhase
import kotlin.time.Duration.Companion.milliseconds

/**
 * Everything the rider, the system and time can do to the Intro in a test, as one list of values, so a search over them
 * can replay a path through the real view model and the fakes at its edges.
 */
internal sealed interface WorldAction {
    /** The rider, or a screen, sends an event. */
    data class Send(val event: IntroUiEvent) : WorldAction

    /** Virtual time passes, which is how a wait for the stops that never answers ends. */
    data class Advance(val ms: Long) : WorldAction

    /** The one availability call the Park & Ride card waits for comes back, with a reading or without one. */
    data class LandAvailability(val withReading: Boolean) : WorldAction
}

/** Where a replay starts: a first run, what the system saved with the screen, or what the device kept. */
internal sealed interface WorldRoot {
    data object Fresh : WorldRoot

    /** The process was killed and the system gave the view model back the state it had saved. */
    data class SystemSaved(val state: IntroState) : WorldRoot

    /** The app was swiped away and the next launch read the progress the device kept. */
    data class DeviceProgress(val state: IntroState) : WorldRoot
}

/**
 * What identifies a place in the Intro for the search. The transient fields that no button depends on, the typed text, its
 * results, the stations near the rider and the badges, are left out, so two worlds that only differ in them are one place.
 */
internal data class WorldNode(
    val state: IntroState,
    val finaleStarted: Boolean,
    val finished: Boolean,
    val availabilityLanded: Boolean,
)

private val FAR_FROM_ANY_STATION = LatLng(latitude = -33.0, longitude = 151.0)

/** One view model, built the way a screen builds it, that a path of [WorldAction]s is replayed on. */
@OptIn(ExperimentalCoroutinesApi::class)
internal class IntroWorld(
    private val scope: KrailTestScope,
    readiness: StopsReadiness,
    root: WorldRoot = WorldRoot.Fresh,
) {
    private val availability = CompletableDeferred<Unit>()
    private var landWithReading = false

    val rig: IntroRig = when (root) {
        WorldRoot.Fresh -> IntroRig(scope, readiness)
        is WorldRoot.SystemSaved -> IntroRig(scope, readiness, handle = savedAt(root.state))
        is WorldRoot.DeviceProgress -> IntroRig(scope, readiness, handle = SavedStateHandle()).also {
            it.progressPreferences.setString(INTRO_PROGRESS_KEY, root.state.toProgress().toText())
        }
    }

    val effects = mutableListOf<IntroEffect>()
    private val collector: Job

    init {
        // The one availability call waits here until the path says it comes back, so a card can be held on Loading.
        rig.loader.onRefresh = {
            availability.await()
            if (landWithReading) {
                rig.parkRideSandook.insertOrReplaceAll(
                    listOf(reading(facilityId = "31", stopId = "2153478")),
                )
            }
        }
        collector = scope.launch(UnconfinedTestDispatcher(scope.scheduler)) {
            rig.viewModel.effects.collect { effects += it }
        }
        rig.settle()
    }

    val state: IntroState get() = rig.state

    fun apply(action: WorldAction) {
        when (action) {
            is WorldAction.Send -> rig.send(action.event)
            is WorldAction.Advance -> {
                rig.testTime += action.ms.milliseconds
                scope.pumpOnce(action.ms)
            }
            is WorldAction.LandAvailability -> {
                landWithReading = action.withReading
                availability.complete(Unit)
                scope.runCurrent()
            }
        }
    }

    fun node(): WorldNode {
        val state = rig.state
        val asking = state.step == IntroStep.PARK_RIDE
        return WorldNode(
            state = state.copy(
                query = "",
                search = IntroSearchPhase.Idle,
                nearbyStops = persistentListOf(),
                stopModes = persistentMapOf(),
                // What the Park & Ride card and the location said only matters on the screen that shows them.
                parkRideCard = if (asking) state.parkRideCard else state.parkRideCard.settled(),
                locationResult = if (asking) state.locationResult else LocationResult.NOT_ASKED,
            ),
            finaleStarted = IntroEffect.StartFinale in effects,
            finished = IntroEffect.NavigateToSavedTrips in effects,
            availabilityLanded = asking && availability.isCompleted,
        )
    }

    private fun ParkRideCardPhase.settled(): ParkRideCardPhase = when (this) {
        is ParkRideCardPhase.Card -> ParkRideCardPhase.Card(
            station.copy(carParks = persistentListOf(), availability = ParkRideAvailabilityState.Unavailable),
        )
        else -> this
    }

    fun dispose() {
        collector.cancel()
        rig.clearViewModel()
    }
}

/**
 * What a rider or the system can do on [step]: the events that screen takes and the passing of time. It is the list the search
 * tries from every place on that step, and it is also the specification `IntroTapSafetyTest` holds the view model to, from the
 * other side: every other event, on every step, must change nothing.
 */
internal fun worldActions(step: IntroStep): List<WorldAction> = when (step) {
    IntroStep.PARK_RIDE -> listOf(
        WorldAction.Send(IntroUiEvent.AnswerParkRide(yes = true)),
        WorldAction.Send(IntroUiEvent.AnswerParkRide(yes = false)),
        WorldAction.Send(
            IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, FOUR_POINT_SIX_KM_FROM_BELLA_VISTA),
        ),
        WorldAction.Send(IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, FAR_FROM_ANY_STATION)),
        WorldAction.Send(IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, null)),
        WorldAction.Send(IntroUiEvent.LocationResultReceived(LocationResult.DENIED, null)),
        WorldAction.Send(IntroUiEvent.LocationResultReceived(LocationResult.UNAVAILABLE, null)),
        WorldAction.Send(IntroUiEvent.AddParkRideAndContinue),
        WorldAction.Send(IntroUiEvent.DeclineParkRide),
        WorldAction.Send(IntroUiEvent.AcknowledgeNoStation),
        WorldAction.Advance(PARK_RIDE_STOPS_WAIT_MS),
        WorldAction.LandAvailability(withReading = true),
        WorldAction.LandAvailability(withReading = false),
    )
    IntroStep.DESTINATION, IntroStep.ORIGIN -> listOf(
        WorldAction.Send(IntroUiEvent.SelectStop(CENTRAL_STOP)),
        WorldAction.Send(IntroUiEvent.SelectStop(TALLAWONG_STOP)),
        WorldAction.Send(IntroUiEvent.ConfirmStop),
    )
    IntroStep.CONFIRM -> listOf(
        WorldAction.Send(IntroUiEvent.AnswerSaveTrip(yes = true)),
        WorldAction.Send(IntroUiEvent.AnswerSaveTrip(yes = false)),
    )
    IntroStep.THEME -> listOf(
        WorldAction.Send(IntroUiEvent.PickTheme(KrailThemeStyle.Train.id)),
        WorldAction.Send(IntroUiEvent.PickTheme(KrailThemeStyle.Bus.id)),
        WorldAction.Send(IntroUiEvent.ConfirmTheme),
        WorldAction.Advance(FINISH_WAIT_TIMEOUT_MS),
    )
}

internal data class WorldEdge(
    val from: WorldNode,
    val action: WorldAction,
    val to: WorldNode,
)

/** A place found by the search, and how to get back to it: the root and the path from it. */
internal data class WorldRecipe(val root: WorldRoot, val path: List<WorldAction>)

internal class WorldGraph(
    val nodes: LinkedHashMap<WorldNode, WorldRecipe>,
    val edges: List<WorldEdge>,
    /** The places the system or the device would reopen the Intro on, for each place that was found. */
    val restores: List<Pair<WorldNode, WorldNode>>,
) {
    fun edgesFrom(node: WorldNode): List<WorldEdge> = edges.filter { it.from == node }

    /** Every place from which some path of rider, system or time actions leads to a finished Intro. */
    fun placesThatCanFinish(): Set<WorldNode> {
        val reaches = nodes.keys.filterTo(mutableSetOf()) { it.finished }
        var grew = true
        while (grew) {
            grew = edges.any { it.from !in reaches && it.to in reaches && reaches.add(it.from) }
        }
        return reaches
    }
}

private class Pending(val root: WorldRoot, val path: List<WorldAction>, val from: WorldNode?, val action: WorldAction?)

/**
 * Searches every place the Intro can be in, breadth first, by replaying each path on a fresh view model: the rider's every
 * event, the passing of time, the availability call coming back, and the process being killed and the Intro reopened from
 * what the system or the device kept. [extraActions] gives the events a place's own buttons send, so a button that is not in
 * the catalogue is still pressed. The state space is small, so the whole of it is visited, not a sample.
 */
internal fun KrailTestScope.exploreWorld(readiness: StopsReadiness, maxPlaces: Int = MAX_PLACES): WorldGraph {
    val nodes = LinkedHashMap<WorldNode, WorldRecipe>()
    val edges = mutableListOf<WorldEdge>()
    val restores = mutableListOf<Pair<WorldNode, WorldNode>>()
    val queue = ArrayDeque<Pending>()
    queue += Pending(WorldRoot.Fresh, emptyList(), null, null)
    while (queue.isNotEmpty()) {
        val next = queue.removeFirst()
        val world = IntroWorld(this, readiness, next.root)
        next.path.forEach(world::apply)
        val here = world.node()
        val bar = introBar(world.state)
        val barEvents = listOfNotNull(bar?.left?.event, bar?.right?.event).map { WorldAction.Send(it) }
        world.dispose()
        val from = next.from
        val action = next.action
        if (from != null && action != null) {
            if (next.path.isEmpty()) restores += from to here else edges += WorldEdge(from, action, here)
        }
        if (here in nodes) continue
        check(nodes.size < maxPlaces) { "more than $maxPlaces places, so the search is not finishing" }
        nodes[here] = WorldRecipe(next.root, next.path)
        if (here.finaleStarted) {
            // The finale covers the screen and every event is dropped from here, which `IntroTapSafetyTest` holds. Only time
            // matters now, because it is what ends the wait for the stops.
            worldActions(IntroStep.THEME).filterIsInstance<WorldAction.Advance>()
                .forEach { queue += Pending(next.root, next.path + it, here, it) }
            continue
        }
        (worldActions(here.state.step) + barEvents).distinct().forEach { queue += Pending(next.root, next.path + it, here, it) }
        // A process killed here is reopened on what the system saved with the screen, or on what the device kept.
        val restore = WorldAction.Advance(0)
        queue += Pending(WorldRoot.SystemSaved(here.state), emptyList(), here, restore)
        queue += Pending(WorldRoot.DeviceProgress(here.state), emptyList(), here, restore)
    }
    return WorldGraph(nodes, edges, restores)
}

private const val MAX_PLACES = 5_000
