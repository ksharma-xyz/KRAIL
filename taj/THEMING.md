# Theming

How colour, light and dark work in KRAIL. Read this before adding a colour, deciding light or
dark in code, or drawing over a surface that ignores the theme.

## Where colours come from

Only from `KrailTheme.colors`, `KrailTheme.typography` and `KrailTheme.dimensions`, the
CompositionLocals set up in `theme/Theme.kt`. A feature never writes `Color(0x…)`. A colour a
screen needs and the palette does not have goes into the palette (`theme/Color.kt`), for both
themes, and `KrailColors` changes also go through `animations/ThemeTransitionAnimations.kt`.

## Light or dark

Two inputs, resolved in one place:

- `ThemeMode` (`LIGHT`, `DARK`, `SYSTEM`), the rider's choice, persisted.
- The phone's setting: `LocalSystemDarkThemeOverride ?: isSystemInDarkTheme()`. The override
  exists because Compose Multiplatform on iOS can report a stale value after the app returns
  from the background; `MainViewController` supplies a reliable one.

`isAppInDarkMode()` in `theme/ThemeManager.kt` combines them. **Everything outside the theme
package asks `isAppInDarkMode()`, never `isSystemInDarkTheme()`.** The phone's value is wrong
whenever the rider has chosen a mode that disagrees with it. The `SystemDarkThemeBan` detekt rule
enforces this; its baseline, `config/system-dark-theme-baseline.txt`, lists the reads that are
still to fix.

## The rider's theme colour

It has four roles (ground, ink, background, decor), one accessor each. See
[`THEME_COLOUR_ROLES.md`](THEME_COLOUR_ROLES.md); it is not repeated here.

## Colours that do not change with the theme

These are the same in light and dark on purpose, and are the allowed exceptions to "everything
comes from the palette":

- **Transport modes and lines**: `core/transport` `TransportMode.kt` and
  `nsw/NswTransportLine.kt`. They are the network's colours, the ones on the signs.
- **Stop labels**: `StopLabelIcons.kt` in `:feature:trip-planner:ui`, drawn on
  `md_theme_stop_label_surface`, which is the same in both themes so the label colours read the
  same everywhere.

## The map is always light

The map tiles have no dark style, so a screen showing the map is light whatever the theme. The
status bar over it is forced to dark icons (`StatusBarAppearanceEffect(lightStatusBar = true)`),
because theme-following icons would be light on a light map in dark mode. When the map is not
showing, the status bar follows the theme again.

## When a hand-written dark tweak is fine

When it adjusts an effect, not a colour: an alpha, a shadow, or a gradient's strength, chosen
with `isAppInDarkMode()`. Examples: `DiscoverChip`, `CloudGradientBackground`, `Button`'s
outline. If the tweak picks a different colour for dark, that colour belongs in the palette
instead.
