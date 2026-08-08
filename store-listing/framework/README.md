# Store Panel Framework

App-agnostic template system for App Store / Google Play screenshot panels.
Nothing in this folder knows about KRAIL: apps supply a config (palette, copy,
screens, badges) and the framework supplies geometry, layout and rules. Designed
to be lifted into its own repo later; consumed here by `../krail/`.

## What a panel is

One store screenshot = one **panel**: a colour field, a fixed text zone, a
device slot showing one full app screen, and small decorations. Every panel of
a device class shares identical geometry so a row of them reads as one system:
**every device starts and ends at the same height.**

```
┌─────────────────────────┐
│ safe border 18u         │  nothing but background art may enter
│  [ghost]      [sticker] │  sticker: top-right, inside safe border
│  TEXT ZONE (fixed h)    │  headline stack, fixed height per class
│  ┌───────────────────┐  │
│  │      DEVICE       │  │  full screen, never cropped, alternating
│  │   (fixed slot)    │  │  tilt: odd panels -4deg, even +4deg
│  └───────────────────┘  │
│ breathing room          │
└─────────────────────────┘
```

## Device classes

Geometry per class lives in `devices.json`. Canvas sizes are the stores'
requirements; slot sizes are the template's internal layout at canvas scale.
Classes ship separately: iPhone 6.9", iPhone 6.5" (derived), Android phone,
Android tablet portrait + landscape, iPad 13", Play feature graphic.

## The contract (what an app supplies)

Per panel: `field` (two gradient stops), `propTints` (hi/lo), `headline`
(lines, each sized s/m/num), `sticker` (label + style), `ghost` (vertical
word), `badge` (optional square badge letter), `screen` (path to a full-frame
capture in this class's native resolution), `tilt` (auto by position).

Per app: display font (embedded), palette, capture set per device class.

## Rules (enforced by review + verify script)

1. One message per panel; benefit word first.
2. Headline claims must be visible in the screen below them.
3. The entire screen, never a crop; no transient prompts or dialogs captured.
4. One device chrome per row; N distinct screens for N panels, zero repeats.
5. Decoration never sits on the text baseline; one sticker per panel.
6. Safe border inviolate for foreground; background art may bleed.
7. Whitespace is a design element; the field must stay visible.
8. Exact store pixels and ratios; text zone under 20% of canvas height for Play.
9. No usage metrics, ever, in outward assets. Live network data is fine.
10. Render-verify the composed output before any upload.
