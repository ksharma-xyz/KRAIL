# Ask KRAIL — manual test cases

Things a device shows and a unit test cannot. The rules being checked are in `ASK_KRAIL_UX.md`;
this is the list to actually try.

Ordered so a full pass is one sitting. Most of these were real defects, which is why they are
worth re-running rather than trusted.

## Setup

Have at least: a **Home** label with a stop, a **Work** label with a stop, and one saved trip.
A few cases need the labels cleared — those say so.

---

## 1. Typing a whole journey

| # | Say / type | Expect |
|---|---|---|
| 1.1 | `home to work` | Both fields fill. No time chip |
| 1.2 | `home to work by 9am` | Both fields fill, chip reads the arrive-by time |
| 1.3 | `office by Monday morning` | Work fills from the **office** synonym, chip says Monday |
| 1.4 | `get me home` | Destination fills; origin comes from where you are (see §5) |
| 1.5 | `hey how are you` | Nothing fills. No field is guessed at |
| 1.6 | `Hogwarts to work` | Quotes *Hogwarts* back as unmatched. Work still fills |
| 1.7 | `homebush to central` | Homebush the **stop**, not your Home label |

## 2. The keyboard and the field

| # | Do | Expect |
|---|---|---|
| 2.1 | Open the screen | Keyboard is already up, caret in the field |
| 2.2 | Press enter | A **new line**, not a search |
| 2.3 | Type until it wraps to 3+ lines | Field grows, bar stays above the keyboard, nothing jumps |
| 2.4 | Clear the field | Send button springs away. Suggestion does **not** come back |
| 2.5 | Rotate with text in the field | Text survives, layout intact |
| 2.6 | Rotate while the model is working | No crash, no duplicate submit |

## 3. Speaking

| # | Do | Expect |
|---|---|---|
| 3.1 | Tap mic, say a trip | Words appear **as you speak**, not all at the end |
| 3.2 | Finish speaking | Field keeps the text. **Nothing submits.** Send is waiting |
| 3.3 | Tap send after speaking | Same result as if you had typed it |
| 3.4 | Tap mic, say nothing | Stops itself at ~10s, no hang |
| 3.5 | Still talking at 10s | Gets extended, cuts at ~15s |
| 3.6 | Tap the stop button mid-sentence | Stops immediately, keeps what it heard |
| 3.7 | Tap mic twice quickly | No "recogniser busy" error surfacing as a permission problem |
| 3.8 | Deny mic permission, tap mic | Says why. Blocked twice → offers Settings |

## 4. The suggestion line

Change the device clock to check these; the bands are Sydney time.

| # | When | Expect |
|---|---|---|
| 4.1 | Weekday 07:00 | `Try "Home to Work by 9am"` |
| 4.2 | Weekday 09:30 | **No** "by 9am" — that hour has passed |
| 4.3 | Weekday 17:00 | Runs the other way: `Work to Home after 6pm` |
| 4.4 | Weekday 19:00 | `Get me home by 9pm` |
| 4.5 | Saturday 11:00, only commute data | `Get me home …` — **never** Central to Parramatta |
| 4.6 | Saturday 11:00, with a Gym label | `Home to Gym …` |
| 4.7 | **Sunday 16:00** | `Home to Work by 9am tomorrow` |
| 4.8 | Any day 22:00 | Relative time, no hour that has passed |
| 4.9 | Fresh install, no labels | A real journey, never a half-filled one |

## 5. Where the journey starts

| # | Do | Expect |
|---|---|---|
| 5.1 | Stand near your Work stop, say `get me home` | Origin is **Work**, not the nearest bus shelter |
| 5.2 | Stand at Home, say `get me home` | Does **not** resolve Home → Home |
| 5.3 | More than ~1km from any label | Falls back to the nearest stop |
| 5.4 | Location permission off | Origin left blank and editable. **No permission prompt** |

## 6. Look and layout

| # | Do | Expect |
|---|---|---|
| 6.1 | Light mode | Bar is **white**, clearly on top of the colour |
| 6.2 | Dark mode | Bar is darker than the background |
| 6.3 | Watch the background for ~30s | Colours drift. Two hues, not one |
| 6.4 | Switch theme (Train / Metro / Bus …) | Wheel, border and background all follow it |
| 6.5 | Font size to largest | **No** second "Speak" button. Mic still tappable |
| 6.6 | Keyboard open | Colour rises with the bar, brightest under it |
| 6.7 | While it is working | Border animates; the text stays readable, never grey |

## 7. Errors

| # | Do | Expect |
|---|---|---|
| 7.1 | Trigger any failure | Banner is readable in **both** themes |
| 7.2 | Start typing after a failure | Banner clears as you edit |
| 7.3 | Aeroplane mode, submit | Fails honestly, no invented result |

## 8. Getting out

| # | Do | Expect |
|---|---|---|
| 8.1 | Back button | Closes, returns to the home screen |
| 8.2 | Back while listening | Mic stops. Nothing submits later |
| 8.3 | Close and reopen | Suggestion is back, field is empty |

---

## Known gaps

Not defects — deliberately not built. See §8 of `ASK_KRAIL_UX.md`.

- Saying a label word with **no** stop pinned (say `office` with Work unset) gives a generic
  failure rather than offering to set it.
- The model still occasionally swallows a day into a place (`10am Monday work`). Labels are not
  yet passed to it as segmentation hints.
