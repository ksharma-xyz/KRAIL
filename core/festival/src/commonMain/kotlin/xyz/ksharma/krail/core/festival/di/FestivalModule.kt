package xyz.ksharma.krail.core.festival.di

import org.koin.dsl.module
import xyz.ksharma.krail.core.festival.FestivalManager
import xyz.ksharma.krail.core.festival.RealFestivalManager

val festivalModule = module {
    single<FestivalManager> { RealFestivalManager() }
}
