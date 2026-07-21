package xyz.ksharma.krail.core.appreview.di

import org.koin.dsl.module
import xyz.ksharma.krail.core.appreview.AppReviewManager
import xyz.ksharma.krail.core.appreview.AppReviewRequester
import xyz.ksharma.krail.core.appreview.IosAppReviewRequester
import xyz.ksharma.krail.core.appreview.RealAppReviewManager

actual val appReviewModule = module {
    single<AppReviewRequester> { IosAppReviewRequester() }

    // Single, not factory: the zero-result-search suppression is in-memory state that has to
    // be shared between the screen that sets it and the screen that reads it.
    single<AppReviewManager> {
        RealAppReviewManager(
            requester = get(),
            lifecycleStore = get(),
            preferences = get(),
            flag = get(),
            analytics = get(),
        )
    }
}
