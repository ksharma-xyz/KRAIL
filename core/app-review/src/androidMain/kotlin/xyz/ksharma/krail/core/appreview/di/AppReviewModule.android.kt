package xyz.ksharma.krail.core.appreview.di

import org.koin.dsl.module
import xyz.ksharma.krail.core.appreview.AndroidAppReviewRequester
import xyz.ksharma.krail.core.appreview.AppReviewManager
import xyz.ksharma.krail.core.appreview.AppReviewRequester
import xyz.ksharma.krail.core.appreview.RealAppReviewManager
import xyz.ksharma.krail.sandook.Sandook

actual val appReviewModule = module {
    single<AppReviewRequester> {
        AndroidAppReviewRequester(context = get(), activityHolder = get())
    }

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
