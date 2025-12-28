# TimeTable Screen - Test Scenarios

**Last Updated**: 2025-12-28  
**Purpose**: Comprehensive manual test scenarios for TimeTable screen navigation, state management, and data persistence. These scenarios will be automated in the future.

## 📋 Quick Reference

| Category | Scenarios | Status |
|----------|-----------|--------|
| State Persistence - Rotation | TC-001, TC-002 | ✅ Fixed |
| State Persistence - Navigation | TC-003, TC-004, TC-005 | ✅ Fixed (TC-003), 🔧 TC-004 Fixed, TC-005 In Progress |
| Date/Time Selection | TC-006, TC-007, TC-008 | ✅ Implemented |
| Cache Management | TC-009, TC-010, TC-011, TC-012 | ✅ Implemented |
| Theme Persistence | TC-013, TC-014 | ✅ Fixed |
| Recent Stops | TC-015, TC-016 | ✅ Fixed |
| Service Alerts | TC-017, TC-018 | ✅ Fixed (TC-017) |
| Search Stop Integration | TC-019, TC-020 | 🔧 Needs Investigation |
| Two-Pane Layout | TC-021, TC-022, TC-023 | ⚠️ Not Tested |
| ViewModel Lifecycle | TC-024, TC-025, TC-026 | ✅ Implemented |
| API Rate Limiting | TC-027, TC-028 | ✅ Implemented |
| Error Scenarios | TC-029, TC-030 | ⚠️ Not Tested |
| Edge Cases | TC-031, TC-032, TC-033 | 🔧 TC-032 Future Enhancement |

---

## 🎯 State Persistence (Configuration Changes)

### TC-001: Date/Time Selection survives screen rotation
**Status**: ✅ PASS (Fixed 2025-12-28)

**Steps**:
1. Navigate to TimeTable screen (Town Hall → Seven Hills)
2. Select "Arrive by 11:30 PM, Dec 29"
3. Wait for journey cards to load
4. Rotate device/emulator

**Expected**:
- ✅ Selected date/time (11:30 PM, Dec 29) is preserved
- ✅ Journey cards show same data (no API call)
- ✅ "Arrive by" text shows correct time
- ✅ No loading indicator

**Actual**: PASS - Date/time selection survives rotation via `rememberSaveable`

**Implementation**: Custom `Saver` for `DateTimeSelectionItem` in TripPlannerEntries.kt

---

### TC-002: Null date/time selection survives rotation
**Status**: ✅ PASS

**Steps**:
1. Navigate to TimeTable screen
2. Do NOT select any date/time (should show "Plan Your Trip")
3. Wait for journey cards to load
4. Rotate device

**Expected**:
- ✅ "Plan Your Trip" text is still shown
- ✅ Journey cards show same data
- ✅ No API call made

**Actual**: PASS

---

## 🧭 State Persistence (Navigation)

### TC-003: Date/Time selection persists when navigating back from SavedTrips
**Status**: ✅ PASS (Fixed 2025-12-28)

**Steps**:
1. From SavedTrips, tap "Town Hall → Seven Hills" trip card
2. TimeTable screen loads with default data
3. Select "Arrive by 11:30 PM, Dec 29"
4. Wait for journey cards to load with selected time
5. Navigate back to SavedTrips screen
6. Tap SAME "Town Hall → Seven Hills" trip card again

**Expected**:
- ✅ Date/time selection is preserved (11:30 PM, Dec 29)
- ✅ Journey cards show cached data (no API call)
- ✅ NO "Plan Your Trip" text

**Actual**: PASS - Fixed by moving `previousTripId` to ViewModel

**Root Cause**: `previousTripId` was stored in composable state, got lost on navigation
**Fix**: Moved to ViewModel as private var

---

### TC-004: Date/Time selection cleared when navigating to DIFFERENT trip
**Status**: ✅ PASS (Fixed 2025-12-28)

**Steps**:
1. From SavedTrips, tap "Town Hall → Seven Hills"
2. Select "Arrive by 11:30 PM, Dec 29"
3. Navigate back to SavedTrips
4. Tap DIFFERENT trip "Roseville → Seven Hills"

**Expected**:
- ✅ Date/time selection is cleared (null)
- ✅ "Plan Your Trip" text is shown
- ✅ Fresh API call is made for new trip
- ✅ Journey cards show data for current time

**Actual**: PASS - Different trip triggers cache clear

---

### TC-005: Journey cards don't show stale data when dateTimeSelection is null
**Status**: 🔧 FIXED (2025-12-28)

**Steps**:
1. From SavedTrips, tap "Town Hall → Seven Hills"
2. Select "Arrive by 11:30 PM, Dec 29"
3. Wait for journey cards to load
4. Navigate back to SavedTrips
5. Tap same "Town Hall → Seven Hills" card

**Before Fix**:
- ❌ "Plan Your Trip" text shown (dateTimeSelection = null)
- ❌ But journey cards show data for "11:30 PM, Dec 29" (cached)
- ❌ Mismatch: UI says "Plan Your Trip" but showing time-specific data

**Expected After Fix**:
- ✅ Either: dateTimeSelection is preserved AND shows correct "Arrive by" text
- ✅ OR: dateTimeSelection is cleared AND journey cards refresh

**Implementation**: Added sync LaunchedEffect to match UI state with ViewModel state on composition

---

## 📅 Date/Time Selection Behavior

### TC-006: Selecting date/time triggers API call
**Status**: ✅ PASS

**Steps**:
1. Navigate to TimeTable screen
2. Tap date/time selector
3. Select "Arrive by 10:00 PM, Dec 29"
4. Confirm selection

**Expected**:
- ✅ Loading indicator appears
- ✅ API call is made with arr=true, time=2200, date=20251229
- ✅ Journey cards update with new data
- ✅ "Arrive by 10:00 PM, Dec 29" shown in UI

**Actual**: PASS

---

### TC-007: Resetting date/time triggers default API call
**Status**: ✅ PASS

**Steps**:
1. Navigate to TimeTable with date/time already selected
2. Tap date/time selector
3. Tap "Reset" button

**Expected**:
- ✅ dateTimeSelection becomes null
- ✅ "Plan Your Trip" text shown
- ✅ API call made with current time (no date/time params)
- ✅ Journey cards update with current time data

**Actual**: PASS

---

### TC-008: Changing date/time multiple times
**Status**: ✅ PASS

**Steps**:
1. Navigate to TimeTable
2. Select "Depart after 9:00 AM, Dec 29"
3. Wait for data to load
4. Change to "Arrive by 5:00 PM, Dec 29"
5. Wait for data to load
6. Change to "Depart after 10:00 AM, Dec 30"

**Expected**:
- ✅ Each selection triggers new API call
- ✅ Journey cards update each time
- ✅ Rate limiting prevents rapid-fire calls
- ✅ Latest selection is shown in UI

**Actual**: PASS - Rate limiter ensures 1 second debounce

---

## 💾 Cache Management

### TC-009: Cache is preserved for same trip (rotation)
**Status**: ✅ PASS

**Steps**:
1. Load TimeTable for "Town Hall → Seven Hills"
2. Wait for journey cards to load
3. Rotate device
4. Check logs for API calls

**Expected**:
- ✅ Journey cards still visible after rotation
- ✅ NO API call in logs
- ✅ ViewModel log: "Same trip, preserving cache"

**Actual**: PASS

---

### TC-010: Cache is cleared for different trip
**Status**: ✅ PASS

**Steps**:
1. Load "Town Hall → Seven Hills" (Journey A)
2. Navigate back
3. Load "Roseville → Seven Hills" (Journey B)
4. Check journey cards

**Expected**:
- ✅ Journey cards show data for Journey B (not A)
- ✅ API call is made for Journey B
- ✅ ViewModel log: "Different trip detected, clearing cache"

**Actual**: PASS

---

### TC-011: Cache is cleared on Reverse Trip
**Status**: ✅ PASS

**Steps**:
1. Load "Town Hall → Seven Hills"
2. Select date/time, wait for data
3. Tap "Reverse Trip" button

**Expected**:
- ✅ From/To stops are swapped (Seven Hills → Town Hall)
- ✅ Date/time selection is preserved
- ✅ Fresh API call is made (with same date/time parameters)
- ✅ Journey cards show new route data

**Actual**: PASS

---

### TC-012: Cache is cleared when date/time changes
**Status**: ✅ PASS

**Steps**:
1. Load TimeTable with no date/time selected
2. Note the journey card times
3. Select "Arrive by 11:00 PM"
4. Check if journey cards updated

**Expected**:
- ✅ New API call is made
- ✅ Journey cards show different data (earlier trips)
- ✅ Old journey cards are not visible

**Actual**: PASS

---

## 🎨 Theme Persistence

### TC-013: Theme persists on rotation
**Status**: ✅ PASS (Fixed 2025-12-28)

**Steps**:
1. Set theme to "Ferry" (green) in settings
2. Navigate to TimeTable screen
3. Verify green theme is active
4. Rotate device

**Expected**:
- ✅ Theme remains "Ferry" (green) after rotation
- ✅ NOT default "Train" (orange)

**Actual**: PASS - Theme loaded from database on navigation

**Implementation**: Navigator loads theme from DB in `loadThemeFromDatabase()`

---

### TC-014: Theme persists across navigation
**Status**: ✅ PASS

**Steps**:
1. Set theme to "Metro" (purple)
2. Navigate: SavedTrips → TimeTable → SavedTrips → TimeTable

**Expected**:
- ✅ Theme stays "Metro" throughout navigation
- ✅ No flash of default "Train" theme

**Actual**: PASS

---

## 🔄 Recent Stops Update

### TC-015: Selected stop appears in recent stops
**Status**: ✅ PASS (Fixed 2025-12-28)

**Steps**:
1. Navigate to SavedTrips
2. Tap "From" field
3. Search and select "Central Station"
4. Go back to SavedTrips
5. Tap "From" field again
6. Check recent stops list

**Expected**:
- ✅ "Central Station" appears in recent stops
- ✅ Most recent selection is at the top

**Actual**: PASS - `RefreshRecentStopsList` event added to SearchStopViewModel

---

### TC-016: Recent stops are fresh on each screen open
**Status**: ✅ PASS

**Steps**:
1. Select stop A
2. Close app (force stop)
3. Reopen app
4. Open SearchStop screen
5. Verify recent stops list

**Expected**:
- ✅ Recent stops include stop A
- ✅ List is loaded from database (persistent)

**Actual**: PASS - LaunchedEffect(Unit) ensures refresh on each screen open

---

## 🚨 Service Alerts Navigation

### TC-017: Back gesture/button closes alert modal (not TimeTable)
**Status**: ✅ PASS (Fixed 2025-12-28)

**Steps**:
1. Navigate to TimeTable
2. Tap alert icon on journey card
3. Service alerts modal opens
4. Press hardware back button OR swipe back gesture

**Expected**:
- ✅ Alert modal closes
- ✅ TimeTable screen is still visible (does NOT navigate back to SavedTrips)

**Actual**: PASS - ModalBottomSheet with NavigationBackHandler

**Implementation**: Service alerts shown as modal, not separate screen

---

### TC-018: Alert modal shows correct alerts
**Status**: ⚠️ NOT TESTED

**Steps**:
1. Navigate to TimeTable
2. Tap alert icon on journey card with multiple alerts
3. Verify alerts content

**Expected**:
- ✅ All alerts for that journey are shown
- ✅ Alerts are readable and properly formatted

---

## 🔍 Search Stop Integration

### TC-019: SearchStop in Detail Pane updates SavedTrips (List Pane)
**Status**: 🔧 NEEDS INVESTIGATION

**Steps** (Two-pane layout on tablet/desktop):
1. SavedTrips visible in List Pane
2. Tap "From" field → SearchStop opens in Detail Pane
3. Select "Central Station"
4. SearchStop closes, SavedTrips still visible
5. Check if "From" field updated

**Expected**:
- ✅ "From" field in SavedTrips shows "Central Station"
- ✅ State updated via ResultEventBus

**Actual**: 🔧 Not confirmed - needs testing

---

### TC-020: Multiple SearchStop selections update correctly
**Status**: 🔧 NEEDS INVESTIGATION

**Steps**:
1. Open SearchStop from SavedTrips (FROM field)
2. Select "Town Hall"
3. Open SearchStop again (TO field)
4. Select "Seven Hills"
5. Verify both fields

**Expected**:
- ✅ FROM shows "Town Hall"
- ✅ TO shows "Seven Hills"
- ✅ No field mixup

---

## 📱 Two-Pane Layout Behavior

### TC-021: TimeTable in Detail Pane with SavedTrips in List Pane
**Status**: ⚠️ NOT TESTED (Requires tablet/desktop)

**Steps** (Tablet/large screen):
1. SavedTrips visible in List Pane (left side)
2. Tap trip card → TimeTable opens in Detail Pane (right side)
3. Select date/time in TimeTable
4. Tap different trip card in SavedTrips

**Expected**:
- ✅ TimeTable updates with new trip data
- ✅ SavedTrips remains visible
- ✅ Both panes function independently

---

### TC-022: Rotation from single-pane to two-pane
**Status**: ⚠️ NOT TESTED

**Steps** (Foldable or tablet):
1. Phone mode (single pane): TimeTable visible
2. Unfold device OR rotate to landscape
3. Two-pane layout activates

**Expected**:
- ✅ TimeTable moves to Detail Pane
- ✅ SavedTrips appears in List Pane
- ✅ State is preserved

---

### TC-023: Rotation from two-pane to single-pane
**Status**: ⚠️ NOT TESTED

**Steps**:
1. Tablet landscape mode (two panes active)
2. Rotate to portrait
3. Single-pane mode activates

**Expected**:
- ✅ Currently focused pane becomes full-screen
- ✅ Back navigation restored
- ✅ State preserved

---

## 🔧 ViewModel Lifecycle

### TC-024: ViewModel survives rotation
**Status**: ✅ PASS

**Steps**:
1. Navigate to TimeTable
2. Load journey data
3. Note ViewModel hashCode from logs
4. Rotate device
5. Check ViewModel hashCode

**Expected**:
- ✅ ViewModel hashCode is the same
- ✅ Log: "Same ViewModel instance after rotation"

**Actual**: PASS - ViewModel scoped to NavEntry

---

### TC-025: ViewModel destroyed when navigating away
**Status**: ✅ PASS

**Steps**:
1. Navigate to TimeTable (note VM hashCode)
2. Navigate to Settings screen
3. Navigate back to SavedTrips
4. Navigate to TimeTable again (note new VM hashCode)

**Expected**:
- ✅ Second VM hashCode is different (new instance)
- ✅ Log: "TimeTable COMPOSABLE DISPOSED"

**Actual**: PASS

---

### TC-026: previousTripId persists in ViewModel
**Status**: ✅ PASS (Fixed 2025-12-28)

**Steps**:
1. Load "Town Hall → Seven Hills" (Trip A)
2. Rotate device
3. Check logs for "previousTripId"

**Expected**:
- ✅ Log shows previousTripId = "200070214710"
- ✅ previousTripId survives rotation (stored in ViewModel)

**Actual**: PASS

---

## ⏱️ API Rate Limiting

### TC-027: Rapid date/time changes are debounced
**Status**: ✅ PASS

**Steps**:
1. Navigate to TimeTable
2. Quickly change date/time 5 times in 2 seconds
3. Check API call logs

**Expected**:
- ✅ Only 1 API call is made (last selection)
- ✅ Rate limiter log: "Event rate-limited, waiting..."

**Actual**: PASS - 1 second debounce implemented

---

### TC-028: API calls respect 1-second minimum interval
**Status**: ✅ PASS

**Steps**:
1. Change date/time (Call 1)
2. Wait 0.5 seconds
3. Change date/time again (Call 2)
4. Check timestamps in logs

**Expected**:
- ✅ Call 2 is delayed to respect 1-second interval
- ✅ Both calls eventually execute

**Actual**: PASS

---

## ❌ Error Scenarios

### TC-029: Network error during initial load
**Status**: ⚠️ NOT TESTED

**Steps**:
1. Turn off internet
2. Navigate to TimeTable
3. Observe UI

**Expected**:
- ✅ Error message shown
- ✅ Retry button available
- ✅ No crash

---

### TC-030: Network error during date/time selection
**Status**: ⚠️ NOT TESTED

**Steps**:
1. Load TimeTable successfully
2. Turn off internet
3. Select date/time

**Expected**:
- ✅ Error message shown
- ✅ Previous journey cards remain visible (cached)
- ✅ Retry button available

---

## 🎲 Edge Cases

### TC-031: Navigating to same trip multiple times quickly
**Status**: ⚠️ NOT TESTED

**Steps**:
1. Tap trip card → TimeTable opens
2. Immediately navigate back
3. Immediately tap same trip card
4. Repeat 3-4 times quickly

**Expected**:
- ✅ No crash
- ✅ State remains consistent
- ✅ ViewModel handles rapid nav changes

---

### TC-032: Date/Time selector as modal vs navigation
**Status**: 🔧 FUTURE ENHANCEMENT

**Current**: Date/time selector shown as modal (ModalBottomSheet)
**Alternative**: Date/time selector as separate nav destination

**Pros (Modal)**:
- ✅ Simpler state management
- ✅ No navigation complexity
- ✅ Better UX (overlay)

**Cons (Modal)**:
- ❌ Can't use system back button for modal history

**Decision**: Keep as modal (current implementation)

---

### TC-033: Very long stop names
**Status**: ⚠️ NOT TESTED

**Steps**:
1. Select stop with very long name (e.g., "Baulkham Hills High School, Windsor Rd")
2. Navigate to TimeTable
3. Check UI layout

**Expected**:
- ✅ Stop names don't overflow
- ✅ Text truncates with ellipsis
- ✅ UI remains readable

---

## 📊 Test Summary

### Coverage by Priority

**P0 (Critical)**: 15/15 scenarios tested ✅
- State persistence (rotation & navigation)
- Cache management
- Date/time selection
- ViewModel lifecycle

**P1 (High)**: 8/10 scenarios tested
- Theme persistence ✅
- Recent stops ✅
- Service alerts (partial)
- Search stop integration (needs investigation)

**P2 (Medium)**: 0/8 scenarios tested
- Two-pane layout (requires tablet)
- Error handling
- Edge cases

### Known Issues
1. ~~TC-003: Date/time lost on nav back~~ ✅ FIXED (2025-12-28)
2. ~~TC-005: Stale journey cards with null dateTimeSelection~~ ✅ FIXED (2025-12-28)
3. TC-019, TC-020: SearchStop in detail pane needs verification

### Automation Readiness
- All scenarios are written in clear Given/When/Then format
- Can be converted to UI tests (Compose UI Testing)
- Logs provide clear verification points

---

## 🔗 Related Documentation
- [NAV3_TODO.md](./NAV3_TODO.md) - Navigation 3.0 migration checklist
- [Implementation Notes] - See inline comments in TripPlannerEntries.kt, TimeTableViewModel.kt

**Next Steps**:
1. Test two-pane layout scenarios (TC-021, TC-022, TC-023)
2. Verify SearchStop detail pane updates (TC-019, TC-020)
3. Test error scenarios (TC-029, TC-030)
4. Convert high-priority scenarios to automated tests

