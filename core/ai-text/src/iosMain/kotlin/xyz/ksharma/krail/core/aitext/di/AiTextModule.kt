package xyz.ksharma.krail.core.aitext.di

import org.koin.dsl.module
import xyz.ksharma.krail.core.aitext.AiTextService
import xyz.ksharma.krail.core.aitext.IosAiTextService

actual val aiTextModule = module {
    single<AiTextService> { IosAiTextService() }
}
