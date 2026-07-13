package xyz.ksharma.krail.trip.planner.ui.searchstop

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import xyz.ksharma.krail.core.testing.fakes.FakeAnalytics
import xyz.ksharma.krail.core.testing.fakes.FakeFlag
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeNearbyStopsManagerForMap
import xyz.ksharma.krail.core.testing.fakes.FakeSandook
import xyz.ksharma.krail.core.testing.fakes.FakeSandookPreferences
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeRemoteAddressResultsManager
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeStopResultsManager
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.trip.planner.ui.components.LABEL_NAME_MAX_LENGTH
import xyz.ksharma.krail.trip.planner.ui.searchstop.SearchStopViewModel
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for the label-event handlers on [SearchStopViewModel] — the slice of `onEvent`
 * that mutates `stopLabels` (assign, create, clear, delete, reorder) plus the lifetime
 * `observeStopLabels` flow.
 *
 * These complement the pure-function tests in `SearchStopRulesTest` (which lock down
 * "what should the UI render?") and the Compose tests in
 * `SearchStopScreenInteractionTest` (which lock down "did the rules actually drive
 * what got rendered?"). The VM tests here lock down "did the handler write the right
 * thing to state and to the DB?".
 *
 * Each handler is asserted on three axes where applicable:
 * - **Optimistic state update** — `_uiState` reflects the change before any IO.
 * - **DB persistence** — the right rows land in [FakeSandook] so a process restart
 *   would replay the same UI state.
 * - **Side effects** — recents pinning for AssignLabelStop, no-op guards on protected
 *   labels for DeleteLabel.
 *
 * Pattern: each test subscribes to `uiState` via Turbine so the underlying
 * `WhileSubscribed` flow stays hot, fires the event, advances the virtual clock so
 * background coroutines settle, then asserts on `expectMostRecentItem()`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchStopViewModelLabelHandlersTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fakeAnalytics = FakeAnalytics()
    private val fakeStopResultsManager = FakeStopResultsManager()
    private val fakeRemoteAddressResultsManager = FakeRemoteAddressResultsManager()
    private val fakeFlag = FakeFlag()
    private val fakeNearbyStopsManager = FakeNearbyStopsManagerForMap()
    private val fakePreferences = FakeSandookPreferences()
    private val fakeSandook = FakeSandook()

    private lateinit var viewModel: SearchStopViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        // Pre-seed Home + Work into the fake DB. Without this, the VM's
        // observeStopLabels() empty-rows branch would seed defaults on its own
        // schedule and tests would race against that initial write.
        fakeSandook.upsertStopLabel("Home", "🏠", null, null, 0L)
        fakeSandook.upsertStopLabel("Work", "💼", null, null, 1L)

        viewModel = SearchStopViewModel(
            analytics = fakeAnalytics,
            stopResultsManager = fakeStopResultsManager,
            remoteAddressResultsManager = fakeRemoteAddressResultsManager,
            flag = fakeFlag,
            nearbyStopsManager = fakeNearbyStopsManager,
            ioDispatcher = testDispatcher,
            preferences = fakePreferences,
            sandook = fakeSandook,
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // region AssignLabelStop

    @Test
    fun `Given unset label When AssignLabelStop fires Then state and sandook reflect attached stop`() =
        runTest {
            viewModel.uiState.test {
                advanceUntilIdle()

                val central = StopItem(stopId = "stop_central", stopName = "Central Station")
                viewModel.onEvent(SearchStopUiEvent.AssignLabelStop("Home", central))
                advanceUntilIdle()

                val state = expectMostRecentItem()
                val home = state.stopLabels.first { it.label == "Home" }
                assertEquals("stop_central", home.stopId)
                assertEquals("Central Station", home.stopName)

                // DB was also written — a fresh VM with the same fakeSandook would
                // recover the same state.
                val rows = fakeSandook.observeStopLabels().first()
                val dbHome = rows.first { it.label == "Home" }
                assertEquals("stop_central", dbHome.stop_id)
                assertEquals("Central Station", dbHome.stop_name)
            }
        }

    @Test
    fun `Given AssignLabelStop When handler runs Then stop is also pinned to recents`() =
        runTest {
            viewModel.uiState.test {
                advanceUntilIdle()

                val central = StopItem(stopId = "stop_central", stopName = "Central Station")
                viewModel.onEvent(SearchStopUiEvent.AssignLabelStop("Home", central))
                advanceUntilIdle()

                // Pinning to recents is the contract that means tapping a saved pill
                // also lights up that stop in Recents next time the screen opens, so
                // the user can still get to it after removing the label.
                val recents = fakeSandook.selectRecentSearchLocations()
                assertTrue(
                    recents.any { it.locationId == "stop_central" },
                    "expected stop_central in recents, got ${recents.map { it.locationId }}",
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    // endregion

    // region RenameLabel

    @Test
    fun `Given new unique name When RenameLabel fires Then label is renamed in state and DB`() =
        runTest {
            viewModel.uiState.test {
                advanceUntilIdle()

                viewModel.onEvent(SearchStopUiEvent.RenameLabel(labelKey = "Work", newName = "Office"))
                advanceUntilIdle()

                val state = expectMostRecentItem()
                assertTrue(state.stopLabels.none { it.label == "Work" })
                assertNotNull(state.stopLabels.firstOrNull { it.label == "Office" })

                val rows = fakeSandook.observeStopLabels().first()
                assertTrue(rows.none { it.label == "Work" })
                assertTrue(rows.any { it.label == "Office" })
            }
        }

    @Test
    fun `Given name already used by another label When RenameLabel fires Then nothing changes`() =
        runTest {
            viewModel.uiState.test {
                advanceUntilIdle()
                expectMostRecentItem()

                // "home" collides with the seeded "Home" case-insensitively after
                // normaliseLabelName — must silently no-op, same as CreateLabel's
                // dedupe. The row-level UI is responsible for surfacing this to the
                // user via a confirm sheet before ever sending the event.
                viewModel.onEvent(SearchStopUiEvent.RenameLabel(labelKey = "Work", newName = "home"))
                advanceUntilIdle()

                expectNoEvents()
                val labels = viewModel.uiState.value.stopLabels
                assertNotNull(labels.firstOrNull { it.label == "Work" })
                assertNotNull(labels.firstOrNull { it.label == "Home" })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `Given blank new name When RenameLabel fires Then nothing changes`() = runTest {
        viewModel.uiState.test {
            advanceUntilIdle()
            expectMostRecentItem()

            viewModel.onEvent(SearchStopUiEvent.RenameLabel(labelKey = "Work", newName = "   "))
            advanceUntilIdle()

            expectNoEvents()
            assertNotNull(viewModel.uiState.value.stopLabels.firstOrNull { it.label == "Work" })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Given new name equal to current name When RenameLabel fires Then nothing changes`() =
        runTest {
            viewModel.uiState.test {
                advanceUntilIdle()
                expectMostRecentItem()

                viewModel.onEvent(SearchStopUiEvent.RenameLabel(labelKey = "Work", newName = "Work"))
                advanceUntilIdle()

                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `Given unknown label key When RenameLabel fires Then nothing changes`() = runTest {
        viewModel.uiState.test {
            advanceUntilIdle()
            val before = expectMostRecentItem().stopLabels

            viewModel.onEvent(SearchStopUiEvent.RenameLabel(labelKey = "Ghost", newName = "Whatever"))
            advanceUntilIdle()

            expectNoEvents()
            assertEquals(before, viewModel.uiState.value.stopLabels)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Given protected Home label When RenameLabel fires Then label is preserved`() = runTest {
        viewModel.uiState.test {
            advanceUntilIdle()
            expectMostRecentItem()

            viewModel.onEvent(SearchStopUiEvent.RenameLabel(labelKey = StopLabel.PROTECTED_LABEL, newName = "Casa"))
            advanceUntilIdle()

            // Handler early-returns; existing state still contains Home, untouched.
            // Defence in depth — the UI also hides the rename field for Home.
            expectNoEvents()
            val state = viewModel.uiState.value
            assertTrue(state.stopLabels.any { it.label == "Home" })
            assertTrue(state.stopLabels.none { it.label == "Casa" })

            // DB row also preserved.
            val rows = fakeSandook.observeStopLabels().first()
            assertTrue(rows.any { it.label == "Home" })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Given home key with mixed case When RenameLabel fires Then label is preserved`() = runTest {
        viewModel.uiState.test {
            advanceUntilIdle()
            expectMostRecentItem()

            viewModel.onEvent(SearchStopUiEvent.RenameLabel(labelKey = "home", newName = "Casa"))
            advanceUntilIdle()

            expectNoEvents()
            val state = viewModel.uiState.value
            assertTrue(state.stopLabels.any { it.label == "Home" })
            assertTrue(state.stopLabels.none { it.label == "Casa" })
            cancelAndIgnoreRemainingEvents()
        }
    }

    // endregion

    // region CreateLabel

    @Test
    fun `Given new label name When CreateLabel fires Then label is appended in state and DB`() =
        runTest {
            viewModel.uiState.test {
                advanceUntilIdle()

                viewModel.onEvent(SearchStopUiEvent.CreateLabel(name = "Gym", emoji = "🏋"))
                advanceUntilIdle()

                val state = expectMostRecentItem()
                val gym = state.stopLabels.first { it.label == "Gym" }
                assertEquals("🏋", gym.emoji)
                assertNull(gym.stopId)
                assertNull(gym.stopName)

                val rows = fakeSandook.observeStopLabels().first()
                assertTrue(rows.any { it.label == "Gym" })
            }
        }

    @Test
    fun `Given duplicate name with different case When CreateLabel fires Then label is not added`() =
        runTest {
            viewModel.uiState.test {
                advanceUntilIdle()

                // "home" should match seeded "Home" case-insensitively after
                // normaliseLabelName — duplicate must silently no-op.
                viewModel.onEvent(SearchStopUiEvent.CreateLabel(name = "home", emoji = "🏠"))
                advanceUntilIdle()

                val state = expectMostRecentItem()
                val homes = state.stopLabels.count { it.label.equals("home", ignoreCase = true) }
                assertEquals(1, homes, "expected exactly one Home label, got $homes")
            }
        }

    @Test
    fun `Given blank name When CreateLabel fires Then nothing changes`() = runTest {
        viewModel.uiState.test {
            advanceUntilIdle()
            val before = expectMostRecentItem().stopLabels.size

            viewModel.onEvent(SearchStopUiEvent.CreateLabel(name = "   ", emoji = "🌀"))
            advanceUntilIdle()

            // Blank input is treated like no input — no new emission. expectNoEvents()
            // asserts that, then we read the StateFlow value directly (which is the
            // unchanged state, since nothing wrote to it).
            expectNoEvents()
            assertEquals(before, viewModel.uiState.value.stopLabels.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Given name with surrounding emoji When CreateLabel fires Then name is normalised`() =
        runTest {
            viewModel.uiState.test {
                advanceUntilIdle()

                // The save-sheet text field allows free typing; the VM is the
                // canonicaliser. "🚗 Garage 🚗" should land as "Garage" in the DB.
                viewModel.onEvent(SearchStopUiEvent.CreateLabel(name = "🚗 Garage 🚗", emoji = "🚗"))
                advanceUntilIdle()

                val state = expectMostRecentItem()
                assertNotNull(state.stopLabels.firstOrNull { it.label == "Garage" })
            }
        }

    @Test
    fun `Given name longer than the max length When CreateLabel fires Then name is truncated`() =
        runTest {
            viewModel.uiState.test {
                advanceUntilIdle()

                // Defense in depth: the TextField already caps input at
                // LABEL_NAME_MAX_LENGTH, but the VM must enforce it independently
                // for any caller that skips the UI.
                val tooLong = "A".repeat(LABEL_NAME_MAX_LENGTH + 10)
                viewModel.onEvent(SearchStopUiEvent.CreateLabel(name = tooLong, emoji = "🚗"))
                advanceUntilIdle()

                val state = expectMostRecentItem()
                val stored = state.stopLabels.firstOrNull { it.label.startsWith("A") }
                assertNotNull(stored)
                assertTrue(stored.label.length <= LABEL_NAME_MAX_LENGTH)

                val rows = fakeSandook.observeStopLabels().first()
                assertTrue(rows.all { it.label.length <= LABEL_NAME_MAX_LENGTH })
            }
        }

    // endregion

    // region ClearLabelStop

    @Test
    fun `Given label with stop When ClearLabelStop fires Then stop is detached`() = runTest {
        viewModel.uiState.test {
            advanceUntilIdle()

            // Set Home first so there's something to clear.
            val central = StopItem(stopId = "stop_central", stopName = "Central Station")
            viewModel.onEvent(SearchStopUiEvent.AssignLabelStop("Home", central))
            advanceUntilIdle()

            viewModel.onEvent(SearchStopUiEvent.ClearLabelStop("Home"))
            advanceUntilIdle()

            val state = expectMostRecentItem()
            val home = state.stopLabels.first { it.label == "Home" }
            assertNull(home.stopId)
            assertNull(home.stopName)

            // DB was also cleared.
            val rows = fakeSandook.observeStopLabels().first()
            val dbHome = rows.first { it.label == "Home" }
            assertNull(dbHome.stop_id)
            assertNull(dbHome.stop_name)
        }
    }

    // endregion

    // region DeleteLabel

    @Test
    fun `Given non-protected label When DeleteLabel fires Then label is removed`() = runTest {
        viewModel.uiState.test {
            advanceUntilIdle()

            viewModel.onEvent(SearchStopUiEvent.DeleteLabel("Work"))
            advanceUntilIdle()

            val state = expectMostRecentItem()
            assertTrue(state.stopLabels.none { it.label == "Work" })

            // DB row was deleted too.
            val rows = fakeSandook.observeStopLabels().first()
            assertTrue(rows.none { it.label == "Work" })
        }
    }

    @Test
    fun `Given protected Home label When DeleteLabel fires Then label is preserved`() = runTest {
        viewModel.uiState.test {
            advanceUntilIdle()

            viewModel.onEvent(SearchStopUiEvent.DeleteLabel(StopLabel.PROTECTED_LABEL))
            advanceUntilIdle()

            // Handler early-returns; existing state still contains Home. Defence in
            // depth — the UI also hides ✕ on Home.
            val state = expectMostRecentItem()
            assertTrue(state.stopLabels.any { it.label == "Home" })

            // DB row also preserved.
            val rows = fakeSandook.observeStopLabels().first()
            assertTrue(rows.any { it.label == "Home" })
        }
    }

    @Test
    fun `Given home key with mixed case When DeleteLabel fires Then label is preserved`() = runTest {
        viewModel.uiState.test {
            advanceUntilIdle()

            // Defensive: the UI should never send "home" (lowercase) because the label
            // value is fixed, but the handler must still treat it as protected.
            viewModel.onEvent(SearchStopUiEvent.DeleteLabel("home"))
            advanceUntilIdle()

            assertTrue(expectMostRecentItem().stopLabels.any { it.label == "Home" })
        }
    }

    // endregion

    // region MoveLabelToIndex

    @Test
    fun `Given two labels When MoveLabelToIndex moves Home to position 1 Then Work comes first`() =
        runTest {
            viewModel.uiState.test {
                advanceUntilIdle()

                viewModel.onEvent(SearchStopUiEvent.MoveLabelToIndex("Home", "Work"))
                advanceUntilIdle()

                val state = expectMostRecentItem()
                assertEquals("Work", state.stopLabels[0].label)
                assertEquals("Home", state.stopLabels[1].label)

                // The handler also re-numbers sort_order across the whole list. Walk
                // the DB rows by label and assert the new order is reflected there
                // too — otherwise next process start would reset to the old order.
                val rows = fakeSandook.observeStopLabels().first()
                val byLabel = rows.associateBy { it.label }
                val workOrder = byLabel["Work"]?.sort_order ?: error("Work missing")
                val homeOrder = byLabel["Home"]?.sort_order ?: error("Home missing")
                assertTrue(workOrder < homeOrder, "expected Work($workOrder) before Home($homeOrder)")
            }
        }

    @Test
    fun `Given target equal to source When MoveLabelToIndex fires Then order is unchanged`() = runTest {
        viewModel.uiState.test {
            advanceUntilIdle()

            // No-op path: dragging onto the same slot should not re-emit or re-write.
            viewModel.onEvent(SearchStopUiEvent.MoveLabelToIndex("Home", "Home"))
            advanceUntilIdle()

            assertEquals("Home", expectMostRecentItem().stopLabels[0].label)
        }
    }

    @Test
    fun `Given unknown label key When MoveLabelToIndex fires Then order is unchanged`() = runTest {
        viewModel.uiState.test {
            advanceUntilIdle()

            // Defensive: a stale label key (e.g. one deleted between the drag start
            // and drag end) should be a no-op rather than crash.
            viewModel.onEvent(SearchStopUiEvent.MoveLabelToIndex("DoesNotExist", "Home"))
            advanceUntilIdle()

            val state = expectMostRecentItem()
            assertEquals("Home", state.stopLabels[0].label)
            assertEquals("Work", state.stopLabels[1].label)
        }
    }

    // endregion

    // region observeStopLabels

    @Test
    fun `Given empty DB When VM is created Then defaults are seeded into sandook`() = runTest {
        // Brand new sandook so observeStopLabels' empty-rows branch fires.
        val freshSandook = FakeSandook()
        val freshVm = SearchStopViewModel(
            analytics = FakeAnalytics(),
            stopResultsManager = FakeStopResultsManager(),
            remoteAddressResultsManager = FakeRemoteAddressResultsManager(),
            flag = FakeFlag(),
            nearbyStopsManager = FakeNearbyStopsManagerForMap(),
            ioDispatcher = testDispatcher,
            preferences = FakeSandookPreferences(),
            sandook = freshSandook,
        )
        // Subscribe so the VM scope's observer collects.
        freshVm.uiState.test {
            advanceUntilIdle()

            val rows = freshSandook.observeStopLabels().first()
            val labels = rows.map { it.label }.toSet()
            // Defaults from StopLabel.defaults: Home + Work, no other rows.
            assertEquals(StopLabel.defaults.map { it.label }.toSet(), labels)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Given labels in DB When sandook emits Then state mirrors DB rows`() = runTest {
        viewModel.uiState.test {
            advanceUntilIdle()

            // Externally update the DB (simulating another VM or a process-level
            // change) and assert the VM reflects the new shape on the next
            // observer tick.
            fakeSandook.upsertStopLabel("Gym", "🏋", null, null, 5L)
            advanceUntilIdle()

            val state = expectMostRecentItem()
            assertEquals(3, state.stopLabels.size)
            assertTrue(state.stopLabels.any { it.label == "Gym" })
        }
    }

    // endregion

    // region Analytics — StopLabelCreatedEvent / StopLabelStopAssignedEvent / StopLabelRemovedEvent
    //
    // These tests lock down the wire-up between label handlers and the analytics
    // tracker. The product question we're protecting: can BigQuery answer
    // "how are users actually using stop labels?" from these three events alone.
    // If a handler stops firing its event, the dashboard goes silent without any
    // crash signal — so the assertion is "the event arrived" plus "the payload
    // matches what the dashboard expects to group on".

    @Test
    fun `Given new label name When CreateLabel fires Then StopLabelCreatedEvent is tracked`() =
        runTest {
            viewModel.uiState.test {
                advanceUntilIdle()

                viewModel.onEvent(SearchStopUiEvent.CreateLabel(name = "Gym", emoji = "🏋"))
                advanceUntilIdle()

                val tracked = fakeAnalytics.getTrackedEvent("stop_label_created")
                assertNotNull(tracked, "expected stop_label_created to fire")
                val event = assertIs<AnalyticsEvent.StopLabelCreatedEvent>(tracked)
                assertEquals("Gym", event.labelName)
                assertEquals("🏋", event.emoji)
                // Seeded Home + Work + new Gym = 3.
                assertEquals(3, event.totalLabelsCountAfter)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `Given duplicate label name When CreateLabel fires Then no analytics event is tracked`() =
        runTest {
            viewModel.uiState.test {
                advanceUntilIdle()

                viewModel.onEvent(SearchStopUiEvent.CreateLabel(name = "home", emoji = "🏠"))
                advanceUntilIdle()

                // Duplicate path is a silent no-op — analytics must mirror that, otherwise
                // the dashboard would inflate "labels created" with phantom rows.
                assertFalse(fakeAnalytics.isEventTracked("stop_label_created"))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `Given blank label name When CreateLabel fires Then no analytics event is tracked`() =
        runTest {
            viewModel.uiState.test {
                advanceUntilIdle()

                viewModel.onEvent(SearchStopUiEvent.CreateLabel(name = "   ", emoji = "🌀"))
                advanceUntilIdle()

                assertFalse(fakeAnalytics.isEventTracked("stop_label_created"))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `Given empty label When AssignLabelStop fires Then assigned event reports first-time pin`() =
        runTest {
            viewModel.uiState.test {
                advanceUntilIdle()

                val central = StopItem(stopId = "stop_central", stopName = "Central Station")
                viewModel.onEvent(SearchStopUiEvent.AssignLabelStop("Home", central))
                advanceUntilIdle()

                val tracked = fakeAnalytics.getTrackedEvent("stop_label_stop_assigned")
                assertNotNull(tracked)
                val event = assertIs<AnalyticsEvent.StopLabelStopAssignedEvent>(tracked)
                assertEquals("Home", event.labelName)
                assertEquals("stop_central", event.stopId)
                assertEquals("Central Station", event.stopName)
                assertFalse(event.isReassignment, "first pin should not be a reassignment")
                assertTrue(event.isProtectedLabel, "Home is the protected label")
                assertEquals(
                    AnalyticsEvent.StopLabelStopAssignedEvent.SOURCE_STAR_SHEET,
                    event.source,
                    "default source is the star -> sheet flow",
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `Given choose-mode source When AssignLabelStop fires Then assigned event carries it through`() =
        runTest {
            viewModel.uiState.test {
                advanceUntilIdle()

                val central = StopItem(stopId = "stop_central", stopName = "Central Station")
                viewModel.onEvent(
                    SearchStopUiEvent.AssignLabelStop(
                        labelKey = "Home",
                        stopItem = central,
                        source = SearchStopUiEvent.AssignLabelStop.SOURCE_CHOOSE_MODE,
                    ),
                )
                advanceUntilIdle()

                val event = assertIs<AnalyticsEvent.StopLabelStopAssignedEvent>(
                    fakeAnalytics.getTrackedEvent("stop_label_stop_assigned"),
                )
                assertEquals(AnalyticsEvent.StopLabelStopAssignedEvent.SOURCE_CHOOSE_MODE, event.source)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `Given previously pinned label When AssignLabelStop fires again Then assigned event reports reassignment`() =
        runTest {
            viewModel.uiState.test {
                advanceUntilIdle()

                // Pin Central first, then overwrite with Town Hall — the second event
                // should carry isReassignment=true so the dashboard can separate
                // "filling empty slots" from "swapping pins".
                val central = StopItem(stopId = "stop_central", stopName = "Central Station")
                val townHall = StopItem(stopId = "stop_townhall", stopName = "Town Hall")
                viewModel.onEvent(SearchStopUiEvent.AssignLabelStop("Work", central))
                advanceUntilIdle()
                fakeAnalytics.clear()
                viewModel.onEvent(SearchStopUiEvent.AssignLabelStop("Work", townHall))
                advanceUntilIdle()

                val tracked = fakeAnalytics.getTrackedEvent("stop_label_stop_assigned")
                assertNotNull(tracked)
                val event = assertIs<AnalyticsEvent.StopLabelStopAssignedEvent>(tracked)
                assertEquals("Work", event.labelName)
                assertEquals("stop_townhall", event.stopId)
                assertTrue(event.isReassignment, "second pin overwrote a different stop")
                assertFalse(event.isProtectedLabel, "Work is a default but not protected")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `Given pinned label When ClearLabelStop fires Then removed event reports CLEAR with hadStop true`() =
        runTest {
            viewModel.uiState.test {
                advanceUntilIdle()

                val central = StopItem(stopId = "stop_central", stopName = "Central Station")
                viewModel.onEvent(SearchStopUiEvent.AssignLabelStop("Home", central))
                advanceUntilIdle()
                fakeAnalytics.clear()
                viewModel.onEvent(SearchStopUiEvent.ClearLabelStop("Home"))
                advanceUntilIdle()

                val tracked = fakeAnalytics.getTrackedEvent("stop_label_removed")
                assertNotNull(tracked)
                val event = assertIs<AnalyticsEvent.StopLabelRemovedEvent>(tracked)
                assertEquals("Home", event.labelName)
                assertEquals(AnalyticsEvent.StopLabelRemovedEvent.Action.CLEAR, event.action)
                assertTrue(event.hadStop, "label had a pinned stop before clear")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `Given non-protected label When DeleteLabel fires Then removed event reports DELETE`() =
        runTest {
            viewModel.uiState.test {
                advanceUntilIdle()

                viewModel.onEvent(SearchStopUiEvent.DeleteLabel("Work"))
                advanceUntilIdle()

                val tracked = fakeAnalytics.getTrackedEvent("stop_label_removed")
                assertNotNull(tracked)
                val event = assertIs<AnalyticsEvent.StopLabelRemovedEvent>(tracked)
                assertEquals("Work", event.labelName)
                assertEquals(AnalyticsEvent.StopLabelRemovedEvent.Action.DELETE, event.action)
                // Seeded Work has no pinned stop, so hadStop must be false. This keeps
                // the "users delete labels they never used" funnel honest.
                assertFalse(event.hadStop)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `Given protected Home label When DeleteLabel fires Then no removed event is tracked`() =
        runTest {
            viewModel.uiState.test {
                advanceUntilIdle()

                viewModel.onEvent(SearchStopUiEvent.DeleteLabel(StopLabel.PROTECTED_LABEL))
                advanceUntilIdle()

                // Handler early-returns for Home; analytics must mirror the no-op so
                // defensive UI clicks don't pollute the dataset.
                assertFalse(fakeAnalytics.isEventTracked("stop_label_removed"))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `Given two labels When MoveLabelToIndex moves Home to position 1 Then reordered event is tracked`() =
        runTest {
            viewModel.uiState.test {
                advanceUntilIdle()

                viewModel.onEvent(SearchStopUiEvent.MoveLabelToIndex("Home", "Work"))
                advanceUntilIdle()

                val tracked = fakeAnalytics.getTrackedEvent("stop_label_reordered")
                assertNotNull(tracked)
                val event = assertIs<AnalyticsEvent.StopLabelReorderedEvent>(tracked)
                assertEquals("Home", event.labelName)
                assertEquals(0, event.previousIndex)
                assertEquals(1, event.newIndex)
                assertEquals(2, event.totalCount)
                // Home is the protected label; flag must be true so the dashboard can
                // separate "user moved Home" from "user moved a custom label".
                assertTrue(event.isProtectedLabel)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `Given target equal to source When MoveLabelToIndex fires Then no reordered event is tracked`() =
        runTest {
            viewModel.uiState.test {
                advanceUntilIdle()

                // The handler returns early when source == target, so no analytics
                // should fire either — otherwise drop-in-place drags would inflate
                // the reorder counts.
                viewModel.onEvent(SearchStopUiEvent.MoveLabelToIndex("Home", "Home"))
                advanceUntilIdle()

                assertFalse(fakeAnalytics.isEventTracked("stop_label_reordered"))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `Given unknown label key When MoveLabelToIndex fires Then no reordered event is tracked`() =
        runTest {
            viewModel.uiState.test {
                advanceUntilIdle()

                viewModel.onEvent(SearchStopUiEvent.MoveLabelToIndex("DoesNotExist", "Home"))
                advanceUntilIdle()

                assertFalse(fakeAnalytics.isEventTracked("stop_label_reordered"))
                cancelAndIgnoreRemainingEvents()
            }
        }

    // endregion
}
