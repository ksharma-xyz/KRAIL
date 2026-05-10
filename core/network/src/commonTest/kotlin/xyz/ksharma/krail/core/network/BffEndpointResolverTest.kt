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
import xyz.ksharma.krail.feature.debug.settings.state.FlagOverride
import xyz.ksharma.krail.feature.debug.settings.state.NetworkTarget
import xyz.ksharma.krail.feature.debug.settings.store.DebugNetworkConfigStore
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Decision-table tests for [BffEndpointResolver]. Pins the cross-product
 * of (isDebug, flagOverride, RC value, networkTarget) to the expected
 * resolved base URL so a regression in any one branch shows up in CI.
 *
 * Rows (in test order):
 *
 * | # | isDebug | override   | rcValue | target    | expected URL                       |
 * |---|---------|------------|---------|-----------|------------------------------------|
 * | 1 | release | n/a        | false   | n/a       | NSW                                |
 * | 2 | release | n/a        | true    | BFF_LOCAL | local BFF                          |
 * | 3 | debug   | FOLLOW_RC  | false   | n/a       | NSW                                |
 * | 4 | debug   | FOLLOW_RC  | true    | BFF_LOCAL | local BFF                          |
 * | 5 | debug   | FORCE_ON   | false   | BFF_LOCAL | local BFF (RC ignored)             |
 * | 6 | debug   | FORCE_OFF  | true    | BFF_PROD  | NSW (RC ignored)                   |
 * | 7 | debug   | FORCE_ON   | false   | BFF_PROD  | prod BFF                           |
 * | 8 | debug   | FORCE_ON   | true    | BFF_PROD  | NSW (prod URL blank, fall back)    |
 */
class BffEndpointResolverTest {

    private val nswUrl = NSW_TRANSPORT_BASE_URL
    private val localBff = "http://10.0.2.2:8080"
    private val prodBff = "https://bff.krail.app"

    @Test
    fun `1 release with RC false routes to NSW`() = runTest {
        val resolver = resolver(
            isDebug = false,
            rcValue = false,
            override = FlagOverride.FOLLOW_RC,
            target = NetworkTarget.BFF_LOCAL,
        )
        assertEquals(nswUrl, resolver.resolveBaseUrl())
    }

    @Test
    fun `2 release with RC true routes to local BFF when target is local`() = runTest {
        val resolver = resolver(
            isDebug = false,
            rcValue = true,
            override = FlagOverride.FORCE_OFF, // ignored in release
            target = NetworkTarget.BFF_LOCAL,
        )
        assertEquals(localBff, resolver.resolveBaseUrl())
    }

    @Test
    fun `3 debug FOLLOW_RC with RC false routes to NSW`() = runTest {
        val resolver = resolver(
            isDebug = true,
            rcValue = false,
            override = FlagOverride.FOLLOW_RC,
            target = NetworkTarget.BFF_LOCAL,
        )
        assertEquals(nswUrl, resolver.resolveBaseUrl())
    }

    @Test
    fun `4 debug FOLLOW_RC with RC true routes to local BFF`() = runTest {
        val resolver = resolver(
            isDebug = true,
            rcValue = true,
            override = FlagOverride.FOLLOW_RC,
            target = NetworkTarget.BFF_LOCAL,
        )
        assertEquals(localBff, resolver.resolveBaseUrl())
    }

    @Test
    fun `5 debug FORCE_ON ignores RC false and routes to local BFF`() = runTest {
        val resolver = resolver(
            isDebug = true,
            rcValue = false,
            override = FlagOverride.FORCE_ON,
            target = NetworkTarget.BFF_LOCAL,
        )
        assertEquals(localBff, resolver.resolveBaseUrl())
    }

    @Test
    fun `6 debug FORCE_OFF ignores RC true and routes to NSW`() = runTest {
        val resolver = resolver(
            isDebug = true,
            rcValue = true,
            override = FlagOverride.FORCE_OFF,
            target = NetworkTarget.BFF_PROD,
        )
        assertEquals(nswUrl, resolver.resolveBaseUrl())
    }

    @Test
    fun `7 debug FORCE_ON with prod target routes to prod BFF when set`() = runTest {
        val resolver = resolver(
            isDebug = true,
            rcValue = false,
            override = FlagOverride.FORCE_ON,
            target = NetworkTarget.BFF_PROD,
            prodBffUrl = prodBff,
        )
        assertEquals(prodBff, resolver.resolveBaseUrl())
    }

    @Test
    fun `8 debug FORCE_ON with prod target falls back to NSW when prod URL blank`() = runTest {
        val resolver = resolver(
            isDebug = true,
            rcValue = true,
            override = FlagOverride.FORCE_ON,
            target = NetworkTarget.BFF_PROD,
            prodBffUrl = "",
        )
        assertEquals(nswUrl, resolver.resolveBaseUrl())
    }

    @Test
    fun `useBff returns true when RC says so in release builds`() = runTest {
        val resolver = resolver(
            isDebug = false,
            rcValue = true,
            override = FlagOverride.FORCE_OFF,
            target = NetworkTarget.BFF_LOCAL,
        )
        assertEquals(true, resolver.useBff())
    }

    private fun resolver(
        isDebug: Boolean,
        rcValue: Boolean,
        override: FlagOverride,
        target: NetworkTarget,
        prodBffUrl: String = prodBff,
    ): BffEndpointResolver = BffEndpointResolver(
        appInfoProvider = FakeAppInfoProvider(isDebug),
        flag = FakeFlag(rcValue),
        debugStore = FakeDebugStore(override = override, target = target),
        bffLocalBaseUrl = localBff,
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
    private val override: FlagOverride,
    private val target: NetworkTarget,
) : DebugNetworkConfigStore {
    override val state: Flow<DebugSettingsState> = MutableStateFlow(
        DebugSettingsState(networkTarget = target, flagOverride = override),
    )

    override suspend fun flagOverride(): FlagOverride = override
    override suspend fun networkTarget(): NetworkTarget = target
    override suspend fun set(event: DebugSettingsEvent) = Unit
}
