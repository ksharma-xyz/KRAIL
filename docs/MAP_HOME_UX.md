# Map-first home: design rules

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

6. **One pill search bar.** On home and on Search Stop. Never two fields stacked.
7. **The microphone is on the home pill only.** Never on Search Stop.
8. **Search Stop always opens on the map, with no keyboard.** The rider taps the bar to raise the keyboard.
9. **Recents float on the map under the search bar on Search Stop,** as a vertical card: one row per stop with its mode
   badges and a plus at the end, the same card the results use. A row of labels (Home, Work, then custom labels) scrolls
   sideways at the top of the card.
10. **The search bar on the map is the app's own dark field.** `TextFieldDefaults.invertedColors()`: dark container, light
    text, 48 dp tall, full pill. No theme-colour ring or border, in every theme.

### Components

11. **Copy the real component.** Open the composable and its tokens before drawing a card, row, field or button:
    `SavedTripCard`, `ParkRideCard`, `TextField`, `SearchTopBar`. Never invent one.
12. **The KRAIL logo floats top left and Settings top right,** solid, the same on iOS and Android, no glass. Discover sits beside
    Settings, to its left, when it is available.

### Space

13. **When space is short, stack in a column or wrap whole items.** A name, label, "to", badge, icon or button is one item.
    Text never breaks across lines beside a sibling. Nothing is clipped, shrunk or truncated to fit, with one exception: a
    single-line text field ends in an ellipsis (rule 34). Do not shorten copy or hide a control to dodge a wrap.
14. **Every form factor, always.** Phone, 320 dp at 200% text, phone landscape, foldable open, iPad portrait and landscape,
    dual screen, split view. Wide screens keep the split of list on the left and map on the right.

### Cost

15. Replaced by rule 45.
16. **Opening the app buys no new network traffic** beyond the existing Park & Ride cooldown. No background polling. Live
    numbers are fetched while the screen is in front only.
17. **Stops, recents, results and the Park & Ride cache are local.** The map tiles are the only new traffic and they come
    from the shared map instance, drawn once.

### Copy

18. Reserved.
19. **No helper text next to headings or items.** No taglines, counts, eyebrows or captions. This includes empty states:
    with no saved trips there is no "Your trips, plan a trip" card, only the map.

### Geometry and stability

20. **One geometry.** The logo and Settings share one horizontal centre line, with equal 16 insets left and right, and the
    notices row (the location pill and the Map offline chip) sits directly under them, centred on the screen. Heights may
    differ; centres do not. Every size, gap and inset is
    a multiple of 4. There is no locate button: the blue location dot on the map does that job, keeps its tap and has a
    screen-reader label.
21. **Chips and the search pill are the same height, 48,** which is also the touch target.
22. **Nothing jumps as data arrives.** A card has the same height whether its numbers are loading, loaded or failed, and
    cards in one row have the same height. At large text a card may grow, and then it grows the same way in every state.
    Placeholders use the same line box as the content they stand in for.
23. Replaced by rule 44.

### Exceptions to rule 4

- At text over 130% only, the row of chips (Set Home, Set Work, Park & Ride) may scroll sideways with a fade at the right
  edge, because stacked chips take too much height. The first chip is always fully visible.
- The Park & Ride row on home is one horizontal row of cards, because stations are short cards and a row keeps them in a
  fixed place instead of pushing them down a long list of trips. The next card always peeks, the map's P pins are another
  way to reach any station, and a station expands in place.
- The label row at the top of the Search Stop recents card (Home, Work and any custom label) is one row that scrolls sideways.
- Nothing else is allowed to scroll sideways.

### Forms, cards and behaviour

24. **Location needs one pill.** When location permission is not granted, one status pill sits directly under the whole form
    on Search Stop (under the bar, or under the From row once a destination is picked), always, centred, above the recents
    or the results, which move down by one pill and one gap. Tapping it asks for permission, or opens Settings if refused.
    On home it sits directly under the top row, centred. "Map offline" is the same pill. If both apply they are two whole items
    in one wrapping row, side by side when there is room and one under the other when there is not.
25. Replaced by rule 36.
26. **The go circle has one state.** It is a plain arrow. The next screen owns loading, success and error, so this screen
    never shows a trips error. The chips have no chevron: they are not dropdowns.
27. **The spacing scale is 4, 8, 12, 16.** Edge inset 16 on every side. Gap between floating elements 8. Card padding 16 on
    all four sides, and padding inside any card is equal top and bottom: a stretched card centres its content, a list card
    uses fixed row padding, never fixed row heights.
28. **Chips share the pill's row** when the row is wide enough (about 650 and up) and otherwise sit above it, so a wide
    screen never shows a short row of chips beside empty space.
29. **Home order: two trips, one Park & Ride row, then the rest.** The top two saved trips come first as the app's normal
    cards, then one horizontal row of Park & Ride cards (big number, station name small), then any further trips. With one
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
33. **Two surface colours in any group of floating controls.** In the To and From block they are near-black and white: the
    field being edited, the go circle and Done are near-black, the other field and the status pill are white. Orange is not
    used there. The count of distinct backgrounds is checked in light and dark.
34. **A text field is one line, 48 tall, and ends in an ellipsis.** This covers the home pill, the search pill, the To value
    and the From value. The accessibility label carries the full text. Only single-line fields may truncate; chips, cards,
    badges and rows still wrap or stack whole items.
35. **The search pill has a back arrow and an X.** Back is a 48 box at the left, like today's search bar. The X shows only
    when the field has content and only clears it, and never dismisses the screen. Back follows rule 39, except from the
    start field, where it returns to To and From with the destination kept (on the bar that still needs a stop). System back and the iOS swipe do the same. In To
    and From the X clears the destination and raises the keyboard. The From row has a matching 48 column with a filled origin
    dot so To and From values start at one x.
36. **To is on top, From is below, and neither row ever moves.** While To is being edited, From shows under it when the
    rider's location is known. Tapping From edits it in place: From becomes the dark text field and To becomes the light
    button, and no row changes position. The dark fill never slides between rows, because the bar being edited is the dark
    one. The back arrow stays on the To row and the origin dot on the From row in every state. Both rows are the same width and
    share both edges. The go circle is 40, inside the From bar with 4 around it, in the same column as the clear X above it,
    and has one state. The block has two surface colours in light and dark.
37. **Every end of a saved trip follows one rule.** A labelled end is the label in bold with its stop small under it. An
    unlabelled end is its stop name. When both ends are labelled the card keeps the app's one-line form, "Home to Work",
    with no stop names. A label shows on a card whenever that card's stop is the labelled stop.
38. **Delete is the app's own delete circle.** In reorder mode a trip card carries `TripDeleteOverlay`: a 24 circle in the
    ink colour with a white X (12), at the card's top-right corner, overhanging it by 6, with a 48 hit area. Stations do not
    carry one and do not reorder one by one. The Park & Ride row is one outlined block with a grip that moves up or down as
    a unit. A station is removed on the Add Park & Ride screen with its toggle, or from its P card, which reads "Remove from
    home" when the station is on home and "Add to home" when it is not. Done saves, back or a tap on the map cancels.
39. **Search Stop has two states and no map button.** It opens as the map state: the map and one dark pill, no list and no
    keyboard. Tapping the bar is the list state, with the keyboard and the recents. X clears the text and keeps the recents.
    Back, or a tap on the map, returns to the map state, and back again goes home. System back is a two-step exit.
    **A pick never leaves the screen.** On the To and From form every pick (a recents row, a label shortcut, a result, a stop
    card action) fills its bar and the screen stays. Editing moves to the other bar while that bar is empty, with the keyboard
    following it, and to nothing when both hold a stop. Only the go circle leaves, with both stops, and back leaves with nothing,
    so a half pick is dropped. A label, or one leg of a trip, is a single field and returns at once with its stop.
40. **A Park & Ride card never promises a trip it cannot start.**
41. **Home and Work chips stay once set.** Tapping Home plans a trip from the nearest stop to Home with no saved trip
    needed. There is no long press on a chip. The app's own Manage stop labels screen (expand a row: Save changes, Remove,
    Delete) opens from the Manage button on the label row in Search Stop. There is no floating menu. The chips sit above
    the pill, in the same column as the cards. Each chip is taj's chip sized button: a set label is the filled one and an
    unset label and Park & Ride are the outlined one.
42. **Nothing is ever laid over content.** No toast, snackbar, banner or floating menu. The result of an action is shown by
    the screen changing. Where a message is needed, it takes a place in the layout and everything else moves to make room.
43. **Icons and behaviours are the app's own.** Find the app's version of a control before drawing it. A new icon, menu or
    pattern needs an explicit yes.
44. **No custom transitions.** Screens change with the platform's standard navigation transition only. No pill that rises
    while cards fade, no card that grows into its screen.
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

## Design rules

### Home structure

| Layer | What | Rules |
|---|---|---|
| Map | Full screen, pins for nearby stops, the rider dot | Draws behind everything. Pins under the card stack are not tappable, so nothing the rider must reach sits there. |
| Top | KRAIL logo (left), Discover and Settings (right), the notices under them: the location pill and the Map offline chip, centred | One centre line, 16 insets, solid, floating. The card stack never rises into it. |
| Card stack | Two trips, the Park & Ride row, the rest of the trips | Scrolls vertically inside its own height. The Park & Ride row scrolls sideways. |
| Chips | Set Home, Set Work, + Park & Ride | One wrapping row above the pill, 48 tall. Home and Work chips stay once set. The Park & Ride chip stays, so Add is always one tap. From about 650 wide they share the pill's row and the pill takes the rest. At text over 130% the row scrolls sideways. |
| Pill | Dark search bar, mic at the end | Bottom, in the thumb's reach. Shares the column's edges (rule 45). |

### The card shell

- Saved trips and Park & Ride share one shell, copied from `SavedTripCard` and `ParkRideCard`.
- Background: the theme colour wash (`themeBackgroundColor()`), 15% light and 45% dark.
- Padding 16, corner radius 16, 8 between cards in the stack.
- A card is a whole tap target. Minimum height 48.
- Saved trip: two stop names stacked, 12 apart. A labelled end follows rule 37.
- Park & Ride: the free spots as a large number with "spots" small beside it, and the car icon and station name small under
  it, then the time of the oldest reading. The number is the total across the station's car parks. Expands in place to each car park with its count, percent full
  and time, each car park its own stacked block, wrapping whole items and never squeezing at any size or text scale.
- No white card wrapping a group of cards. No heading card.

### The home stack

- Trips are the app's card, in one column (rule 45).
- The Park & Ride row is one block the full width of the stack, after the second trip by default.
- Its tiles are 148 wide (scaling with text, never more than the width less 60) and all the same height.
- The row runs off the right screen edge, with the 16 inset at the left and at the end of the scroll, so the next tile is
  cut by the screen edge and there is no mask or gap.
- Tapping a tile expands it in place to the full row width: the station name, each car park's spots, percent full and time,
  and a Directions button (48 round, outline) on each car park that has a position of its own. Tapping the name collapses it.
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

- Line 1 is the stop name with a 48 close button at the right. A card closes, a field does not.
- Line 2 is the mode badges, left.
- Then the Departure Board in its own light card.
- Padding 16 all round, 8 between the three blocks.
- The board heading holds the title, one Lines chip (only when the stop has more than one line) and the chevron. The board
  is closed until the chevron is tapped, and only then is it fetched. The chip opens a row of the stop's line badges, and a
  badge filters the board to its line.
- Buttons at the bottom: Save as pills, then Go here or Start here. The pills are the app's own label pills, a label no stop
  carries outlined and worded "Save as Home", and the label this stop carries one filled pill that is not a control. A label
  sitting on another stop is not offered. The pills are whole items that wrap, 48 tall, and the row keeps the tallest room it
  has held so assigning a label never shortens the card. The action reads Go here when choosing a destination and Start here in
  a start search. While a stop is being chosen for a label it stays Select stop and there is no Save as.
- A Park & Ride pin shows the station's numbers instead of the board: the total and its time when the station has several car
  parks, then every car park stacked at equal weight, each with its own Directions button, then Add to home or Remove from
  home. Directions use the car park's own coordinates. A car park without them has no Directions button.
  Go here sits under Add to home, the primary action, when the station has a stop in the stops table, the smallest stop id the table holds, and a station
  with no stop has no Go here, so the card never promises a trip it cannot start (rule 40).
- The card floats at the bottom of the map in the one column of rule 45, on a solid surface under the card wash, and closes
  with its close button, a tap on the map or system back.

### Search bar

- Dark inverted field, 48 tall, 16 in from the screen edge, shadow only (no outline).
- Home: placeholder "Where are you going?" with the microphone at the end. The pill opens Search Stop. The microphone opens
  Ask KRAIL and shows only when the feature flag and the device allow it, never as a dead icon.
- Search Stop: placeholder per field, no microphone (rules 7, 35 and 39).

### Search Stop

1. Bar on top. Map behind. No keyboard.
2. Recents are a vertical list in a floating card under the bar, the same card as the results. The label row comes first
   when any label is set (one row that scrolls sideways), then recent stops, at most five rows. With location off and no
   recents, the curated stops from the Intro's suggested stops list show instead, each carrying its own stop id and name. Each row is a whole item: the name, its mode badges and a plus at the end. With
   location on, a rider with no recents sees the nearest stops instead.
3. Tapping the bar raises the keyboard. The recents list is replaced by results as the rider types.
4. Picking a stop fills its bar and the screen stays (rule 39). From fills itself with the nearest stop when location is
   known, and stays editable. With location off it asks "Choose start".
5. The bar reads To with the stop. The From row sits under it with the go circle inside it at the end (rules 26 and 36), shown
   as soon as both bars hold a stop. Tapping a bar edits it in place. With no start, From reads Choose starting point and there
   is no go circle. The go circle is the only way out with a trip.
6. Tapping a pin opens a floating stop card: departures, Save as, Go here or Start here.

### Spacing

| What | Value |
|---|---|
| Scale | 4, 8, 12, 16 (24 only between sections) |
| Edge inset, all sides | 16 (top and bottom measured from the safe area) |
| Gap between floating elements | 8 (pill to status pill to From row to list, card to card, chips to pill, stack to chips) |
| Card padding | 16 on all four sides, equal top and bottom |
| List row | 14 above and below its content, 16 left, a 44 hit area for the plus |
| Search pill, chips, go circle, Done | 48 tall or wide (the go circle inside the From bar is 40, rule 36) |
| Corner radius of any pill, chip or circle | Half its height, always. A field is one line, so a pill never grows into a rounded rectangle |
| Status pill | 32 tall with a 48 hit area |

### Motion

- Screens change with the platform's standard navigation transition (rule 44). Nothing is choreographed.
- Saving a trip is on the timetable. Back on home shows the new card already in place.
- A card never animates its height because data arrived. Only a rider's tap expands a Park & Ride card.
- Reduce motion: a plain cross-fade, no movement.

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
| Search placeholders | Choose destination, Choose starting point, Choose a stop (when assigning a label) |
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
| Search pill | The same bar on top of Search Stop |
| Card stack | The scrolling list of saved trip and Park & Ride cards on home |
| Saved trip card | A saved trip, as the app draws it |
| Park & Ride card | A saved station, with its live numbers |
| Recents list | The vertical list of recent stops on Search Stop |
| Results card | Matches while typing |
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
- No horizontal list that holds content the rider must find (the exceptions to rule 4 are the only ones).
- No empty-state card that explains what to do.
- No element that moves because data loaded or failed.
- No card stretched wide around short content.
- No theme-colour ring or border on the search bar.
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
