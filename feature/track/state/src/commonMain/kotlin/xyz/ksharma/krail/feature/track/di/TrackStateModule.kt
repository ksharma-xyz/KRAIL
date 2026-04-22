package xyz.ksharma.krail.feature.track.di

import org.koin.dsl.module
import xyz.ksharma.krail.feature.track.TrackingManager

val trackStateModule = module {
    single<TrackingManager> { TrackingManager() }
}
