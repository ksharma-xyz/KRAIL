package xyz.ksharma.krail.core.speechtotext.di

import org.koin.dsl.module
import xyz.ksharma.krail.core.speechtotext.IosSpeechToTextService
import xyz.ksharma.krail.core.speechtotext.PreferredSpeechToTextService
import xyz.ksharma.krail.core.speechtotext.SpeechAnalyzerSpeechToTextService
import xyz.ksharma.krail.core.speechtotext.SpeechToTextService

actual val speechToTextModule = module {
    // iOS 26's analyzer where the device has it, the SFSpeechRecognizer path everywhere else.
    // Both are built either way: neither opens a microphone or touches a model until asked, and
    // deciding between them needs an availability answer only the implementations can give.
    single<SpeechToTextService> {
        PreferredSpeechToTextService(
            preferred = SpeechAnalyzerSpeechToTextService(),
            fallback = IosSpeechToTextService(),
        )
    }
}
