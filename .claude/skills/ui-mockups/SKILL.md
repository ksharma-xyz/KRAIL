---
name: ui-mockups
description: Design and present UI mockups for a KRAIL feature before any Compose code is written. Use whenever asked to "mock up", "design", "show options", "how would X look", or to plan a new screen, section, chip, card or any new surface for data. Covers the data-reality check, the frames every mockup must show (narrow, phone, landscape, foldable, dual-pane), state height stability, accessibility, copy rules, on-device AI surfaces, how to render, and how to present options so the maintainer can pick one.
---

# UI mockups for KRAIL

The maintainer picks a design from mockups, then it gets built. A mockup that skips a width,
a state or a font size gets sent back, so every item below is applied **without being asked**.
Nothing here replaces the source docs; it is the order to read them in and the frames they imply.

## 0. Read first

| Doc | What it decides for the mockup |
|---|---|
| `docs/FEATURE_QUALITY_CHECKLIST.md` | Reuse audit, real-data collisions, all four states, visual weight |
| `docs/TABLET_FOLDABLE_UX.md` | Breakpoints and what each screen does at each one |
| `core/adaptive-ui/.../DualPaneScaffold.kt` | Fixed 480 dp list pane (`DUAL_PANE_LIST_WIDTH`) |
| `composeApp/.../KrailNavHost.kt` + `navigation/entries/*Entry.kt` | Which routes are a Navigation 3 `listPane()` / `detailPane()` |
| `docs/LAYOUT_AND_INSETS.md` | Inset authority, bottom-anchored inputs, keyboard |
| `taj/THEME_COLOUR_ROLES.md`, `taj/THEMING.md` | Ground vs ink colour, contrast, light and dark |
| `taj/src/commonMain/.../theme/Color.kt` | Real token values for the mockup |
| The screen's own UX doc (listed in `CLAUDE.md`) | Invariants the design must not break |
| `feature/trip-planner/ui/ALERT_SUMMARY_UX.md` | The pattern for any on-device AI surface |

Find the real composables the design touches (card, row, leg) and look at their committed
screenshot tests in `<module>/screenshots/*.png` before drawing anything. Mockups copy the
existing visual language: spacing, badges, time styles. They do not invent a new one.

## 1. Data reality before pixels

Query the actual source (API, feed, database) and write down, in the mockup page's intro:

- **Which values exist** (enum levels, numbers, text) and which fields are always empty.
- **Coverage**: which modes, lines or regions have it and which do not.
- **Horizon**: how far ahead or back the data exists.
- **What absence looks like**: missing field, sentinel value, or an error.

Design for the data that exists, not the data the format allows. Show a "would need data we
do not get" option only when it is labelled as such.

## 2. Frames every mockup includes

Pick the layout mode from the **width the component is given and the font scale**, never from
the device type.

| Frame | Width | Notes |
|---|---|---|
| Narrow, big font | 320 dp at 200% font | Where layouts break first. Define the fallback mode |
| Phone portrait | 360 to 411 dp | Compact, the everyday case |
| Phone landscape | compact height, < 480 dp tall | Single pane always, and it scrolls. Keep the phone's vertical structure (stop lists stay vertical); do not flatten content into rows to save height |
| Foldable open / small tablet portrait | 600 to 840 dp | Medium, still single pane. Cap stretchy content (about 480 dp) and keep stacking vertically: there is room below. Do not fill spare width with side-by-side extras |
| Tablet, dual-pane | >= 840 dp, height >= 480 dp | List pane fixed at 480 dp, so list content renders as on a phone. Show what the other pane (map or detail) does |
| Detail pane | Nav 3 `detailPane()` routes | Wide: arrange pieces side by side that stack on a phone |

Show both **light and dark** for at least one frame.

Visual restraint, from maintainer review:

- No traffic-light colours (red, amber, green) drawn on top of a coloured surface such as a
  line-coloured leg card. Colour on colour reads cheap. Prefer one ink colour with shape, fill
  or density carrying the level, or put semantic colour on a neutral surface.
- Never label segments of a chart in place (text or underlines under each run). Real data
  alternates (seats, standing, seats) and per-segment labels collapse. Use one fixed-height
  legend row listing only the values present, plus a headline sentence.
- Mock the worst real pattern, not the tidy one: alternating values, all one value, longest names.
- Drop orientation or helper labels the rider can infer ("Front of train", "Back"). Every
  extra label crowds the component.

## 3. States and height stability (hard requirement)

- **States follow the data source.** First map each piece of data to the request that brings
  it. Data that arrives in the response the screen already loads has no loading or error state
  of its own: it is present from the first frame or absent. Only a separate request creates
  loading, slow and failure states. KRAIL calls NSW directly from the phone (no BFF in the
  live path), and every phone shares the app's NSW key, so a new request also costs data and
  shared rate limit. Prefer data the screen already downloads. When a second source is
  unavoidable, prefetch it once per screen and draw the extra only if it is already in hand
  when shown, so no in-place loading state exists. Say which source the design assumes.
- For each surface, draw the states that this mapping produces: loading, content, empty and
  error for the screen's own request, and only the extra ones a separate request adds.
- **Loading, content and error occupy the same height.** No jump when data arrives or fails.
  Draw the states side by side with a dashed guide at the shared height.
- Decide whether a component **exists** from data already in hand, before it is shown. If it
  qualifies, its slot is a fixed height from the first frame. If not, there is no slot. Never
  let a component appear or vanish after first render.
- Variable-length text (AI output, alerts) gets a fixed line budget and a fixed box.
- No data at all means draw nothing. Never a grey "unknown" placeholder.
- If a committed slot then fails or turns out empty, the default is a **smooth collapse to zero
  height**, not a quiet placeholder: fade the content out (about 150 ms), then
  `shrinkVertically(shrinkTowards = Alignment.Top)` (about 300 ms), so content below slides up.
  Show it in the mockup as a filmstrip (content, fading, gone).
  - Collapse at most once per appearance. If data returns, expand once, with hysteresis
    (about two polls) so it never yo-yos.
  - Delay the loading skeleton about 300 ms, so a fast failure never draws anything.
  - One animation owner: if a parent already uses `animateContentSize`, the inner exit drives it.
  - Reduced motion: remove instantly. Nothing is announced to screen readers.
- State survives rotation and theme change (`rememberSaveable` for anything a rider would notice).

## 4. Accessibility

- **Never colour alone.** Every colour-coded value also has words, a count or a shape.
- **Never dim text with opacity** to make it secondary: on a coloured card it drops below
  4.5:1 (a 75% cream label on a line-coloured card measured about 3.9:1). Show hierarchy with
  size and weight at full ink. Behind text on a coloured card, darken (dark theme) or whiten
  (light theme) the surface, never add a light tint of the ink. Composite before measuring.
- Text contrast via `themeInkColor()` (4.5:1); strokes and icons at `UI_COMPONENT_CONTRAST_AA`.
  Amber and yellow fail as text on white: use them as fills only.
- 200% font frame is mandatory (section 2). Text wraps or the layout switches mode; it never
  truncates meaning.
- Touch targets at least 48 dp.
- Write the screen-reader sentence for each new element in the mockup notes.

## 5. Copy

- Rider words, not feed jargon (`STANDING_ONLY` is "Standing only").
- No arrow characters, no em dash, no middle dot. Not in copy, not in the mockup page.
- One vocabulary across every surface of the feature.
- Name any other service the way the station board does: line, clock time with am or pm,
  platform ("the next T1, at 8:59am from Platform 1"), and make the mention jump to that
  service in the UI. A bare "the 8:59" is ambiguous.
- Only refer to things a rider can see where they are. No internal numbering (car 6, stop
  sequence, trip id); use what is visible: front, middle, back, the end the train comes in from.
- Full sentences with a verb; no ticket-stub fragments ("At Central 9:26"). Read it aloud.
- Who gets the verb depends on the cost of being wrong. Cheap and checkable (where to stand):
  speak to the rider, "Seats at the back of the train". Costs time (another service): the
  service gets the verb and the rider keeps the choice, "Want a seat? The next T1, at 8:59am
  from Platform 1, has seats available and is due at Central at 9:26am". Never "skip", "wait"
  or "don't board".
- Name the payoff in the data's own words, never a vague one: "seats", not "room" or "space".
  Match the level ("Seats at the back", "A few seats left at the back"). Give other services a
  clock time, never "3 minutes behind", and an arrival as "due at", never "by" (a promise). Never promise ("you will get a seat"). Match certainty in the wording: live
  "now", forecast "expected", proven pattern "usually", no data says nothing.
- Before mocking advice, list the riders it could hurt (needs a seat, wheelchair or pram,
  night travel, tight connection, visitor) and give each a rule. Safety and accessibility
  override optimisation.

## 6. On-device AI surfaces

Follow `ALERT_SUMMARY_UX.md`: `AiTextService` in `:core:ai-text`, three gates (Remote Config
flag, device availability, call outcome) that all collapse to the same result.

- The code-written surface (chart, headline) is the baseline, correct on every phone. AI is an
  addition and must **only add information the screen does not already show**: never restate
  a headline, label or level. Tell the model what is already on screen, and drop any reply that
  only restates it.
- On failure, drop, or restate, the AI box **fades and folds away**. No fallback text: a fallback
  that repeats the screen is duplication.
- Mock the AI version **and** the no-model version. Most phones have no model.
- The model only gets facts that code built. Before showing a reply, code checks that every
  number, stop, time and level word in it appears in those facts. Any miss: drop the reply.
- AI box keeps a fixed height across generating and resolved; failure folds it away (above).
- Reuse `AiWheelMark` and `Modifier.aiGradientBorder` in the rider's theme gradient.

## 7. The review page (the standard deliverable)

**One page per feature, for its whole life.** Design, rider thinking, research and results,
and decisions all live on the same page as collapsible parts (`<details>`), with a jump bar
at the top. Never start a second page for research or principles; add a part instead.

Every mockup round is **one interactive HTML review page, published as a private Artifact**,
and the conversation about it happens in the page's comments. Long explanations go on the
page, not in chat; the chat message is the link plus one or two lines.

The page contains, in this order:

1. **What the data allows**: a few tiles (section 1).
2. **A sticky control bar** that switches the whole page live: the option being decided (for
   example colour treatment A to D), app theme dark or light, and line or mode colour.
3. **Live phone frames per surface**, tappable like the app (cards expand, rows open), each with
   **scenario buttons** for every state in section 3: loads normally, slow load, fails after
   loading, fails fast, live updates, not eligible. A side column says what each scenario shows
   and the screen-reader sentence.
4. **AI states** when the feature has one: model answers, fails a check, phone has no model.
5. **Every width** from section 2 as static frames in a sideways-scrolling band.
6. **Open decisions** as clickable option cards with a "My pick" tag, and a fixed bar that
   builds an answer code (`1C 2B 3B`) with a Copy button, so the maintainer can paste it into a
   comment or chat.
7. **Correctness checks** that must be settled before building.

The page itself is always **light** (maintainer preference), whatever the viewer's system
theme; the phone frames carry their own app dark or light switch.

Motion mimics the app, softly: expand with `grid-template-rows: 0fr to 1fr` over about 320 ms
on an emphasized ease (stands in for `animateContentSize`), fades about 150 to 250 ms, level
changes about 600 ms, skeleton shimmer, AI wheel and border turning while generating and
decelerating to a stop when done (`ALERT_SUMMARY_UX.md`). Honour `prefers-reduced-motion`.

Iterate by republishing the same file (same URL). Read and answer comments with the
ArtifactComments tool. Keep a static PNG render only for quick checks or when a page cannot be used.

## 8. Rendering checks

- Write the review page in the session scratchpad, with token values copied from
  `Color.kt` and Roboto. No external images.
- Before publishing, take one look at a local render with headless Chrome:
  `"/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" --headless=new --hide-scrollbars --force-device-scale-factor=2 --window-size=W,H --virtual-time-budget=4000 --screenshot=out.png file://in.html`
  Windows narrower than 500 px crop silently; draw small frames on a wider page instead.
- Look at the PNG yourself before sending: clipped bottoms, misaligned labels, leftover old copy.
- Share the published link; a file path alone is not enough.

## 9. Presenting

The review page and every message about it follow this order:

1. **Data reality**: one short paragraph (section 1).
2. **Options**, lettered, each with a one-line trade-off. Two to four options, not ten.
3. **Recommendation**: which one, and why it changes what the rider does.
4. **Decisions needed**: in their own bullet block, separate from the prose.

After the maintainer picks, the next round shows the chosen option across every frame in
section 2 and every state in section 3, then the build starts. Nothing is built before a pick.
