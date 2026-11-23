package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.runtime.Composable
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.taj.preview.PreviewScreen
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep

// The theme screen in every state a rider sees: nothing chosen, and each theme chosen. A preview and a snapshot
// run with the motion off, so every one of them is the settled frame, which is also what reduced motion shows: the
// chosen badge lifted with a thicker ring and the others at their place. Each is drawn in the colour of the theme
// chosen, as the app does, and `PreviewScreen` records it in light and dark and at 100 and 200 percent text. The
// 320 dp ones are in `narrow`.

internal val PreviewThemeNoneState = IntroState(step = IntroStep.THEME)

internal fun previewThemeChosenState(style: KrailThemeStyle) =
    IntroState(step = IntroStep.THEME, selectedThemeId = style.id)

@Composable
internal fun PreviewIntroThemeStep(state: IntroState) {
    PreviewTheme(themeStyle = themeChoiceFor(state.selectedThemeId)?.style ?: KrailThemeStyle.Train) {
        IntroFlowScreen(
            state = state,
            suggestedStops = DEFAULT_SUGGESTED_STOPS,
            onEvent = { _, _ -> },
        )
    }
}

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewIntroThemeNoneChosen() = PreviewIntroThemeStep(PreviewThemeNoneState)

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewIntroThemeTrainChosen() = PreviewIntroThemeStep(previewThemeChosenState(KrailThemeStyle.Train))

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewIntroThemeMetroChosen() = PreviewIntroThemeStep(previewThemeChosenState(KrailThemeStyle.Metro))

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewIntroThemeBusChosen() = PreviewIntroThemeStep(previewThemeChosenState(KrailThemeStyle.Bus))

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewIntroThemeFerryChosen() = PreviewIntroThemeStep(previewThemeChosenState(KrailThemeStyle.Ferry))
