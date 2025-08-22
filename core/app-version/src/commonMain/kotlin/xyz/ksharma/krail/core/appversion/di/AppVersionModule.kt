package xyz.ksharma.krail.core.appversion.di

import org.koin.dsl.module
import xyz.ksharma.krail.core.appversion.AppVersionManager
import xyz.ksharma.krail.core.appversion.RealAppVersionManager

val appVersionModule = module {
    single<AppVersionManager> { RealAppVersionManager(get(), get()) }
}
