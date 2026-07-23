package xyz.ksharma.krail.core.appreview.di

import org.koin.dsl.module
import xyz.ksharma.krail.core.appreview.AppReviewDebugSignal
import xyz.ksharma.krail.core.appreview.AppReviewManager
import xyz.ksharma.krail.core.appreview.AppReviewRequester
import xyz.ksharma.krail.core.appreview.IosAppReviewRequester
import xyz.ksharma.krail.core.appreview.RealAppReviewDebugSignal
import xyz.ksharma.krail.core.appreview.RealAppReviewManager
import xyz.ksharma.krail.sandook.Sandook

actual val appReviewModule = module {
    single<AppReviewDebugSignal> { RealAppReviewDebugSignal() }

    single<AppReviewRequester> { IosAppReviewRequester(debugSignal = get()) }

    // Single, not factory: the armed delight moment is in-memory state that must be shared
    // between the screen that arms it and the Saved Trips screen that fires it.
    single<AppReviewManager> {
        val sandook = get<Sandook>()
        RealAppReviewManager(
            requester = get(),
            lifecycleStore = get(),
            preferences = get(),
            flag = get(),
            analytics = get(),
            savedTripCount = { sandook.selectAllTrips().size.toLong() },
        )
    }
}
