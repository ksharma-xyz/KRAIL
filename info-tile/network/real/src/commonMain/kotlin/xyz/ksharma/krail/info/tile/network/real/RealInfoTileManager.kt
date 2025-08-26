package xyz.ksharma.krail.info.tile.network.real

import xyz.ksharma.krail.core.appinfo.AppInfoProvider
import xyz.ksharma.krail.core.appversion.AppVersionManager
import xyz.ksharma.krail.info.tile.network.api.InfoTileManager
import xyz.ksharma.krail.platform.ops.PlatformOps

class RealInfoTileManager(
    private val appVersionManager: AppVersionManager,
    private val appInfoProvider: AppInfoProvider,
    private val platformOps: PlatformOps,
) : InfoTileManager {


}
