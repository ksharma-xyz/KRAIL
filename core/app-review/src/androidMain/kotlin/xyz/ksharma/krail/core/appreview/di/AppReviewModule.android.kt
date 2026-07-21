package xyz.ksharma.krail.core.appreview.di

import org.koin.dsl.module
import xyz.ksharma.krail.core.appreview.AndroidAppReviewRequester
import xyz.ksharma.krail.core.appreview.AppReviewRequester

actual val appReviewModule = module {
    single<AppReviewRequester> {
        AndroidAppReviewRequester(context = get(), activityHolder = get())
    }
}
