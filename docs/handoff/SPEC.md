# Saved Trips · Organise — Implementation Spec

**Feature:** Reorganise the Saved Trips screen so trips cluster under their starting anchor (Home, Work, Uni…), each rendered as a full-bleed "Cover Art" tile with user-picked color + emoji.

**Platform:** Compose Multiplatform (KRAIL · Sydney transit app).

**Design files in this package:**
- `Saved Trips Cover Art Spec.html` — final designs + visual spec (open this first)
- `Saved Trips Organise.html` — earlier exploration of 6 tile variants for context
- `tokens.css`, `krail.css`, `icons/` — the `taj` design system files used by the designs

---

## The core rule (anchor-first direction)

> Every saved trip belongs to **one and only one anchor**. The anchor is always the **starting point**. Tapping a tile under "Home" always means "leave Home, go to that place."

This eliminates direction ambiguity at the tile level — direction lives in the cluster header, not the tile. If the user wants the reverse leg, the trip-detail screen has a single **Swap** button that flips it. Swap is UI-only — it does not save a new trip; it just shows the timetable for the reverse direction.

## Anchor priority

The most-used stop label across the user's saved trips becomes the **PRIMARY** anchor. Subsequent clusters are ordered by usage count, descending.

- **9 trips → 🏠 Home** _(PRIMARY)_
- **2 trips → 💼 Work**
- **1 trip → 🎓 Uni**

Rules:
- A stop label needs ≥ 2 matching trips to earn its own cluster. Singletons drop to "Other trips" at the bottom.
- For new users with no clear winner, the first stop label they create becomes Primary.
- Ties broken by recency of last trip use.

## Tile (Cover Art)

| Property        | Value                                                         |
| --------------- | ------------------------------------------------------------- |
| Aspect ratio    | 1 : 1 (square)                                                |
| Background      | Single solid color, user-picked at save time                  |
| Foreground glyph| Single emoji at 130 px, opacity 0.18, floats right            |
| Title           | Place name, 24 px / 900 weight, white                          |
| Subtitle        | Stop name, 11 px / 600 weight, white at 0.85 opacity           |
| Direction badge | Top-left "glassy" pill (`rgba(255,255,255,0.22)` + blur)       |

### Direction encoding

| State    | Badge                          | Wording      |
| -------- | ------------------------------ | ------------ |
| Outbound | Solid glassy pill              | `Home to`    |
| Inbound  | Dashed border (no fill)        | `Home from`  |

**No `→` arrows anywhere.** Words carry meaning; dashed treatment reinforces it.

### Defaults (when user skips the picker)

- **Color** — `theme.transportMode.color` at save time
- **Emoji** — the stop-label icon (home / work / school / etc.)

### Bidirectional saved trips

If the user saved both `Home → Hospital` and `Hospital → Home`:
- Two tiles appear under the Home cluster — one solid badge ("Home to"), one dashed ("Home from").
- Don't merge silently — they're still two saved entries the user set up separately.
- _Future_: settings toggle "Auto-merge return trips" that hides the inbound tile.

## Stop-label creation flow (the prerequisite)

**Before** a user can save a trip with an anchor, they must first create the **stop label** itself. This is where the name, emoji, and color get assigned — once, up front — so every trip that uses that anchor inherits the same identity automatically.

When the user taps "Add a stop label" (from Settings or the empty-state on Saved Trips), they go through three required steps:

1. **Name** — free-text, e.g. "Home", "Work", "Mum's place", "Gym". Used as the cluster header on Saved Trips and as the word in the direction badge ("Mum's place to", "Mum's place from").
2. **Emoji** — required. Curated 40-emoji list of place glyphs (home, work, hospital, café, beach, gym, school, gym, airport, library, park…) with a search field for the full Unicode set. Used as the watermark glyph on every tile under that anchor.
3. **Color** — required. 12 swatches drawn from the KRAIL palette. Picker enforces WCAG AA contrast against white text and rejects swatches that fail. Used as the tile background for every trip under that anchor.

A live preview tile updates as the user chooses, so they see exactly what their saved-trip tiles will look like before committing.

### Why required, not optional

Every saved trip needs a recognisable identity to make Cover Art work. If labels were created without color/emoji, the system would have to fall back to defaults silently, which breaks the "I picked this" feeling that makes the feature memorable. Forcing the choice up front (one time, ~10 seconds) means every tile downstream is intentional.

### Editable later

Long-press a stop label in Settings → edit name, emoji, or color. The change propagates instantly to every saved trip and tile that uses that anchor.

## Save-trip flow (after labels exist)

When saving a trip, the start and/or destination stops are matched against the user's existing stop labels:

- If the start stop matches an existing label → the trip auto-clusters under that anchor; tile inherits its color + emoji.
- If neither end matches a label → trip drops to "Other trips" until the user labels one of its stops.
- The save-trip screen offers a shortcut: **"Make this a labelled stop"** that opens the stop-label creation flow inline if the user wants to promote a stop to anchor status on the spot.

## Detail screen (after tap)

```
┌──────────────────────────────────────┐
│  ← Trip                          ♥   │
├──────────────────────────────────────┤
│ ╔══════════════════════════════════╗ │
│ ║ FROM HOME                       🏥║ │
│ ║ Hospital                          ║ │
│ ║ RPA Camperdown                    ║ │
│ ╚══════════════════════════════════╝ │
│                                       │
│ ┌──────────────────────────────────┐ │
│ │ 🏠  Departing from               │ │
│ │     Home · Surry Hills            │ │
│ ├──────────────────────────────────┤ │
│ │ 🏥  Arriving at                  │ │
│ │     Hospital · RPA                │ │
│ ├──────────────────────────────────┤ │
│ │  ⇋  Swap · go from Hospital      │ │
│ │     to Home instead               │ │
│ └──────────────────────────────────┘ │
│                                       │
│ NEXT DEPARTURES                       │
│ 8:42  ON TIME · 36 MIN                │
│ T1    1 change · arrive 9:18          │
└──────────────────────────────────────┘
```

The hero card uses the trip's tile color; the place emoji floats as a watermark. Same visual DNA as the tile.

## Singletons row ("Other trips")

Trips that don't share an anchor with any other trip (e.g. `Gym → Café`) sit in an "Other trips" row at the bottom of the screen.

- Rendered as **smaller horizontal cards**, not square tiles, to signal they're a lower-priority list.
- Background is a **gradient** from start-place color to end-place color.
- Same Swap rule: tap = first-to-second.

## Motion · Organise toggle

| Time     | What happens                                                       |
| -------- | ------------------------------------------------------------------ |
| 0 ms     | Tap pill, flat list fades to 30% opacity                           |
| 120 ms   | Anchor stop-labels lift into cluster headers (scale 0.95 → 1.0)    |
| 240 ms   | Tile grid re-flows; tiles slide into their cluster slot, 30 ms stagger |
| 620 ms   | Singletons settle at the bottom                                    |

Toggling off plays the same animation in reverse.

## Accessibility

- **contentDescription** — `"Open timetable from {anchor} to {place}"` (or `"from {place} to {anchor}"` for inbound).
- **Color is decorative**, never the only signal — direction always uses words.
- **Min hit target** — 156 × 156 dp (well above 44 pt).
- **Color picker** enforces WCAG AA contrast against white text.
- **Reduce-motion** — Organise toggle becomes an instant fade (no slide/scale).

## Compose Multiplatform notes

- Reuse `KrailIcons` stop-label set; the place-emoji is stored as a unicode string on the trip entity.
- Tile color is a hex string on the trip entity; default = `theme.transportMode.color` at save time.
- Swap on detail screen is **UI-only flip** — does not mutate the saved trip; just swaps origin/destination state for that screen.
- Cluster-priority computation:
  ```kotlin
  trips.groupBy { it.anchorId }
       .entries
       .sortedByDescending { it.value.size }
  ```
- Anchor detection: a stop label appearing on ≥ 2 trips becomes a candidate anchor; ties broken by recency.

## Open questions

The implementing model should answer these before coding. Default recommendations in parens.

1. If a trip has neither end labelled (just street addresses), should it auto-cluster by GPS proximity? _(Recommend: not in v1.)_
2. What happens when a user has 30+ trips under one anchor? _(Recommend: collapse with "Show all" after the first 8.)_
3. Should "Other trips" also use Cover Art tiles, or stay as compact rows? _(Recommend: compact rows — they're the lower-priority list.)_
4. Should the Swap button on the detail screen also offer "Save reverse" as a one-tap shortcut? _(Recommend: yes, in a kebab menu.)_

---

## Prompt to paste into the implementing model

```
I'm implementing a "Saved Trips · Organise" feature in KRAIL (Sydney public-transport
app, Compose Multiplatform). Attached: SPEC.md, Saved Trips Cover Art Spec.html,
Saved Trips Organise.html (earlier exploration), and the taj design system
(tokens.css, krail.css, icons/).

Build the screen described in SPEC.md. Match the visual design in
"Saved Trips Cover Art Spec.html". Key rules:

1. Saved trips cluster under their anchor stop. The anchor is ALWAYS the
   starting point. Tapping a tile means "leave the anchor, arrive at the place."
2. The most-used anchor is PRIMARY and shown first. Other clusters follow in
   usage-count order. Singletons go to "Other trips" at the bottom.
3. Each tile is a full-bleed colored card. Color and emoji are user-picked when
   saving the trip; defaults are transport-mode color + stop-label glyph.
4. Direction is encoded by a glassy badge at top-left:
     • Outbound: solid badge, "Home to"
     • Inbound:  dashed-border badge, "Home from"
   No "→" arrow characters anywhere — banned in our codebase.
5. Tap → trip detail screen with a Swap button that flips direction in-screen
   only (does not mutate the saved trip).
6. Light + dark mode; theme color follows user's selected transport mode.
7. Use the taj design system files attached. Don't introduce new accent colors
   outside the palette.
8. Animation when "Organise" toggles: fade flat list, lift anchors into headers,
   re-flow tiles into clusters with a 30ms stagger. Reversible.

Output: Compose Multiplatform code in the existing taj/ module structure.
Match Material 3 component conventions where possible.

Answer the open questions in SPEC.md before coding if you have a strong opinion,
otherwise default to the recommendations.
```
