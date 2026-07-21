# Feature Quality Checklist

Read this **before writing code** for any feature that adds a screen, a section, or a new
surface for existing data.

Every item below comes from a real defect shipped in this repo and caught by the maintainer
during manual testing, not by detekt, not by unit tests, and not by the build. The point of
the list is to move that catching earlier: these are the questions to answer while designing,
so they never become a round of "you broke X, also Y".

---

## 1. Reuse audit — do this first, before any new component

Search before you build. Inventing a parallel version of something that exists is the single
most common waste in this codebase.

- [ ] Does a component already do this? (`grep` the feature and `taj` modules)
- [ ] Does an equivalent **state** already exist elsewhere — loading, empty, error, no-match?
      Use the same component *and the same wording*.
- [ ] Is there already a **manager / helper / gate** for this data? Reuse it rather than
      calling the service directly.
- [ ] Does the same information already render somewhere else? If so, reuse the exact
      composable so the two cannot drift.

> Shipped defects this prevents: a bespoke shimmer placeholder when `LoadingEmojiAnim` was the
> app-wide loading treatment; a hand-written "No stations match X" when SearchStop already had
> "No match found!"; a second availability fetch path that would have bypassed the existing
> Park & Ride rate limit.

## 2. Check the real data for collisions — in both directions

Before choosing a key for grouping, de-duplication or identity, look at the **actual**
production data (Remote Config defaults, fixtures, a real API response).

- [ ] Can one A map to many B?
- [ ] Can one B map to many A?
- [ ] Does the key still hold if **both** are true at once?

> Shipped defect: Park & Ride stations were de-duplicated by `facilityId`, which fixed
> "one car park, several stops" (Mona Vale) and left "one stop, several car parks" (Tallawong
> showing three times). Grouping by `stopId` instead would have flipped which one broke.

## 3. Configuration changes — assume the Activity is destroyed

Rotation, theme switch, font-size change, dark mode, split screen, unfolding. **A screen that
works until the device rotates is a broken screen**, and no static check will tell you.

- [ ] Every new `NavKey` route registered in `SerializationConfig.kt`
      (guarded by `NavKeySerializationConfigTest` — do not delete it)
- [ ] `rememberSaveable`, not `remember`, for anything the user would notice losing
- [ ] `collectAsStateWithLifecycle()` for `uiState`
- [ ] **State hoisted above any conditional branch that changes composition structure.**
      A composable called from two call sites (e.g. inside a dual-pane scaffold vs directly)
      occupies two different composition slots, so state remembered *inside* it is silently
      dropped when the layout switches.
- [ ] Anything in `rememberSaveable` is `@Serializable`, `Parcelable`, or has a `Saver`
- [ ] After rotation the user is looking at **exactly** what they were looking at: same text
      typed, same filtered results, same scroll position, same expanded rows

> Shipped defects: the picker crashed on rotation because its route was unregistered; typed
> search text vanished on rotation because the list pane was called from two call sites.

## 4. Empty, loading and error states — design all four up front

Enumerate the states before writing the happy path. Write them into the state class.

- [ ] Loading, empty, error, and content all exist and were each looked at
- [ ] The error is **actionable** where retrying could help
- [ ] Empty states are **scoped to the thing that is empty**. A section's empty state is a
      section-level prompt, not a full-page hero — the moment a sibling section has content,
      a page-level hero is stranded above real content and looks broken
- [ ] Nothing claims "there is nothing here" while something is plainly on screen
- [ ] Cached data beats a spinner: if data is already stored, render it instead of flashing a
      loader on the way to the same result

> Shipped defect: "Let's Go! Sydney" was the *saved trips* empty state rendered as a
> whole-page hero, so it sat orphaned above the Park & Ride cards with dead space beneath it.

## 5. Visual weight must encode role

- [ ] An **action** and a **data card** must not share the same treatment. If a button is
      filled the same way as a card carrying live data, they read as the same thing
- [ ] The loudest element on screen is the most important one
- [ ] Tap targets are clipped to their intended shape — an unclipped `klickable` ripples as a
      full-width rectangle regardless of what the content looks like
- [ ] Vertical padding is generous enough that rows read as blocks, not a dense list
- [ ] Section headers get more space above than below: the gap belongs to the break between
      groups, not the group's own title

> Shipped defect: "Add another" used the same fill as `ParkRideCard`, so an action and a card
> of live parking data were indistinguishable.

## 6. Accessibility is designed in, not retrofitted

Four pillars, all considered from the first commit: **screen reader**, **colour contrast**,
**text scaling**, **keyboard control**. Voice control follows from keyboard control, so
getting keyboard right covers it.

### Screen reader

- [ ] **Group a row into one node.** A card built from a badge, a title, a subtitle and a
      trailing indicator is announced as four separate stops unless it merges. Use
      `Modifier.semantics(mergeDescendants = true) { }` on the container so it is read as one
      thing, in order, and swiped through as one item
- [ ] **State goes in `stateDescription`, not the label.** Concatenating "Added" into
      `contentDescription` makes it part of the control's *name*; a screen reader should report
      the name and the state separately, and re-announce state when it changes
- [ ] **Set a `role`** so the control is announced as a button/checkbox/etc, and its available
      actions are correct
- [ ] **Hide decoration.** A purely visual indicator whose meaning is already carried by the
      row's `stateDescription` should be `clearAndSetSemantics {}` rather than repeating itself.
      Same for placeholders and loading skeletons
- [ ] **Images that carry meaning get a description; images that do not get `null`.** Never a
      description that reads out the file or component name
- [ ] Announcement order matches visual order

### Colour contrast

- [ ] Foreground on any themed fill goes through `getForegroundColor` /
      `ensureMinimumContrast` — never a hardcoded hex, never assume the theme colour is dark
- [ ] Verified for **every** `KrailThemeStyle` in **both** light and dark. There is a test for
      this (`ThemeContrastTest`); extend it rather than eyeballing a screenshot
- [ ] State is never conveyed by colour alone — pair it with a glyph, text or shape change

### Text scaling

- [ ] Text wraps rather than truncating. At large font scales a clamped name is unreadable,
      and a taller row always beats an unfinishable one
- [ ] Layouts grow with the text: no fixed heights around scaling content
- [ ] Checked at the largest supported font scale, not just the default

### Keyboard control

- [ ] Every interactive element is reachable and activatable by keyboard. `clickable` /
      `klickable` gives this for free; a custom gesture handler does not
- [ ] Focus order follows visual order
- [ ] Focus is visible, and does not get trapped in a sheet or dialog with no way back
- [ ] Nothing is reachable only by a gesture that has no keyboard equivalent

> Shipped defect: the picker row set `semantics { }` without `mergeDescendants`, so its badge,
> station name, subtitle and add toggle were announced as four separate items, and the added
> state was concatenated into the label instead of being reported as state.

## 7. The same data must look the same everywhere

- [ ] Same numbers, same typography, same colours, same wording, on every surface
- [ ] The identity badge/icon is the same component, not a lookalike
- [ ] If two surfaces render one thing, they share a composable

> Shipped defect: the map sheet recoloured availability text against its container while the
> home card used default content colour, so one facility looked like two different features.

## 8. One budget, one gate

When a second entry point reaches the same rate-limited or cached resource:

- [ ] It goes through the **existing** gate, not a parallel copy
- [ ] Shared cooldown/cache logic lives in one place both callers use
- [ ] There is a **test** proving the second entry point does not double-spend
      (see `ParkRideAvailabilityLoaderTest`)

## 9. Decisions get a comment and a test

If a choice is non-obvious — why this key, why cached-first, why this is a no-op — it needs a
comment saying *why* and a test pinning the behaviour. A future change should fail loudly
rather than silently regress.

## 10. Before handing anything over

Never describe a change as working when only `./gradlew` has run.

- [ ] `./scripts/fullQualityChecks.sh` green
- [ ] `./gradlew testAndroidHostTest --continue` green
- [ ] Installed, opened the changed screen, **rotated it**, rotated again while loading
- [ ] Navigated away and back
- [ ] `adb logcat -d | grep -A 30 "FATAL EXCEPTION"` clean
- [ ] Theme switched, light and dark
- [ ] Loading, empty and error states each seen
- [ ] **Say plainly which checks were skipped** if no device was attached. Do not call a
      change verified because the static checks passed

---

## Working style

- **Ask before building, not after.** A UX decision with two defensible answers is a question,
  not a guess. Guessing costs a build, an install and a round of feedback.
- **Prefer the boring existing thing.** Consistency beats a locally better idea.
- **Explain in place, never transiently.** No snackbars. If something cannot be actioned, the
  row says why, permanently, so it is understood before the tap rather than after.
- **Refactor rather than suppress.** Hitting `LongParameterList` or `TooManyFunctions` means
  the class is doing too much — extract a collaborator. Never `@Suppress`, never baseline.
- **Delete what an idea replaced.** When direction changes, remove the token, parameter or
  branch the old approach needed, so dead API does not accumulate.
