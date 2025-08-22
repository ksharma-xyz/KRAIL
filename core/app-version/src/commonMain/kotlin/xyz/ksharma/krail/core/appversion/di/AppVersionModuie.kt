package xyz.ksharma.krail.core.appversion.di

import org.koin.dsl.module
import xyz.ksharma.krail.core.appinfo.AppVersionManager
import xyz.ksharma.krail.core.appinfo.RealAppVersionManager

val appVersionModule = module {
    single<AppVersionManager> { RealAppVersionManager(get(), get()) }
}
