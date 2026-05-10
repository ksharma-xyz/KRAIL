package xyz.ksharma.krail.feature.debug.settings.ui

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
import xyz.ksharma.krail.feature.debug.settings.state.FlagOverride
import xyz.ksharma.krail.feature.debug.settings.state.NetworkTarget
import xyz.ksharma.krail.feature.debug.settings.store.DebugNetworkConfigStore
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for [DebugSettingsViewModel]. Verifies that dispatching events
 * mutates the underlying store and that the live RC value flows into
 * `liveBffEnabled` on subscription.
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
    fun `Given default state When selectNetworkTarget BFF_PROD Then store receives event`() = runTest {
        val store = FakeDebugStore()
        val viewModel = DebugSettingsViewModel(store = store, flag = FakeFlag(false))

        viewModel.selectNetworkTarget(NetworkTarget.BFF_PROD)

        assertEquals(
            DebugSettingsEvent.SetNetworkTarget(NetworkTarget.BFF_PROD),
            store.lastEvent,
        )
    }

    @Test
    fun `Given default state When selectFlagOverride FORCE_ON Then store receives event`() = runTest {
        val store = FakeDebugStore()
        val viewModel = DebugSettingsViewModel(store = store, flag = FakeFlag(false))

        viewModel.selectFlagOverride(FlagOverride.FORCE_ON)

        assertEquals(
            DebugSettingsEvent.SetFlagOverride(FlagOverride.FORCE_ON),
            store.lastEvent,
        )
    }

    @Test
    fun `Given live RC value When state collected Then liveBffEnabled emits the value`() = runTest {
        val store = FakeDebugStore()
        val viewModel = DebugSettingsViewModel(store = store, flag = FakeFlag(true))

        // Subscribe to state to trigger onStart, which refreshes liveBffEnabled.
        viewModel.state.test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(true, viewModel.liveBffEnabled.value)
    }

    @Test
    fun `Given default state When reset called Then store receives Reset event`() = runTest {
        val store = FakeDebugStore()
        val viewModel = DebugSettingsViewModel(store = store, flag = FakeFlag(false))

        viewModel.reset()

        assertEquals(DebugSettingsEvent.Reset, store.lastEvent)
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
    override val state: Flow<DebugSettingsState> = _state.asStateFlow()
    var lastEvent: DebugSettingsEvent? = null
        private set

    override suspend fun flagOverride(): FlagOverride = _state.value.flagOverride
    override suspend fun networkTarget(): NetworkTarget = _state.value.networkTarget

    override suspend fun set(event: DebugSettingsEvent) {
        lastEvent = event
        _state.value = when (event) {
            is DebugSettingsEvent.SetNetworkTarget -> _state.value.copy(networkTarget = event.target)
            is DebugSettingsEvent.SetFlagOverride -> _state.value.copy(flagOverride = event.override)
            DebugSettingsEvent.Reset -> DebugSettingsState.default()
        }
    }
}
