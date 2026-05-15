package xyz.ksharma.krail.feature.debug.settings.ui.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import xyz.ksharma.krail.feature.debug.settings.ui.DebugSettingsViewModel

val debugSettingsUiModule = module {
    viewModelOf(::DebugSettingsViewModel)
}
