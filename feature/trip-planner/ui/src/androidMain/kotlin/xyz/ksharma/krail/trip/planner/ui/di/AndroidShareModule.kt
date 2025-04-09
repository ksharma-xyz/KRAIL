package xyz.ksharma.krail.trip.planner.ui.di

import org.koin.dsl.module
import xyz.ksharma.krail.trip.planner.ui.settings.AndroidSharer
import xyz.ksharma.krail.trip.planner.ui.settings.Sharer

actual val shareModule = module {
    single<Sharer> { AndroidSharer(context = get()) }
}
