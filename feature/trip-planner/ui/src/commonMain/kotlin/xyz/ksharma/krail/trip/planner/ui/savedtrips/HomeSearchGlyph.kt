package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import xyz.ksharma.krail.taj.LocalTextColor
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.toAdaptiveDecorativeIconSize
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags

/**
 * The search glyph at the start of the home capsule, always drawn, whether or not the microphone is.
 *
 * It is Material Icons Core's `Icons.Filled.Search`, which this module and taj already depend on (taj draws `Icons.Default.Add` and
 * `Icons.AutoMirrored.Filled.ArrowBack` from the same artifact), so no image file is added anywhere. A search of the whole repo
 * (drawables, taj's vectors, compose resources, Material icons on the classpath) found no search glyph of our own.
 *
 * Its size is taj's adaptive icon size for a decorative icon ([toAdaptiveDecorativeIconSize]): the default icon size times the font
 * scale, and times 0.8 of it past 150 percent, so it grows with the text without outgrowing the capsule. It is decoration, so
 * a screen reader skips it: the capsule itself says "Where are you going?". It is tinted [tint], the capsule's text colour inside
 * taj's `TextField`.
 */
@Composable
internal fun HomeSearchGlyph(
    modifier: Modifier = Modifier,
    tint: Color = LocalTextColor.current,
) {
    Image(
        imageVector = Icons.Filled.Search,
        contentDescription = null,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier
            .testTag(TripPlannerTestTags.HOME_SEARCH_GLYPH)
            .size(KrailTheme.dimensions.iconDefault.toAdaptiveDecorativeIconSize()),
    )
}
