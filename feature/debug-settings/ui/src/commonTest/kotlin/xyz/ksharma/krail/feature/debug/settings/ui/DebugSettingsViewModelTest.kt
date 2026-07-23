package xyz.ksharma.krail.feature.debug.settings.ui

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import xyz.ksharma.krail.core.remoteconfig.flag.Flag
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.FlagValue
import xyz.ksharma.krail.feature.debug.settings.state.DebugSettingsEvent
import xyz.ksharma.krail.feature.debug.settings.state.DebugSettingsState
import xyz.ksharma.krail.feature.debug.settings.state.NetworkSource
import xyz.ksharma.krail.feature.debug.settings.store.DebugNetworkConfigStore
import xyz.ksharma.krail.sandook.LifecycleCounter
import xyz.ksharma.krail.sandook.UserLifecycleStore
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for [DebugSettingsViewModel]. Verifies that dispatching events
 * mutates the underlying store and that the live RC value flows into
 * `bffEnabled` on subscription.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DebugSettingsViewModelTest {

    private val mainDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Given default state When selectSource BFF_PROD Then store receives event`() = runTest {
        val store = FakeDebugStore()
        val viewModel = DebugSettingsViewModel(
            store = store,
            flag = FakeFlag(false),
            userLifecycleStore = FakeUserLifecycleStore(),
        )

        viewModel.selectSource(NetworkSource.BFF_PROD)

        assertEquals(
            DebugSettingsEvent.SetSource(NetworkSource.BFF_PROD),
            store.lastEvent,
        )
    }

    @Test
    fun `Given default state When selectSource NSW_DIRECT Then store receives event`() = runTest {
        val store = FakeDebugStore()
        val viewModel = DebugSettingsViewModel(
            store = store,
            flag = FakeFlag(false),
            userLifecycleStore = FakeUserLifecycleStore(),
        )

        viewModel.selectSource(NetworkSource.NSW_DIRECT)

        assertEquals(
            DebugSettingsEvent.SetSource(NetworkSource.NSW_DIRECT),
            store.lastEvent,
        )
    }

    @Test
    fun `Given live RC value true When state collected Then bffEnabled emits true`() = runTest {
        val store = FakeDebugStore()
        val viewModel = DebugSettingsViewModel(
            store = store,
            flag = FakeFlag(true),
            userLifecycleStore = FakeUserLifecycleStore(),
        )

        // Subscribe to state to trigger onStart, which refreshes bffEnabled.
        viewModel.state.test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(true, viewModel.bffEnabled.value)
    }

    @Test
    fun `Given live RC value false When state collected Then bffEnabled emits false`() = runTest {
        val store = FakeDebugStore()
        val viewModel = DebugSettingsViewModel(
            store = store,
            flag = FakeFlag(false),
            userLifecycleStore = FakeUserLifecycleStore(),
        )

        viewModel.state.test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(false, viewModel.bffEnabled.value)
    }

    @Test
    fun `Given default state When reset called Then store receives Reset event`() = runTest {
        val store = FakeDebugStore()
        val viewModel = DebugSettingsViewModel(
            store = store,
            flag = FakeFlag(false),
            userLifecycleStore = FakeUserLifecycleStore(),
        )

        viewModel.reset()

        assertEquals(DebugSettingsEvent.Reset, store.lastEvent)
    }

    @Test
    fun `resetInAppReviewAsks clears the review counter but keeps the install date`() = runTest {
        val lifecycleStore = FakeUserLifecycleStore()
        lifecycleStore.recordFirstInstallIfAbsent()
        lifecycleStore.increment(LifecycleCounter.REVIEW_PROMPT_REQUESTED)
        val viewModel = DebugSettingsViewModel(
            store = FakeDebugStore(),
            flag = FakeFlag(false),
            userLifecycleStore = lifecycleStore,
        )

        viewModel.resetInAppReviewAsks()

        assertEquals(0L, lifecycleStore.count(LifecycleCounter.REVIEW_PROMPT_REQUESTED))
        assertEquals(true, lifecycleStore.firstInstallAtMillis() != null)
    }
}

private class FakeUserLifecycleStore : UserLifecycleStore {
    private var installAtMillis: Long? = null
    private val counts = mutableMapOf<LifecycleCounter, Long>()

    override fun recordFirstInstallIfAbsent() {
        if (installAtMillis == null) installAtMillis = 0L
    }

    override fun firstInstallAtMillis(): Long? = installAtMillis
    override fun daysSinceFirstInstall(): Long? = installAtMillis?.let { 0L }

    override fun increment(counter: LifecycleCounter): Long {
        val next = counts.getOrElse(counter) { 0L } + 1
        counts[counter] = next
        return next
    }

    override fun count(counter: LifecycleCounter): Long = counts.getOrElse(counter) { 0L }
    override fun lastAtMillis(counter: LifecycleCounter): Long? = null
    override fun millisSinceLast(counter: LifecycleCounter): Long? = null

    override fun reset(counter: LifecycleCounter) {
        counts.remove(counter)
    }
}

private class FakeFlag(private val bffEnabled: Boolean) : Flag {
    override fun getFlagValue(key: String): FlagValue = when (key) {
        FlagKeys.ENABLE_PROTO_BFF.key -> FlagValue.BooleanValue(bffEnabled)
        else -> FlagValue.BooleanValue(false)
    }
}

private class FakeDebugStore : DebugNetworkConfigStore {
    private val _state = MutableStateFlow(DebugSettingsState.default())
    override val state: StateFlow<DebugSettingsState> = _state.asStateFlow()
    var lastEvent: DebugSettingsEvent? = null
        private set

    override suspend fun source(): NetworkSource = _state.value.source

    override suspend fun set(event: DebugSettingsEvent) {
        lastEvent = event
        _state.value = when (event) {
            is DebugSettingsEvent.SetSource -> _state.value.copy(source = event.source)
            is DebugSettingsEvent.SetTripTrackingEnabled -> _state.value.copy(tripTrackingEnabled = event.enabled)
            is DebugSettingsEvent.SetAddressSearchEnabled -> _state.value.copy(addressSearchEnabled = event.enabled)
            DebugSettingsEvent.Reset -> DebugSettingsState.default()
        }
    }
}
