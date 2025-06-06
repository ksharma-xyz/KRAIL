package xyz.ksharma.krail.sandook.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import xyz.ksharma.krail.sandook.RealSandook
import xyz.ksharma.krail.sandook.RealSandookPreferences
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.sandook.SandookPreferences

val sandookModule = module {
    singleOf(::RealSandook) { bind<Sandook>() }
    includes(sqlDriverModule)
    singleOf(::RealSandookPreferences) { bind<SandookPreferences>() }
}

expect val sqlDriverModule: Module
