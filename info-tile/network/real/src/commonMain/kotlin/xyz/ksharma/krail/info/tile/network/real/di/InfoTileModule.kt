package xyz.ksharma.krail.info.tile.network.real.di

import org.koin.dsl.module
import xyz.ksharma.krail.info.tile.network.api.InfoTileManager
import xyz.ksharma.krail.info.tile.network.real.RealInfoTileManager

val infoTileModule = module {
    single<InfoTileManager> {
        RealInfoTileManager(
            appVersionManager = get(),
            appInfoProvider = get(),
            preferences = get(),
            flag = get(),
        )
    }
}
