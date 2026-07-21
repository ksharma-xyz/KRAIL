package xyz.ksharma.krail.core.appreview.di

import org.koin.dsl.module
import xyz.ksharma.krail.core.appreview.AppReviewRequester
import xyz.ksharma.krail.core.appreview.IosAppReviewRequester

actual val appReviewModule = module {
    single<AppReviewRequester> { IosAppReviewRequester() }
}
