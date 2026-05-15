package xyz.ksharma.krail.feature.pro.ui.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import xyz.ksharma.krail.feature.pro.ui.ProViewModel

val proUiModule = module {
    viewModelOf(::ProViewModel)
}
