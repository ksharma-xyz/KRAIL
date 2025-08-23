package xyz.ksharma.krail.splash

sealed interface SplashUiEvent {
    data object SplashAnimationComplete : SplashUiEvent
}
