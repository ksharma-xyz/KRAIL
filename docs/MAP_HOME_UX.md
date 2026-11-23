# Map-first home: design rules

> **The home part of this document is SUPERSEDED (2026-11-22).** The map home is not used. The home is main's Saved
> Trips screen again, with two additions, described in "The classic home" below. These stay in force: everything about Search Stop (rules 8, 9, 26, 33 to 36 and 39 and the
> Search Stop section), the Intro's hand-off, the language and spacing rules, and the cost rules. For the home these are
> SUPERSEDED, and describe code that is kept in the repository and drawn by nothing: rules 1, 2, 3, 5, 10, 12, 20,
> 24, 28, 29, 31, 38, 40 to 42 and 45 to 47, and the sections Home structure, The card shell, The home stack and
> Reorder mode. Rules 6 and 7 hold in part: the home has one search bar with the microphone in it, and Search Stop
> has no microphone.

## The classic home

What the Saved Trips screen is now. It is main's screen, file for file, plus these two additions. Nothing else on it
changed: the title bar, the tracking card, the section titles, the saved trip cards, reorder and its tip, the Park &
Ride cards with the pink `P`, and the Add Park & Ride card with its own line.

| Part | What |
|---|---|
| Title bar | KRAIL, Discover and Settings, as on main |
| Pills | Home and Work, inside the sheet at the bottom, directly above the search bar. They stay put while the list scrolls and go with the sheet while the cards are being reordered |
| List | Info tiles, Tracking Real Time, Saved Trips, Park & Ride, as on main |
| Sheet and bar | main's theme coloured sheet at the bottom, in every posture, holding the pills row and, 8 dp under it, one read only search bar. A tap opens Search Stop on the destination field. The microphone at its end opens Ask KRAIL and shows only where Ask KRAIL is available |
| Wide window | The 480 dp list pane on the left and the map pane on the right, as on main. The location permission chip is on the map pane |

**The pills.** A row of whole pills in the sheet, directly above the search bar: the sheet's own 16 dp side inset, 8 dp
apart, an 8 dp gap (`spacingM`) down to the bar, each pill with a 48 dp touch target. They are not in the list, so the list
is main's list. A label with no stop is taj's `OutlinedButton` at its chip size, reading "+ Home" or "+ Work" (a label the
rider made reads "+ " and its name). Its text and outline are the near black ink that `getForegroundColor` picks for the
sheet's colour, passed through the button's own `colors` parameter, because taj's default (cream in dark mode) is only 2.21 to
3.29 to 1 on the four theme colours and the near black is 5.92 to 8.80. A label with a stop is the in-app near black label pill (`SetLabelPill`), with the
label's icon and name and a 1 dp `outlineSubtle` hairline drawn inside it, so its edge shows on a dark page. The two are the
same height, so the row does not move when a label is saved. When the pills do not fit one line the whole pill goes onto
the next line: the row never scrolls sideways and nothing is shrunk or clipped. A pill's text is one line and ends in an
ellipsis only if that one pill is wider than the row. A set label plans from the nearest stop, or opens Search Stop holding
that stop as the destination when there is no location fix. An unset label opens Search Stop to set it. Both are the
existing `LabelChipTapEffect` and its rules, unchanged.

**The sheet and the bar.** The pills and the bar sit in the sheet `main` drew the search row in: the rider's theme colour,
rounded on the top two corners only (36 dp), full width from the screen edge down behind the navigation bar, with 28 dp
above the first row and 20 dp below the bar plus the navigation bar. It is 96 dp tall without pills and 56 dp taller with
them, and taller again for each line the pills wrap onto at large text. It slides up and fades in once the saved trips have loaded, as on main. The bar is
taj's `TextField`, read only, in taj's own default field colours (the page surface, so a light pill in light mode and a dark
one in dark mode, never the inverted set), 48 dp tall, a full pill, with the placeholder "Where are you going?", inside
the page's 16 dp side inset. The microphone is taj's `MicIcon` as it ships, plain, in the bar's content colour, in a 48 dp
slot flush with the bar's end. There is no From bar, no go circle, no swap button and no collapsed "Plan a trip" button.
The list keeps the sheet's measured height plus 8 dp clear at its end, so the last card scrolls fully into view at every
text size, and the sheet is not part of the list's layout.

**Not on the home.** The map, the Park & Ride tiles, the nearby station suggestion, the notices row, floating stop cards, pins and
the location notice are not drawn on a phone. The location notice is Search Stop's, and the wide window's map pane
carries it as the same small button, at the pane's top left corner.


## Objective

A rider opens KRAIL and, without reading anything, reaches what they came for.
From home a rider can:

- Open a saved trip with one tap on its card.
- Search for a new stop, pick it and see trips. As few taps as today, never more than one extra.
- Read the live free spots for a saved Park & Ride station, on open.
- Find a stop near them on the map.
- Set up Home, Work and the first Park & Ride station.

The map is for place. It answers "where is this stop" and "what is near me" without a screen change.
It is the background the rider always has, not a screen they navigate to.

## Targets

- Targets are measured before and after, never guessed.
- Taps from open to a timetable for a saved trip must not go up.
- Taps from open to a trip results list for a new destination go up by at most one.
- Time to first content (cards and pill) comes from local data only and never waits for map tiles.
- Network calls on open do not rise beyond the Park & Ride cooldown (see Cost).
- Rotation, 200% text, dark mode and a screen reader work on every screen.

## Non-negotiables

If a design breaks one of these rules, the design changes, not the rule.

### Layout

1. **The map is the home screen.** Everything else floats on top of it as cards. No bottom sheet, no drag handle, no snap
   points, no docked panel. Not even a quiet one.
2. **Floating means floating.** Cards and bars have the map visible around and between them. A white wrapper that holds
   other cards is a docked panel in disguise.
3. **One list, one card shell.** Saved trips and Park & Ride are the same kind of thing (a place the rider goes) and look
   like one family: same tint, radius, padding and inner rhythm. They are never a floating card next to a white box.
4. **Nothing important is reachable only by a sideways swipe.** If the rider has to find it, it is on screen vertically. A
   sideways row hides what is off the edge.
5. **Saved trips are never hidden.** Park & Ride is on the first page, with an Add button, and expands and collapses in
   place.

### Search

6. **One pill search bar.** On home and on Search Stop. Never two fields stacked. Home is always the one pill, whatever
   stops a pin card, a chip or Ask KRAIL has handed on: those open Search Stop, which holds the To and From form.
7. **The microphone is on the home pill only.** Never on Search Stop.
8. **Search Stop always opens on a clean map, with no keyboard.** Only four things float on it: the box (the Timetable style card, see The Search Stop box) at
   the top, the location notice under it while location is not allowed (the chip is replaced), and on the control line the Options button, only while
   the location is allowed, with the map's own location control (rule 39). There is no list, no recents, no near you and no card at open, so the map stays
   the screen. The rider taps a pill to raise the keyboard and the list.
9. **The recents are in the list state, not on the map.** (This replaces the earlier rule that floated a recents card on
   the map at open, which covered most of it.) Tapping a pill is the list state: the cloud gradient covers the map and the
   list sits under the same box, drawn as the Search Stop list always was, straight on the gradient with no card behind it.
   From the top: the label row (the set labels, Home, Work and any custom label, then taj's Manage button at chip size, one
   row that scrolls sideways), Select on map (it returns to the map state), the heading with Clear all as taj's small text
   button, then the recents (at most five), the nearest stops under Near you, or the suggested stops. Each stop is a whole
   row as `StopLabelAssignRow` draws it: the name, its mode badges under it, a chevron at the end that opens the label wall,
   and a divider under it. A stop that already has a label shows its small label pill and no chevron. With no recents the
   nearest stops show when a location fix exists, and otherwise the suggested stops. The mode filter is not in the box: it is
   the Options button on the map's bottom row (rule 39). The map stays composed under the gradient, so going back to it
   keeps its camera, and its controls are hidden from a screen reader while it is covered.
10. **Everything that floats on the map is the soft theme tint, in light and dark mode, never a near black slab and never pure
    white.** Cards and chips take their fill from one accessor, `mapCardColors()`: the theme colour at a few strengths laid
    over the page's own surface, the family the Timetable's From and To card and the saved trip cards are drawn in, so Train
    is a light peach in light mode and a dark brown in dark mode, Metro a light mint and a dark green. The stop card's body
    and the Options card are the soft step, and the stop card's header is a stronger step, at least 1.15 to 1 from the body,
    with text at least 4.5 to 1 on both for Train, Metro, Bus and Ferry in both modes (`MapCardColorsTest` prints the table).
    A chip (the Options button and the Location permission required chip) is the Timetable's chip: the tint with the
    page's text colour, plus a 1 dp `softLabel` hairline (`mapChipEdge`) so a pale chip holds against the light map, and
    Options takes taj's default solid theme colour while any mode is switched off. Placeholders on a tint use the page's
    text colour, never the placeholder token, which fails on the dark tints. **(The glow here is replaced: Search Stop has no glow, its edge is the `softLabel` hairline of The Search Stop box.)** Cards wore the info tile's glow around their
    corner (`Modifier.infoTileGlow`, through `searchStopEdge`) and the box was taj's `surface`; both are replaced. A pale card is
    about 1.0 to 1 against the light map, which is why the hairline is there. A translucent theme wash is not used
    as a fill over the map. **taj components are used as they ship**, with their own sizes and defaults and only the
    parameters they expose: colours go through a component's own `colors` parameter or taj's text colour locals, never a
    restyle, a custom shape or shadow, a size outside `ButtonDefaults`, or a wrapper that redraws a taj look. taj has no
    card, chip or pill, so a container is an existing in-app card shell. `FloatingDarkTheme`, taj's `KrailTheme` fixed to
    dark, is kept only where a reused component reads mode adaptive tokens inside and takes no colours and sits on a fixed
    dark card (the Park & Ride pin card, which keeps the fixed dark until it moves to the tint). The pills are taj's `TextField`, 48 tall, full radius, in the
    shared `softFieldColors()`: a fill that is a touch darker than the box in light mode and a touch lighter in dark mode,
    the page's own text colour, and a placeholder that passes 4.5 to 1 on it, with no ring and no border of their own. The
    fill is about 1.16 to 1 against the box in light mode and 1.22 in dark, soft on purpose, and the pills read because they
    are rounded and hold text. The home search pill is the same taj `TextField`, read only, with the fixed dark fill.

    Placeholders use the same line box as the content they stand in for.
23. Replaced by rule 44.

### Exceptions to rule 4

- The Park & Ride row on home is one horizontal row of cards, because stations are short cards and a row keeps them in a
  fixed place instead of pushing them down a long list of trips. The next card always peeks, the map's P pins are another
  way to reach any station, and a station expands in place.
- The label row at the top of the Search Stop list (Home, Work and any custom label, then Manage) is one row that scrolls sideways.
- The home chips and the Search Stop mode chips are not exceptions: they wrap whole at every text size, with no sideways scroll and no fade.
- Nothing else is allowed to scroll sideways.

### Forms, cards and behaviour

24. **(Replaced by the location notice, see The Search Stop box. The chip is kept for the saved trips map pane.) Location needs one small button.** When location permission is not granted, one small status button, taj's filled
    `Button` at its extra small size (normal weight text) in the soft theme tint with a hairline (rule 10), with a real 48 dp
    touch target, sits directly under the box on Search Stop's map state, at the box's left edge and one 8 gap below it. It is not inside the box, which holds only
    the To and From pills. At large text its words wrap inside it. It is not drawn in the list state of a phone, which is the
    box and the list, and a wide window's list pane keeps it. Tapping it asks for permission, or opens Settings if refused.
    On home the same status is the same button, and it is one of the whole items in the chips' own wrapping row, left aligned
    on the chips' edge: from the bottom the search pill, then that row, then the cards. "Map offline" is the same button, a
    status that does nothing when tapped. When the line is full a notice goes onto the next line with the chips, and nothing
    is squeezed, clipped or hidden. The group grows upward and the cards give way to it, and an open card or reorder mode
    takes the whole group.
25. Replaced by rule 36.
26. **The go circle has one state.** It is a plain arrow. The next screen owns loading, success and error, so this screen
    never shows a trips error. The chips have no chevron: they are not dropdowns.
27. **The spacing scale is 4, 8, 12, 16.** Edge inset 16 on every side. Gap between floating elements 8. Card padding 16 on
    all four sides, and padding inside any card is equal top and bottom: a stretched card centres its content, a list card
    uses fixed row padding, never fixed row heights.
28. **Chips share the pill's row** when the row is wide enough (about 650 and up) and otherwise sit above it, so a wide
    screen never shows a short row of chips beside empty space.
29. **Home order: two trips, one Park & Ride row, then the rest.** The top two saved trips come first as the app's normal
    cards, then one horizontal row of Park & Ride cards (big number, then the station name in the name type of the old card), then any further trips. With one
    trip the row follows it, with none it comes first, with no stations there is no row. Trips and the row itself are
    reordered in reorder mode (rule 31). There is no Settings choice for the row's place: it sits after the second trip
    until it is dragged. A tapped station expands in place to the full row width with each car park's numbers and a
    Directions button that opens the default maps app.
30. **No map options sheet.** The map always loads stops within 3 km, from one config value (Remote Config
    `nearby_stops_radius_km`, default 3.0). There are no distance scale or compass toggles, no radius chips and no
    first-open sheet. Park & Ride stations show on the map as the P disc the Add Park & Ride map already draws, with the
    cached number for saved stations.
31. **Reorder mode is explicit.** Long-press enters it, a dark Done at the top right saves, back or a swipe cancels without
    saving. The logo, gear, pill and chips are hidden while it is on. The Park & Ride row is placed by dragging it, never by
    a setting.
32. **Every pill is a pill.** The corner radius is half the height at any size or font scale. Geometry, symmetry and type
    are checked by measurement, never by eye.
33. **Two surface colours in any group of floating controls, and one on Search Stop.** On home, in the To and From block, they
    are near-black and white: the field being edited, the go circle and Done are near-black, the other field is white. Orange
    is not used there. **(Replaced on Search Stop: the box is one card in the soft tint with two plain rows, and the go circle is the theme colour, see The Search Stop box.)** The count of distinct backgrounds is checked in light and dark.
34. **A text field is one line, 48 tall, and ends in an ellipsis.** This covers the home pill, the search pill, the To value
    and the From value. The accessibility label carries the full text. Only single-line fields may truncate; chips, cards,
    badges and rows still wrap or stack whole items.
35. **The search pill has a back arrow and an X. (The group card has no back arrow now: system back does the two step back, and the X takes the row's circle slot while the row holds text. The bare single pill keeps its arrow.)** Back is a 48 box at the left, like today's search bar. The X shows only
    when the field has content and only clears it, and never dismisses the screen. Back follows rule 39, except from the
    start field, where it returns to To and From with the destination kept (on the bar that still needs a stop). System back and the iOS swipe do the same. In To
    and From the X clears the destination and raises the keyboard. The To row has a matching 48 column with the destination pin
    so From and To values start at one x. The arrow and the X are drawn in the bar's content colour, never a hex.
36. **From is on top, To is below it, and neither row ever moves.** On Search Stop both bars are the same taj text field as a
    row of the Timetable style card, From above To, a divider between them (the pills, the back arrow and the go circle at the end of To as 4 around 40 are replaced, see The Search Stop box). Tapping a bar edits it in place: the query field and
    the keyboard move to it, and nothing else changes, because the idle bar is the same field unfocused, and a tap on it
    focuses it. Focus shows by the caret, the keyboard and the placeholder, never by a fill, a ring or a colour swap, and no
    row changes position. (On the classic home, with the maps flag off, the bar being edited is still the dark one and the
    other the light button, with To above From as the home block draws it.) The back arrow stays at the start of the first
    bar, which is From, or the one bar of a single field, in every state. To has the destination pin at its start, the pin
    the stop rows draw for a destination. The From dot cannot share the 48 column with the arrow, so From has none. Both
    rows are the same width and share both edges. The go circle is 40, at the end of the To bar with 4 around it, and has
    one state. While To is edited and nothing is typed it keeps its place, and the clear X takes it only while there is text.
    **A placeholder is never a stop name.** Each bar has one fixed prompt: From reads Choose starting point and To reads
    Choose destination. A stop a bar holds is its real text while the bar is at rest. Tapping a bar that holds a stop starts
    its field empty with the fixed prompt showing, the old stop is not drawn as grey text, and leaving without a pick
    restores the selection.
    **A card only when two fields show.** The box is a card, with its edge and padding, only while From and To are both
    there. A single field (a label being assigned, or one leg of a trip being changed) is one bare rounded pill on the map,
    full radius and 48 tall, filled with the card's own surface and wearing the same edge, with the back arrow inside it and
    the same 16 side margins. The choice follows the number of rows the form shows and nothing else (`groupCardShownFor`).
    The label field's prompt names the label: Search and select Home, Search and select Work, or Search and select followed
    by the label's saved name exactly as saved.
37. **Every end of a saved trip follows one rule.** A labelled end is the label in bold with its stop small under it. An
    unlabelled end is its stop name. When both ends are labelled the card keeps the app's one-line form, "Home to Work",
    with no stop names. A label shows on a card whenever that card's stop is the labelled stop.
38. **Delete is the app's own delete circle.** In reorder mode a trip card carries `TripDeleteOverlay`: a 24 circle in the
    ink colour with a white X (12), at the card's top-right corner, overhanging it by 6, with a 48 hit area. Stations do not
    carry one and do not reorder one by one. The Park & Ride row is one outlined block with a grip that moves up or down as
    a unit. A station is removed on the Add Park & Ride screen with its toggle, or from its P card, which reads "Remove from
    home" when the station is on home and "Add to home" when it is not. Done saves, back or a tap on the map cancels.
39. **Search Stop has two states.** It opens as the clean map state (rule 8): the map, the box with the two pills, the location
    chip while location is not allowed, and the bottom two lines, with no keyboard. The lowest line is the map's own info icon
    (its attribution, a licence requirement that is never turned off) alone at the bottom most left, resting on the library's
    own margin with no padding of ours. Above it is the control line: Options at the start and the location control at the
    end, on one centre line at every text size. Options stands over the icon in z order. The pill is 4 above the visible i
    (spacingXS), measured from the i and not from the icon's slot, so the gap is the same at every text size and the line rises
    with the pill. Its 48 finger target is a real box that is itself the clickable, and it ends where the i begins, so it
    reaches over the icon's empty top edge and never over the i. Options and the i share the box's left edge. Options is taj's
    extra small `Button` reading Options after the filter icon, in the soft theme tint with a hairline (rule 10). It opens the mode chips as a card 8 above
    the line's visible top and shuts them again with a short fade and scale from the card's bottom start corner (200 ms in,
    150 ms out, reduced motion plain), a tap on the map shuts them too, and the selection (which modes show their stop pins) is kept while
    the card is shut. The card is the soft tint with the glow, as wide as the box, and its chips always wrap whole and never
    scroll sideways. Options takes taj's default solid theme colour while any mode is switched off, with no count. Options
    is drawn only while the location permission is granted (`modeControlsFor`): not asked yet or refused it is not drawn
    at all, it fades and scales back in when the permission is granted, and the modes the rider switched off are kept.
    Options and the location control also fade and shrink away while a stop card is open, and come back when it is dismissed,
    and an open Options card is shut as the button goes.
    Options and the card are in the map state only and are gone while the keyboard is up. The map's compass and scale bar
    start 8 under the box and the chip. Tapping a pill is the list state (rule 9), with the keyboard. X clears the text and
    keeps the recents. Back, or Select on map, returns to the map state, and back again goes home. System back is a two-step
    exit. Rules 9 and 39 are one picture: the map state is clean, and the list state is the list the Search Stop screen
    always had.
    **A pick never leaves the screen.** On the To and From form every pick (a recents row, a label shortcut, a result, a stop
    card action) fills its bar and the screen stays. Editing moves to the other bar while that bar is empty, with the keyboard
    following it, and to nothing when both hold a stop. Only the go circle leaves, with both stops, and back leaves with nothing,
    so a half pick is dropped. The go circle opens the trip results over Search Stop, so back from them lands on Search Stop
    with both stops still set, the go circle shown and no keyboard, and back again goes home. A label, or one leg of a trip, is a single field and returns at once with its stop.
40. **A Park & Ride card never promises a trip it cannot start.**
41. **Home and Work chips stay once set.** Tapping Home plans a trip from the nearest stop to Home with no saved trip
    needed. There is no long press on a chip. The app's own Manage stop labels screen (expand a row: Save changes, Remove,
    Delete) opens from the Manage button on the label row in Search Stop. There is no floating menu. The chips sit above
    the pill, in the same column as the cards. Each chip is taj's filled button at its small size in the fixed dark, and a set
    label, an unset label and Park & Ride are the same chip. The chips are whole items that wrap at every width and text
    size, never a row that scrolls, and the notices (rule 24) are whole items in the same flow.
42. **Nothing is ever laid over content.** No toast, snackbar, banner or floating menu. The result of an action is shown by
    the screen changing. Where a message is needed, it takes a place in the layout and everything else moves to make room.
43. **Icons and behaviours are the app's own.** Find the app's version of a control before drawing it. A new icon, menu or
    pattern needs an explicit yes.
44. **No custom transitions between screens.** Screens change with the platform's standard navigation transition only. No
    pill that rises while cards fade, no card that grows into its screen. What floats on the map inside Search Stop moves on
    the one motion spec (Motion), and only there.
45. **One column, one set of edges.** Trips are one vertical column, at most 560 wide and centred. The cards, the chips and
    the pill share its left and right edges at every size. Expanded and dual screen keep the 480 list pane with the map
    beside it, and the same rule holds inside the pane.
46. **First run shows one choice at a time.** A new rider sees the map and only the "Show stops near you" card. Whatever
    they answer, the rest appears: the chips, the pill and the suggestion below. A plain fade is acceptable, a choreographed
    one is not.
47. **A Park & Ride suggestion appears when a station is within 5 km:** the nearest one to Home, or to the current location
    before Home is set, as a card with Add to home and Not now, so the first station never needs the + chip. Local only, no
    network. The radius is the Remote Config value `park_ride_suggestion_radius_km` and a dismissed card stays gone. It shows
    once, never when a station is already saved, and never when the Intro already asked. The card is the Park & Ride tile's own
    shell and figures with the two buttons under them, in the row's place while there is no row. Home that has no station near
    gives no card, even with the rider at one.
48. **Every X is inside a circle.** The close X of the stop card and the clear X of the search field are taj's round icon button,
    a 40 dp circle in a real 48 dp box that is itself the clickable, because Compose's minimum touch target loses to the map
    under a control that floats over it. The cross is one step of the icon scale larger than a bare glyph (24 on the card, 32 in
    the field). The stop card's circle is the app's surface on the header band, the field's is the go circle's own pair, and the
    cross is 4.5 to 1 or better on its circle. A screen reader hears one button, Close with the stop's name or Clear.

## Design rules

### Home structure

| Layer | What | Rules |
|---|---|---|
| Map | Full screen, pins for nearby stops, the rider dot | Draws behind everything. Pins under the card stack are not tappable, so nothing the rider must reach sits there. |
| Top | KRAIL logo (left), Discover and Settings (right), nothing else | One centre line, 16 insets, solid, floating. The card stack never rises into it. |
| Card stack | Two trips, the Park & Ride row, the rest of the trips | Scrolls vertically inside its own height. The Park & Ride row scrolls sideways. |
| Chips | Set Home, Set Work, + Park & Ride | One wrapping flow of whole items above the pill, taj's small filled button in the fixed dark with a 48 touch target. Home and Work chips stay once set. The Park & Ride chip stays, so Add is always one tap. From about 650 wide they share the pill's row and the pill takes the rest. It never scrolls. |
| Notices | The location button and the Map offline button, only while one applies | Taj's extra small filled button in the soft theme tint with a hairline (rule 10), whole items in the chips' own flow, left aligned on the chips' edge (rule 24). |
| Pill | Taj's text field, read only, in the fixed dark, mic at the end | Bottom, in the thumb's reach. Shares the column's edges (rule 45). The only search bar home ever shows: no To and From block, no go circle, no swap button. |

### The card shell

- Saved trips and Park & Ride share one shell, copied from `SavedTripCard` and `ParkRideCard`.
- Background: a saved trip card is the theme colour wash (`themeBackgroundColor()`), 15% light and 45% dark. A card that floats on the map is opaque, because a translucent wash is unreadable over the light map: the stop card and the Options card are the soft tint laid over the page surface (rule 10), and the Park & Ride tiles and cards and the suggestion card are still the fixed dark with fixed light content.
- Padding 16, corner radius 16, 8 between cards in the stack.
- A card is a whole tap target. Minimum height 48.
- Saved trip: two stop names stacked, 12 apart. A labelled end follows rule 37.
- Park & Ride: the free spots as a large number with "spots" small beside it, and the car icon and station name under
  it, in the name type of the old Park & Ride card (`displaySmall`, normal weight), then the time of the oldest reading. The number is the total across the station's car parks. Expands in place to each car park with its count, percent full
  and time, each car park its own stacked block, wrapping whole items and never squeezing at any size or text scale.
- No white card wrapping a group of cards. No heading card.

### The home stack

- Trips are the app's card, in one column (rule 45).
- The Park & Ride row is one block the full width of the stack, after the second trip by default.
- Its tiles are 148 wide (scaling with text, never more than the width less 60) and all the same height.
- The row runs off the right screen edge, with the 16 inset at the left and at the end of the scroll, so the next tile is
  cut by the screen edge and there is no mask or gap.
- Tapping a tile expands it in place to the full row width: the station name, each car park's spots, percent full and time,
  and a Directions button on each car park that has a position of its own: taj's round icon button in the theme colour with a white turn right arrow, 48 across, which is the touch target. Tapping the name collapses it.
  One tile is open at a time and the others wait behind it.
- No heading sits above the trips or the row, and nothing takes the row's place when no station is saved.
- The rest of the trips follow the row and scroll vertically inside the stack.
- A Park & Ride tile is left-aligned like the trip cards. The number line ("124 spots") and the station line (car icon and
  name) share one left edge at 16 padding, 8 apart, with equal padding top and bottom and at least 16 on the right.

### Reorder mode

- Enter: long-press any card. That card lifts and the mode starts. Nothing else enters it.
- Chrome: the logo, gear, status pill, chips and the search pill all go. The top row holds one dark **Done** button at the
  right, 48 tall, 16 from the top and the right edge. The stack takes the freed height.
- Handles: each trip card shows a small grip bar centred on its top edge, and the Park & Ride row sits in an outlined block
  with its own wider grip. The whole card is the drag target.
- Drag: trips reorder among themselves, and the row drags as a block between trips. That is the only way to place the row.
- Done saves the order and returns to home.
- System back, a swipe back and a tap on the map cancel without saving: the order animates back and the chrome returns. No
  dialog, no toast.
- Backgrounding or killing the app mid-edit does not save either. A rotation keeps the edit.
- All local. Done is one save, the trips in one database transaction and the row's place in one preference, and cancel writes nothing. No network and no timers.

### Stop card

A tapped pin opens a floating stop card, never a bottom sheet.

- The card is two parts. The header is a band in the stronger tint step with the card's corner on its top: the stop name as
  the card's title (the app's `title`, 20 sp bold, the largest and strongest style on the card), the mode badges under
  it, and the close X at the end, a 40 circle in a 48 target (rule 47). The stop name may wrap inside the band without
  pushing the X out. The body is the soft tint: the Departure Board row (the section label, `titleMediumRegular`, 16 sp
  regular, so it never reads as the title at 100 or 200 percent) and the actions. No divider, the band is the separation,
  and the same 16 edge inset is in both parts, so the stop name, the board title, the rows and the buttons share one left
  edge. 8 between the blocks.
- The board heading holds the title and the chevron, and the whole row, at least 48 tall, opens and closes the board, not only
  the arrow. The board is closed until the heading is tapped, and only then is it fetched. Open, it lists every departure for
  every line, and the rider narrows it with the board's own filter, the pill and the per mode line groups above the rows. There
  is no Lines chip.
- The actions are pinned under the board, so the board scrolls inside the card and the action never leaves it. Save as pills, then
  the one primary action, taj's theme coloured `Button`, worded by what the rider is doing on Search Stop: Set as destination
  for the To field, Set as starting point for the From field, and Set as followed by the label's saved name exactly as saved
  (Set as Home, Set as Work, Set as Gym) when a stop is being chosen for a label. It is one line that ends in an ellipsis if too
  long, and a screen reader hears the words drawn. The words cross fade if the context changes with the card open. Hosts with no
  field to name, the home map, keep Go here (Start here in a start search) and Select stop. The pills are the app's own label
  pills, a label no stop carries outlined and worded "Save as Home", and the label this stop carries one filled pill that is not a
  control. A label sitting on another stop is not offered. The pills are whole items that wrap, 48 tall, and the row keeps the
  tallest room it has held so assigning a label never shortens the card. While a stop is being chosen for a label there is no
  Save as.
- Expanding the board hides the box at the top (the group card or the bare pill, and the location chip) with a slide up and a fade,
  and the card takes its room, up to the window less the top inset and a margin. The box is kept, not rebuilt. It returns when
  the board is collapsed or the card is dismissed. System back goes one step at a time: it shuts an expanded board, then dismisses
  the card, then is the screen's own two step back.
- The card slides up from the bottom with a fade as it arrives and slides back down as it is dismissed (see Motion).
- A Park & Ride pin shows the station's numbers instead of the board: the total and its time when the station has several car
  parks, then every car park stacked at equal weight, each with its own Directions button, then Add to home or Remove from
  home. Directions use the car park's own coordinates. A car park without them has no Directions button.
  Go here sits under Add to home, the primary action, when the station has a stop in the stops table, the smallest stop id the table holds, and a station
  with no stop has no Go here, so the card never promises a trip it cannot start (rule 40).
- The card floats at the bottom of the map in the one column of rule 45, in the soft tint of rule 10 (the Park & Ride pin card
  keeps the fixed dark for now), and closes with its close button, a tap on the map or system back.

### Search bar

- Home: dark inverted field, 48 tall, 16 in from the screen edge, shadow only (no outline). On Search Stop the bars are soft
  pills of taj's text field in the box's one mode aware surface (rule 10), with no shadow or outline of their own.
- Home: placeholder "Where are you going?" with the microphone at the end. The pill opens Search Stop. The microphone opens
  Ask KRAIL and shows only when the feature flag and the device allow it, never as a dead icon. The flag is read again each time home
  comes to the front.
- Search Stop: placeholder per field, no microphone (rules 7, 35 and 39).

### Search Stop

1. One box on top holding only the To and From pills, 8 apart. Under it, while the location is not allowed, the location
   status button, left aligned on the box's edge and one 8 gap below it. Map behind, clean, with no keyboard. At the bottom,
   the map's info icon alone at the bottom most left, with the Options button and the location control on one line over it (rule
   39), and the Options card above that line when it is open.
2. The recents are in the list state. A tap on a pill covers the map with the cloud gradient and shows the list under the same
   box (rule 9): the label row when any label is set, Select on map, the heading with Clear all, then recent stops, at most
   five rows. With location off and no recents, the suggested stops from the Intro show instead, each carrying its own stop id
   and name. Each row is the name and its mode badges with a chevron at the end, or the small label pill and no chevron for a
   labelled stop. With location on, a rider with no recents sees the nearest stops under Near you instead.
3. The list is replaced by results as the rider types.
4. Picking a stop fills its bar and the screen stays (rule 39). From fills itself with the nearest stop when location is
   known, and stays editable. With location off it asks "Choose start".
5. The bar reads To with the stop. The From row sits under it with the go circle inside it at the end (rules 26 and 36), shown
   as soon as both bars hold a stop. Tapping a bar edits it in place. With no start, From reads Choose starting point and there
   is no go circle. The go circle is the only way out with a trip, and back from the results returns here.
6. Tapping a pin opens a floating stop card: departures, Save as, and Set as destination or Set as starting point (rule 10, Stop card).

### The Search Stop box (S6, replaces the pills, the back arrow, the glow, the old go circle and the chip)

This block is in force where it differs from rules 8, 10, 24, 33, 35 and 36 above, which are marked replaced below.

- **The box is the Timetable's own From and To card.** One rounded card in the soft theme tint (`mapCardColors().tint`, the
  tint the Timetable card is, in light and dark mode), with two plain rows, From above To, and a 1 dp `outlineSubtle` divider
  between them. No pills, no second fill, no back arrow: system back does the two step back. The rows are still the editable
  fields (taj `TextField` with no container, fixed prompts Choose starting point and Choose destination, `onSurface` text,
  prompt and caret, because the placeholder token fails 4.5 on the dark tints). A single field stays the bare pill, with its
  back arrow, and is a card only when From and To both show.
- **Markers.** From is the 10 dp filled dot, To is the pin. A stop that has a saved label shows that label's own icon in place
  of the marker (`stopLabelIcon`, eight vectors chosen by label name; emoji are stripped from names, so a custom name with no
  icon keeps the dot or pin) and its row reads as the Timetable's does, the label in bold then the stop in brackets, with no
  helper text.
- **The grid, in dp at 411 wide and 100 percent text.** Card padding 16 (`spacingXL`), marker 20 (`iconSmall`), gap 8, text at 44,
  rows 56. The divider runs from the text start to 8 before the circle column, which is 64 from the card's right edge. When a
  name no longer fits beside its marker (320 dp, 200 percent) the marker wraps whole above it, the text and the divider start at
  16, the names wrap whole and the card grows. Nothing is truncated or squeezed. `ToFromCardGrid.kt` holds the numbers.
- **The right column.** Two 40 dp taj `RoundIconButton` circles on one axis, 16 from the card's right edge (the left padding), each
  centred on its row (8 above and below in a 56 row), each in a real 48 dp target (`CircleTargetButton`, the pattern of
  `CloseCircleButton`) so a tap never reaches the map. The column is reserved in every state, so the card is the same height
  with and without the go circle.
  - The swap is at the end of the From row: `softLabel` fill, `ic_reverse` in `getForegroundColor`, screen reader Swap From and To.
    A tap trades the stops (not the rows) and the glyph makes a half turn, 180 degrees in 300 ms, while the circle stays still.
    It is drawn once the rider has picked a stop, since an auto filled start alone is nothing to swap.
  - The go circle is at the end of the To row: the theme colour, the existing arrow in `getForegroundColor`, screen reader Show
    trips. It starts the trip as before, and is present only when both stops are chosen (absent, not disabled). It arrives
    growing to 1.2 by 180 ms and settling to 1.0 by 300 ms (one keyframes spec of two eases, not a spring) with an 80 ms fade, and
    leaves shrinking to nothing in 150 ms, taking no tap on the way out. While the row being edited holds text, an X takes that
    row's circle.
- **No glow anywhere in Search Stop.** The box, the bare pill, the Options card, the stop card and the notice wear a 1 dp
  `softLabel` hairline (`searchStopEdge`, through `mapChipEdge`) in light mode on the map (3.81 against the map ground), none on the
  dark map (the card is 5.9 to 10 against it), and the hairline in both modes on the gradient of the list state. The info tile
  keeps its glow on the home and in the timetable.
- **The location notice is the real info tile.** Under the box, one 8 gap down, left aligned and the box's width: the `InfoTile`
  in its new `Plain` style (the soft tint, the tile's 1 dp themed border, no shadow), expanded, titled Location is off, with
  Turn on location to see stops near you, the app's pin, Allow or Settings (what the old chip did on a tap) and the tile's own Not
  now. Not now hides it for the visit only (a `rememberSaveable` flag in the top block, never saved, not through the info tile
  manager). It comes and goes with the shared fade and size change, is not drawn in the phone list state, and takes every touch
  over it. Options stays hidden while location is not granted.
- **The info tile.** `InfoTileStyle.Plain` is new. Its actions row is a `FlowRow` now, so at 320 dp and 200 percent the call to
  action wraps whole under Not now instead of breaking in the middle of a word.

Cost. Layout and short animations only. No API calls, no payload, no timers, no battery. The one new piece of state is the
notice's `rememberSaveable` flag.

### Spacing

| What | Value |
|---|---|
| Scale | 4, 8, 12, 16 (24 only between sections) |
| Edge inset, all sides | 16 (top and bottom measured from the safe area) |
| Gap between floating elements | 8 (the chips and notices flow to the pill, card to card, stack to the chips and notices). On Search Stop: between the two pills in the box, the box to the location button, and the Options card to the bottom row |
| Search Stop edge | 16 from the system top inset to the box and from the box to the screen's sides |
| Card padding | 16 on all four sides, equal top and bottom, for the Options card, which takes the card's own radius token (16) and padding token (16). The box has no padding of its own: its rows are 56 tall with 16 at the start and the circle column at the end, so it is 113 tall at 100 percent text and grows at large text when a marker wraps above its name |
| Search Stop bottom two lines | The info icon alone at the bottom most left, resting on the library's 8 margin with no padding of ours, its i starting 1.75 in from the icon's 21 square. Above it the control line: the Options pill 4 above the i, the location control ending 16 in and centred on the pill, one centre line at every text size. Options has a 48 finger target that ends at the i's top. The Options card is 8 above the line's visible top. The 21, 8 and 1.75 are read from the library, not measured on a device, and are the numbers to check on a phone |
| List row | 14 above and below its content, 16 left, a 44 hit area for the plus |
| Search pill, go circle, Done | 48 tall or wide (the go circle inside the From bar is 40, rule 36). A chip or a notice has a 48 touch target |
| Corner radius of any pill, chip or circle | Half its height, always. A field is one line, so a pill never grows into a rounded rectangle |
| Status button | taj's extra small filled button with a 48 touch target, on home and on Search Stop. On Search Stop it is under the box, not in it. The Options button is the same button |

### Motion

- Screens change with the platform's standard navigation transition (rule 44). Nothing is choreographed.
- Saving a trip is on the timetable. Back on home shows the new card already in place.
- Inside Search Stop one motion spec, `MapMotion`, holds every number: an arrival takes 200 ms on a decelerating curve with a 150 ms
  fade, a departure 150 ms on an accelerating one, a label that changes in place cross fades in 150 ms, the Departure Board moves in
  300 ms, and the top box slides in 250 ms. Finite and short, never a loop. With reduced motion every one of them is plain: there or
  not, with no frame between.
- What moves: the Options card and button (fade and scale from the bottom start corner, 80 percent), the location control (to nothing
  about its own centre), both hiding while a stop card is open or the location is not allowed; the stop card (slides up with a fade,
  back down on dismiss); the stop card's action words (cross fade); the Departure Board (the height, the chevron and the rows' fade in
  step, the rows a touch after the height starts and gone before it ends, the loading, failed and rows phases cross fading); the top box
  (slides up and fades while the board is expanded, the card's room growing on the same curve); and the bare pill and the group card
  (a fade between them with a soft size change and no clip); the go circle (arrives 0 to 1.2 by 180 ms and settles to 1.0 by 300 ms with an 80 ms fade, leaves in 150 ms), the swap glyph (a half turn in 300 ms) and the location notice (the shared fade and size change).
- A card never animates its height because data arrived. Only a rider's tap expands a Park & Ride card or the Departure Board.
- Reduce motion: plain, no movement.

### Colour and type

- Theme colour is used in its four roles only (ground, ink, background wash, decor), as in `taj/THEME_COLOUR_ROLES.md`.
  Never draw a theme-colour ring around a control.
- Type is the app's: stop names `bodyLarge`, field text `titleLarge` normal weight, numbers `headlineMedium`. Nothing
  smaller than the app's `bodySmall`.

### States

- Every surface defines loading, empty, error, offline and content, all at the same height.
- Offline keeps the flat ground, pins and the rider dot, plus a Map offline chip.

### Accessibility

- 48 dp touch targets, labels on every icon button, focus order top to bottom.
- A screen reader reaches every stop the map shows through a list (the Near you rows).

## Cost

- Opening home adds no network call beyond the existing Park & Ride cooldown.
- Cooldown per facility: 120 s in the morning peak window (05:00 to 10:00) and 600 s outside it (Remote Config defaults). A
  facility inside its cooldown shows the cached number from the local database.
- Numbers on open cost at most one call per saved facility, only for facilities off cooldown, through the same cooldown path
  as the expanded cards. It is never a second unthrottled way to hit the API: the NSW API has no batch endpoint and its key
  is shared by every phone.
- No background polling. A poll runs only while a card is open and the screen is in front.
- Search on the shared map instance adds no requests while the view stays put. Stops, recents and results are local.
- Reorder mode is local, with no network and no timers.

## Language

### Words we use

| Where | Words |
|---|---|
| Home pill | Where are you going? |
| Search placeholders | Choose destination, Choose starting point, Search and select Home, Search and select Work, Search and select and the label's own name (when assigning a label). Fixed, never a stop name |
| Buttons | Change start, Show trips, Go here, Start here, Select stop, Choose start, Add to home, Not now |
| Save as pills | Save as Home, Save as Work, Save as and the label's own name |
| Chips | Set Home, Set Work, Park & Ride (with a plus) |
| Park & Ride | spots available, full, Last updated at |
| Section names | Recent, Near you |

### Rules for words

- Sentence case. No full stops on buttons, labels or placeholders.
- A button is a verb or a place. "Show trips", not "Submit".
- Stop names are written as the app writes them (Town Hall, Central Station). Never shortened to fit.
- "Park & Ride" with the ampersand everywhere.
- A number always has its unit beside it, in smaller type, on the same baseline: 124 spots.
- No tips, status lines or explainers beside a heading or an item. The item shows its own content only.
- No marketing words and no exclamation marks.

### Names for parts

| Name | Meaning |
|---|---|
| Home pill | The dark bar at the bottom of home |
| Search pill | One of the two soft pills in the box on top of Search Stop |
| Card stack | The scrolling list of saved trip and Park & Ride cards on home |
| Saved trip card | A saved trip, as the app draws it |
| Park & Ride card | A saved station, with its live numbers |
| Recents list | The vertical list of recent stops in Search Stop's list state |
| Results list | Matches while typing |
| Stop card | What a tapped pin shows |
| Chip | Set Home, Set Work and Park & Ride, in one row above the pill |

## Do

- Read the real composable and tokens, then copy them.
- Check 320 dp at 200% text first, then the other sizes.
- Put the search pill in the thumb's reach, at the bottom.
- Let the map show through, around and between cards.
- Stack or wrap whole items when space is short.
- Show cached numbers at once with their time, and refresh within the cooldown.
- Keep every gap on the 4-step scale: 16 at the edge, 8 between things, 16 inside a card.
- Keep the same card for the same kind of thing.
- Share one centre line and one inset across the top row.
- Give a placeholder the same line box as its content.
- Keep a way to reach every stop without the map (Near you).
- Label a guess as a guess, and measure the rest.

## Don't

- No bottom sheet, handle, snap points or docked panel.
- No white box around a group of cards.
- No list, recents or near you on the map at open.
- No horizontal list that holds content the rider must find (the exceptions to rule 4 are the only ones).
- No empty-state card that explains what to do.
- No element that moves because data loaded or failed.
- No card stretched wide around short content.
- No theme-colour ring or border on the search bar, and no glow anywhere in Search Stop: the box's edge is the 1 dp `softLabel` hairline in light mode.
- No second field, no mic on Search Stop.
- No text broken beside a sibling, no clipping, no truncating, no shrinking. The one exception is a single-line text field,
  which ends in an ellipsis.
- No hiding a control or shortening a name to dodge a wrap.
- No helper text, taglines, counts or eyebrows.
- No invented components.
- No new polling, no per-feature HTTP client, no new event names.
- No toast, snackbar, banner or any notice laid over content, ever. It hides what the rider is looking at. The change itself
  is the confirmation (the chip becomes Home, the card shows its label, the tile appears). A message that is truly needed
  sits in the flow and pushes content down.
