package xyz.ksharma.krail.core.textrecognition.di

import org.koin.dsl.module
import xyz.ksharma.krail.core.textrecognition.AndroidTextRecognitionService
import xyz.ksharma.krail.core.textrecognition.TextRecognitionService

actual val textRecognitionModule = module {
    single<TextRecognitionService> { AndroidTextRecognitionService() }
}
