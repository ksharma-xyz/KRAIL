package xyz.ksharma.krail.core.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import xyz.ksharma.krail.core.appinfo.AppInfo
import xyz.ksharma.krail.core.appinfo.AppInfoProvider
import xyz.ksharma.krail.core.appinfo.DevicePlatformType
import xyz.ksharma.krail.core.remoteconfig.flag.Flag
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.FlagValue
import xyz.ksharma.krail.feature.debug.settings.state.DebugSettingsEvent
import xyz.ksharma.krail.feature.debug.settings.state.DebugSettingsState
import xyz.ksharma.krail.feature.debug.settings.state.NetworkSource
import xyz.ksharma.krail.feature.debug.settings.store.DebugNetworkConfigStore
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Decision-table tests for [BffEndpointResolver]. Pins the cross-product
 * of (isDebug, source, RC value, URL availability) to the expected
 * resolved base URL so a regression in any one branch shows up in CI.
 *
 * Rows (in test order):
 *
 * |  # | isDebug | source     | rcValue | localURL | prodURL | expected   |
 * |----|---------|------------|---------|----------|---------|------------|
 * |  1 | release | (FOLLOW)   | false   | set      | set     | NSW        |
 * |  2 | release | (FOLLOW)   | true    | set      | set     | prod BFF   |
 * |  3 | release | (FOLLOW)   | true    | set      | blank   | NSW        |
 * |  4 | debug   | FOLLOW_RC  | false   | set      | set     | NSW        |
 * |  5 | debug   | FOLLOW_RC  | true    | set      | set     | local BFF  |
 * |  6 | debug   | FOLLOW_RC  | true    | blank    | set     | NSW        |
 * |  7 | debug   | NSW_DIRECT | true    | set      | set     | NSW        |
 * |  8 | debug   | BFF_LOCAL  | false   | set      | set     | local BFF  |
 * |  9 | debug   | BFF_LOCAL  | false   | blank    | set     | NSW        |
 * | 10 | debug   | BFF_PROD   | false   | set      | set     | prod BFF   |
 * | 11 | debug   | BFF_PROD   | false   | set      | blank   | NSW        |
 */
class BffEndpointResolverTest {

    private val nswUrl = NSW_TRANSPORT_BASE_URL
    private val localBff = "http://10.0.2.2:8080"
    private val prodBff = "https://bff.krail.app"

    @Test
    fun `01 release with RC false routes to NSW`() = runTest {
        val resolver = resolver(
            isDebug = false,
            rcValue = false,
            source = NetworkSource.FOLLOW_RC,
        )
        assertEquals(nswUrl, resolver.resolveBaseUrl())
    }

    @Test
    fun `02 release with RC true routes to prod BFF`() = runTest {
        val resolver = resolver(
            isDebug = false,
            rcValue = true,
            // source is ignored in release builds
            source = NetworkSource.NSW_DIRECT,
        )
        assertEquals(prodBff, resolver.resolveBaseUrl())
    }

    @Test
    fun `03 release with RC true falls back to NSW when prod URL blank`() = runTest {
        val resolver = resolver(
            isDebug = false,
            rcValue = true,
            source = NetworkSource.FOLLOW_RC,
            prodBffUrl = "",
        )
        assertEquals(nswUrl, resolver.resolveBaseUrl())
    }

    @Test
    fun `04 debug FOLLOW_RC with RC false routes to NSW`() = runTest {
        val resolver = resolver(
            isDebug = true,
            rcValue = false,
            source = NetworkSource.FOLLOW_RC,
        )
        assertEquals(nswUrl, resolver.resolveBaseUrl())
    }

    @Test
    fun `05 debug FOLLOW_RC with RC true routes to local BFF`() = runTest {
        val resolver = resolver(
            isDebug = true,
            rcValue = true,
            source = NetworkSource.FOLLOW_RC,
        )
        assertEquals(localBff, resolver.resolveBaseUrl())
    }

    @Test
    fun `06 debug FOLLOW_RC with RC true falls back to NSW when local URL blank`() = runTest {
        val resolver = resolver(
            isDebug = true,
            rcValue = true,
            source = NetworkSource.FOLLOW_RC,
            localBffUrl = "",
        )
        assertEquals(nswUrl, resolver.resolveBaseUrl())
    }

    @Test
    fun `07 debug NSW_DIRECT ignores RC and routes to NSW`() = runTest {
        val resolver = resolver(
            isDebug = true,
            rcValue = true,
            source = NetworkSource.NSW_DIRECT,
        )
        assertEquals(nswUrl, resolver.resolveBaseUrl())
    }

    @Test
    fun `08 debug BFF_LOCAL ignores RC and routes to local BFF`() = runTest {
        val resolver = resolver(
            isDebug = true,
            rcValue = false,
            source = NetworkSource.BFF_LOCAL,
        )
        assertEquals(localBff, resolver.resolveBaseUrl())
    }

    @Test
    fun `09 debug BFF_LOCAL falls back to NSW when local URL blank`() = runTest {
        val resolver = resolver(
            isDebug = true,
            rcValue = false,
            source = NetworkSource.BFF_LOCAL,
            localBffUrl = "",
        )
        assertEquals(nswUrl, resolver.resolveBaseUrl())
    }

    @Test
    fun `10 debug BFF_PROD ignores RC and routes to prod BFF`() = runTest {
        val resolver = resolver(
            isDebug = true,
            rcValue = false,
            source = NetworkSource.BFF_PROD,
        )
        assertEquals(prodBff, resolver.resolveBaseUrl())
    }

    @Test
    fun `11 debug BFF_PROD falls back to NSW when prod URL blank`() = runTest {
        val resolver = resolver(
            isDebug = true,
            rcValue = false,
            source = NetworkSource.BFF_PROD,
            prodBffUrl = "",
        )
        assertEquals(nswUrl, resolver.resolveBaseUrl())
    }

    @Test
    fun `bffEnabled mirrors the live RC value`() = runTest {
        assertEquals(
            true,
            resolver(isDebug = false, rcValue = true, source = NetworkSource.FOLLOW_RC).bffEnabled(),
        )
        assertEquals(
            false,
            resolver(isDebug = false, rcValue = false, source = NetworkSource.FOLLOW_RC).bffEnabled(),
        )
    }

    private fun resolver(
        isDebug: Boolean,
        rcValue: Boolean,
        source: NetworkSource,
        localBffUrl: String = localBff,
        prodBffUrl: String = prodBff,
    ): BffEndpointResolver = BffEndpointResolver(
        appInfoProvider = FakeAppInfoProvider(isDebug),
        flag = FakeFlag(rcValue),
        debugStore = FakeDebugStore(source),
        bffLocalBaseUrl = localBffUrl,
        bffProdBaseUrl = prodBffUrl,
    )
}

private class FakeAppInfoProvider(private val isDebug: Boolean) : AppInfoProvider {
    override fun getAppInfo(): AppInfo = object : AppInfo {
        override val devicePlatformType: DevicePlatformType = DevicePlatformType.ANDROID
        override val isDebug: Boolean = this@FakeAppInfoProvider.isDebug
        override val appVersion: String = "1.0.0"
        override val appBuildNumber: String = "1"
        override val osVersion: String = "30"
        override val fontSize: String = "1.0"
        override val isDarkTheme: Boolean = false
        override val deviceModel: String = "test"
        override val deviceManufacturer: String = "test"
        override val locale: String = "en_AU"
        override val timeZone: String = "Australia/Sydney"
        override val appStoreUrl: String = "https://example.com"
    }
}

private class FakeFlag(private val bffEnabled: Boolean) : Flag {
    override fun getFlagValue(key: String): FlagValue = when (key) {
        FlagKeys.ENABLE_PROTO_BFF.key -> FlagValue.BooleanValue(bffEnabled)
        else -> FlagValue.BooleanValue(false)
    }
}

private class FakeDebugStore(
    private val source: NetworkSource,
) : DebugNetworkConfigStore {
    override val state: Flow<DebugSettingsState> = MutableStateFlow(
        DebugSettingsState(source = source),
    )

    override suspend fun source(): NetworkSource = source
    override suspend fun set(event: DebugSettingsEvent) = Unit
}
