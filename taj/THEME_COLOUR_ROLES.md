# The rider's theme colour has four roles

Read this before drawing anything in the rider's theme colour.

The colour a rider picks in **Change Theme** is used in four different ways, and each way carries
a different legibility obligation. `taj` exposes one accessor per role rather than one accessor
for the colour, because a call site that does not say which role it means cannot be checked by
anything.

---

## Pick one

| Accessor | Use it for | Adapted? | Obligation, and who holds it |
|---|---|---|---|
| `themeGroundColor()` | a **filled shape with content drawn on top** — the Save button, the `P` badge, a selected chip | no | content-on-it ≥ 4.5, via `getForegroundColor`. Held by `ThemeContrastTest`. |
| `themeInkColor()` | **text, an icon or a stroke drawn straight onto a surface** — a stop name, Reset, the date chevrons, a card border | **yes** | the colour itself ≥ 4.5 text / 3.0 non-text. Held by `ThemeInkContrastTest`. |
| `themeBackgroundColor()` | the **translucent wash** behind card fills | no | content-on-it ≥ 4.5. |
| `themeDecorColor()` | **decoration** — a gradient stop, a ripple, the cloud field | no | none, and saying so is the point. |

There is one more, for a single genuine exception:

| `themeInkColorOn(background, minContrast)` | ink drawn on a ground that is **not** an app surface | yes, against the ground you pass |

Today only the time picker's hand uses it: the hand sits on a dial filled with
`themeBackgroundColor()`, not on a surface.

### Choosing in one question

> Is something drawn **on top of** this colour, or is this colour drawn **on top of** something?

- On top of it → **ground** (or **wash**, if translucent).
- It is on top of something → **ink**.
- Neither, because nothing is read → **decoration**.

### Thresholds

`themeInkColor()` defaults to `TEXT_CONTRAST_AA` (4.5). Pass `UI_COMPONENT_CONTRAST_AA` (3.0) when
the thing is a stroke, a track or an icon rather than text:

```kotlin
Text(text = stopName, color = themeInkColor())                          // read as text
Icon(painter = chevron, tint = themeInkColor(UI_COMPONENT_CONTRAST_AA)) // not text
```

Getting this wrong is easy and quiet. A chip's **label** is text even though the chip is a
control; the chevron beside it is not.

---

## Why this exists

Purple Drip measured **2.95:1** against the dark surface and **2.49:1** on a bottom sheet, against
a 4.5 requirement for text. It shipped that way. The rider's chosen stop name, the Reset button,
the date chevrons and the "Show previous departures" link were all drawn in it.

It was the only shipped theme that failed, but "only one fails today" is not a property anyone can
rely on for the next theme. The fix is not a better hex; it is that **ink is derived rather than
declared**, so a theme nobody has invented yet is already handled.

### Why one ink per scheme, not one per ground

Adapting against whatever ground the ink happens to land on is the obvious implementation, and it
is wrong: it produces a different shade of the same theme on the surface, on a sheet and on a card.
Open a sheet over a screen and two of them are visible at once.

So the ink is derived **once per theme and per scheme**, against the hardest ground in that scheme.
Harder ground means more contrast everywhere else, so a single value clears them all.

The hardest ground is **computed** from `inkGrounds()` by luminance, not named. Naming it means a
new surface token silently invalidates every ink. It is `pastDepartureRowSurface` in both schemes,
which is not the one most people would guess — in dark mode the *lightest* ground is hardest,
because dark-mode ink is light.

### What the derivation does

`Color.ensureContrastWith(background, toward, minContrast)` blends the colour toward the ground's
own `onSurface` along a 32-rung ladder and stops at the first rung that clears.

- **Total.** The last rung is `onSurface`, which clears every threshold against its own ground by
  definition, so a passing rung always exists.
- **A ladder scan, not a binary search.** Compose's `lerp` interpolates in Oklab, and contrast is
  not monotone along that path — 110 of 7680 measured ladders reverse somewhere in the middle. A
  binary search would return a wrong answer for those.
- **Quantised on purpose.** Rungs mean a one-digit edit to a surface token cannot shift every
  derived colour by 1/255 and churn every screenshot golden.
- **Unchanged when it can be.** A colour that already clears is returned byte-identical. Train,
  Bus and Ferry are untouched in dark mode; Purple Drip is untouched in light mode.

Hue drift across all six themes and every ground is at most **2.2°**, so the brand survives.

### The predecessor, and why it was not enough

`ensureMinimumContrast` raises HSV *value* and nothing else, so a colour already at full value
cannot be adapted further — and when its loop runs out of steps it **returns the colour anyway**,
with no signal. Swept over 1920 hue/saturation/value combinations it returns a still-failing colour
for **24%** of them against the sheet ground. Light mode never failed, because darkening has no
floor. That asymmetry is why the gap went unnoticed for so long.

It is still in the tree for callers that adapt a **transport line** colour against a known
background. For the rider's theme colour, use the role accessors.

---

## Two traps

**Alpha is ignored.** `contrastRatio` reads `luminance()`, which does not look at the alpha
channel. Passing a translucent colour — anything from `themeBackgroundColor()` — as a background
measures it as though it were opaque and over-adapts. Composite it over what is behind it first:

```kotlin
val dial = themeBackgroundColor()
val ground = dial.compositeOver(KrailTheme.colors.bottomSheetBackground)
val hand = themeInkColorOn(background = ground, minContrast = UI_COMPONENT_CONTRAST_AA)
```

**Do not key the derivation on an animated colour.** `KrailTheme.colors.surface` animates for
1500 ms during a light/dark transition. Deriving against it re-runs the search every frame and
shimmers the ink. `themeInkColor()` keys its `remember` on `(themeHex, isDarkMode)` for exactly
this reason.

---

## What holds this

| Check | Where | Catches |
|---|---|---|
| `ContrastAdaptationTest` | `:taj` | the derivation failing to reach its target for **any** colour on any ground — 1920 colours × every ground × both thresholds |
| `ThemeInkContrastTest` | `:taj` | a shipped theme's ink failing on a ground; also holds `inkGrounds()` against `KrailColors`, so a new ground token must be listed or excused |
| `ThemeGradientPairTest` | `:taj` | a theme's AI gradient partner chosen too near or too far in hue |
| `ThemeColorRoleRule` | detekt | the deprecated `themeColor()` and its import, and `themeGroundColor()` in an ink position — a `color`/`tint` argument or a `BorderStroke`, on a callee that does not take a fill |
| `ThemeContrastTest` | `:taj` | content drawn **on** a theme colour (the ground role) |

The first three are **self-healing over `KrailThemeStyle.entries`**, so a seventh theme is measured
the day it is added rather than the day someone remembers to extend a list.

---

## Adding a seventh theme

Nothing here needs updating. Concretely:

1. Add the entry to `KrailThemeStyle`. The compiler will demand `aiGradientPartner` — it is a
   constructor parameter precisely so it cannot be forgotten. It used to live in a map with a
   default, which meant a new theme silently wore Train's gradient.
2. Build. `ThemeGradientPairTest` fails if the partner sits outside the 90–150° window, and tells
   you the angle you chose.
3. That is all. The ink, the wash, the cloud-field tints and the on-theme glyph colour are all
   derived, and the contrast guards pick the new theme up automatically.

The ordering behind that is deliberate, strongest first: **derive it** (nothing to declare) →
**put it in the enum constructor** (a compile error at the line being edited) → **iterate
`entries` in a test** (grows itself) → **a detekt rule** (stops the wrong call). Reach for a test
only when the two above it cannot apply.
