package xyz.ksharma.krail.trip.planner.ui.intro

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The badges' place on a tall 411 dp phone, 915 dp high, at 100 percent text. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h915dp-xxhdpi", manifest = Config.NONE)
class IntroThemeCentrePhoneTest : IntroThemeCentreChecks(textScale = 1f)

/** The same phone at 150 percent text. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h915dp-xxhdpi", manifest = Config.NONE)
class IntroThemeCentrePhoneLargeTextTest : IntroThemeCentreChecks(textScale = 1.5f)

/** The narrowest window, 320 dp wide and 800 dp high, at 200 percent text. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w320dp-h800dp-xxhdpi", manifest = Config.NONE)
class IntroThemeCentreNarrowTest : IntroThemeCentreChecks(textScale = 2f)

/**
 * A window tall enough, 411 by 1280 dp, to hold the grid on its middle with the headline and the bar clear, which a phone
 * is not at 100 percent text: there the headline zone ends past the middle of the window and the grid starts under it.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h1280dp-xxhdpi", manifest = Config.NONE)
class IntroThemeCentreTallTest : IntroThemeCentreChecks(textScale = 1f) {

    @Test
    fun `the window is tall enough for the grid to sit on its middle`() = assertWindowHoldsGridOnItsMiddle()
}
