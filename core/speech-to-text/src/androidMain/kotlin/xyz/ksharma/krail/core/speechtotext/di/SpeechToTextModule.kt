package xyz.ksharma.krail.core.speechtotext.di

import org.koin.dsl.module
import xyz.ksharma.krail.core.speechtotext.AndroidSpeechToTextService
import xyz.ksharma.krail.core.speechtotext.SpeechToTextService

actual val speechToTextModule = module {
    single<SpeechToTextService> { AndroidSpeechToTextService(context = get()) }
}
