package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep

/** Tags the temporary body of [step], so a test can tell the screen was reached. */
internal fun introPendingTag(step: IntroStep): String = "intro.pending.${step.name.lowercase()}"

/**
 * TEMPORARY. The body of a screen that is not built yet: the theme screen, built in the next unit of
 * the Intro rebuild, and the origin and confirm screens, built in the one after. It draws nothing and
 * exists so every [IntroStep] has a body and the app compiles and runs. Each screen's own unit
 * replaces its use of this and deletes it when the last one has gone.
 */
@Composable
internal fun IntroPendingStepBody(step: IntroStep, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().testTag(introPendingTag(step)))
}
