package xyz.ksharma.krail.core.aitext.di

import org.koin.dsl.module
import xyz.ksharma.krail.core.aitext.AiTextService
import xyz.ksharma.krail.core.aitext.AndroidAiTextService

actual val aiTextModule = module {
    single<AiTextService> { AndroidAiTextService(context = get()) }
}
