package xyz.ksharma.krail.feature.debug.settings.store

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import xyz.ksharma.krail.feature.debug.settings.state.DebugSettingsEvent
import xyz.ksharma.krail.feature.debug.settings.state.DebugSettingsState
import xyz.ksharma.krail.feature.debug.settings.state.FlagOverride
import xyz.ksharma.krail.feature.debug.settings.state.NetworkTarget
import xyz.ksharma.krail.sandook.SandookPreferences
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for [RealDebugNetworkConfigStore]. Covers hydration from defaults,
 * write-and-read round-tripping for both the network target and flag
 * override, and the reset path. Uses an in-memory [SandookPreferences]
 * fake to avoid any DB infrastructure in commonTest.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RealDebugNetworkConfigStoreTest {

    @Test
    fun `Given fresh install When constructed Then state hydrates to defaults`() = runTest {
        val store = newStore()

        store.state.test {
            assertEquals(DebugSettingsState.default(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Given default state When SetNetworkTarget BFF_PROD Then state emits new target`() = runTest {
        val store = newStore()

        store.set(DebugSettingsEvent.SetNetworkTarget(NetworkTarget.BFF_PROD))

        assertEquals(NetworkTarget.BFF_PROD, store.networkTarget())
    }

    @Test
    fun `Given default state When SetFlagOverride FORCE_ON Then state emits new override`() = runTest {
        val store = newStore()

        store.set(DebugSettingsEvent.SetFlagOverride(FlagOverride.FORCE_ON))

        assertEquals(FlagOverride.FORCE_ON, store.flagOverride())
    }

    @Test
    fun `Given mutated state When Reset Then both fields revert to defaults`() = runTest {
        val store = newStore()
        store.set(DebugSettingsEvent.SetNetworkTarget(NetworkTarget.BFF_PROD))
        store.set(DebugSettingsEvent.SetFlagOverride(FlagOverride.FORCE_ON))

        store.set(DebugSettingsEvent.Reset)

        assertEquals(DebugSettingsState.default().networkTarget, store.networkTarget())
        assertEquals(DebugSettingsState.default().flagOverride, store.flagOverride())
    }

    @Test
    fun `Given persisted prefs When new store constructed Then hydrates from prefs`() = runTest {
        val prefs = InMemorySandookPreferences()
        prefs.setString(RealDebugNetworkConfigStore.KEY_DEBUG_NETWORK_TARGET, NetworkTarget.BFF_PROD.name)
        prefs.setString(RealDebugNetworkConfigStore.KEY_DEBUG_FLAG_OVERRIDE, FlagOverride.FORCE_ON.name)

        val store = RealDebugNetworkConfigStore(prefs, UnconfinedTestDispatcher())

        assertEquals(NetworkTarget.BFF_PROD, store.networkTarget())
        assertEquals(FlagOverride.FORCE_ON, store.flagOverride())
    }

    @Test
    fun `Given corrupt enum string When hydrating Then falls back to default for that field`() = runTest {
        val prefs = InMemorySandookPreferences()
        prefs.setString(RealDebugNetworkConfigStore.KEY_DEBUG_NETWORK_TARGET, "NOT_A_REAL_VALUE")

        val store = RealDebugNetworkConfigStore(prefs, UnconfinedTestDispatcher())

        assertEquals(DebugSettingsState.DEFAULT_NETWORK_TARGET, store.networkTarget())
    }

    private fun newStore(): RealDebugNetworkConfigStore = RealDebugNetworkConfigStore(
        preferences = InMemorySandookPreferences(),
        ioDispatcher = UnconfinedTestDispatcher(),
    )
}

private class InMemorySandookPreferences : SandookPreferences {
    private val longs = mutableMapOf<String, Long>()
    private val strings = mutableMapOf<String, String>()
    private val bools = mutableMapOf<String, Boolean>()
    private val doubles = mutableMapOf<String, Double>()

    override fun getLong(key: String): Long? = longs[key]
    override fun setLong(key: String, value: Long) {
        longs[key] = value
    }

    override fun getString(key: String): String? = strings[key]
    override fun setString(key: String, value: String) {
        strings[key] = value
    }

    override fun getBoolean(key: String): Boolean? = bools[key]
    override fun setBoolean(key: String, value: Boolean) {
        bools[key] = value
    }

    override fun getDouble(key: String): Double? = doubles[key]
    override fun setDouble(key: String, value: Double) {
        doubles[key] = value
    }

    override fun deletePreference(key: String) {
        longs.remove(key)
        strings.remove(key)
        bools.remove(key)
        doubles.remove(key)
    }
}
