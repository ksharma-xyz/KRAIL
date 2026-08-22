# Three contrast guards, and the theme colour that fell between them

**2026-08-22** · Design system / a guard that measured the wrong pairing · ~6 builds, 2 install cycles, one afternoon

## Symptom

With **Purple Drip** selected and the device in dark mode, the rider's chosen stop name on the
search row was barely readable — dimmer than the "Destination" placeholder beside it, which is
deliberately quiet. The same purple was used for "Not now", "Reset", "Show previous departures",
the date chevrons and the KRAIL wordmark.

Worst of all, the time picker's hand was drawn in the theme colour on a dial *filled* with the
same theme colour. Purple on purple.

Light mode looked fine. Every other theme looked fine.

## Root cause

Two, and the second is the one worth remembering.

**1. `#AC00C9` measures 2.95:1 on the dark surface and 2.49:1 on a bottom sheet.** Text needs 4.5,
non-text needs 3.0. It was the only shipped theme that failed on dark.

**2. Three contrast guards already existed, all passing, and none of them measured this pairing.**

| Guard | Measures | Why it missed |
|---|---|---|
| `ThemeContrastTest` | content drawn **on** each theme colour | only looks at the theme colour as a *ground* |
| `KrailColorTokenContrastTest` | the fixed `KrailColors` tokens | theme colours are not `KrailColors` tokens |
| `TransportModeContrastTest` | `TransportMode.all` against both surfaces | `TransportMode.all` is Train, Metro, Bus, Light Rail, Ferry, Coach, School Bus. **Purple Drip and Barbie Pink are `KrailThemeStyle`-only and are never iterated.** |

The union looks like full coverage. It is not: nothing measured *a theme colour used as ink against
the surface it is drawn on*. The gap was invisible because each guard's name suggests it covers the
theme colours, and each one genuinely does cover something.

**3. The escape hatch could not have fixed it either.** `ensureMinimumContrast` raises HSV *value*
and nothing else, so a fully saturated colour with little value left cannot be adapted. When its
loop exhausts, it **returns the colour anyway, with no signal**. Swept over 1920
hue/saturation/value combinations it returns a still-failing colour for 24% of them against the
sheet ground. Light mode never fails, because darkening has no floor — that asymmetry is why this
sat undetected.

## Why it took so long

**Wrong turn 1: assuming the fix was a better hex.** The first instinct is to hand-pick a
dark-mode purple. That fixes one theme and leaves the next one to whoever adds it, which is the
same trap one level along.

**Wrong turn 2: reaching for the existing helper.** `ensureMinimumContrast` is right there and
named for exactly this job. Only measuring it revealed that it cannot reach 4.5 for this hue —
it tops out at `#DA00FF` / 4.45 after exhausting all 20 steps and returns it silently.

**Wrong turn 3: binary-searching the blend.** The obvious implementation of a replacement is to
binary-search the blend factor, which is correct only if contrast rises monotonically along the
blend. Compose's `lerp` interpolates in **Oklab**, and 110 of 7680 measured ladders reverse
somewhere in the middle. This was caught by sweeping before writing, not by a failing test.

**Wrong turn 4: deriving per ground.** Adapting the ink against whatever ground it lands on gives
a different shade on the surface, on a sheet and on a card — and a sheet opened over a screen shows
two at once. Caught by a reviewer question, not by a check.

**Wrong turn 5: measuring against a translucent ground.** `contrastRatio` reads `luminance()`,
which ignores alpha. The time picker's dial was passed in as `themeBackgroundColor()` — 45% purple
— so it was measured as though opaque and the hand over-adapted to near-white `#E1ADE6`. Only
sampling the rendered pixels off a device screenshot showed it. `KrailColorTokenContrastTest`'s
KDoc already warned about exactly this; the warning was in the wrong file to be seen.

## What would have caught it sooner

**Measure pixels, not intentions.** Every number in the fix was confirmed by sampling the actual
PNG from a device screenshot. That is what caught the near-white clock hand, and what proved the
derived value is byte-identical on device (`#CD7BDC`) to the host-side model.

**Sweep the input space before trusting a helper.** A 1920-colour sweep took minutes to write and
immediately falsified two assumptions: that `ensureMinimumContrast` was total, and that contrast is
monotone along an Oklab blend. Both would have been expensive to discover from a bug report.

**Ask what a guard measures, not what it is called.** All three existing guards passed. The useful
question was not "is there a contrast test?" but "which *pairing* does each one measure?" — a
three-row table that made the hole obvious.

**A union of guards is not coverage.** When several checks each cover part of a space, write down
the space and mark the parts. The gap here sat exactly where two reasonable scopes stopped.

## Actions taken

- [x] `Color.ensureContrastWith` — total by construction, blends toward the ground's `onSurface`
      along a quantised ladder, no monotonicity assumed.
      ([A11yColors.kt](../../taj/src/commonMain/kotlin/xyz/ksharma/krail/taj/theme/A11yColors.kt))
- [x] Four role accessors so a call site states its obligation: `themeGroundColor`,
      `themeInkColor`, `themeBackgroundColor`, `themeDecorColor`.
      ([ColorsExt.kt](../../taj/src/commonMain/kotlin/xyz/ksharma/krail/taj/ColorsExt.kt))
- [x] `ContrastAdaptationTest` — the 1920-colour sweep, asserting the derivation is total, and
      pinning the old helper's failure so the justification cannot rot.
- [x] `ThemeInkContrastTest` — the missing pairing, self-healing over `KrailThemeStyle.entries`
      and over `inkGrounds()`, and it holds that ground list against `KrailColors`.
- [x] `ThemeColorRoleRule` — detekt rule banning the deprecated accessor and the unadapted ground
      colour in an ink position. Zero offenders, no baseline.
      ([GUARDS.md](../testing/GUARDS.md))
- [x] `aiGradientPartner` moved from a map-with-default onto the `KrailThemeStyle` constructor, so
      a new theme cannot silently inherit Train's gradient. `ThemeGradientPairTest` holds the hue
      window — writing it revealed the KDoc's stated 97–134° range was never true.
- [x] The instruction lives in
      [taj/THEME_COLOUR_ROLES.md](../../taj/THEME_COLOUR_ROLES.md); this entry is the story.
