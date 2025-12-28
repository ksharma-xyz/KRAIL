# TimeTable Screen - Manual Test Scenarios

**Purpose**: This document contains manual test scenarios for the TimeTable screen to verify critical functionality around state management, navigation, and data persistence.

**Status**: These are currently **MANUAL** test cases. They should be automated in the future.

**Last Updated**: 2025-12-28

---

## 📋 Test Scenario Categories

1. [State Persistence - Configuration Changes](#1-state-persistence---configuration-changes)
2. [State Persistence - Navigation](#2-state-persistence---navigation)
3. [Date/Time Selection Behavior](#3-datetime-selection-behavior)
4. [Cache Management](#4-cache-management)
5. [Theme Persistence](#5-theme-persistence)
6. [Recent Stops Updates](#6-recent-stops-updates)
7. [Service Alerts Navigation](#7-service-alerts-navigation)
8. [Search Stop Integration](#8-search-stop-integration)

---

## 1. State Persistence - Configuration Changes

### TC-001: Screen Rotation - Same Trip
**Pre-conditions**:
- Open TimeTable for trip "Town Hall → Seven Hills"
- Select date/time: "Arrive by 11:30 PM, Dec 29"
- Journey cards are displayed

**Steps**:
1. Rotate device from portrait to landscape
2. Observe UI state

**Expected Results**:
- ✅ Journey cards remain visible (cached data)
- ✅ Date/time selection is preserved: "Arrive by 11:30 PM, Dec 29"
- ✅ From/To stop names are visible
- ✅ **NO** API call is made
- ✅ **NO** loading spinner
- ✅ Same ViewModel instance

**Why This Matters**: Rotation should not lose user's date/time selection or reload data unnecessarily.

**Related Issues**: Fixed on 2025-12-28 - DateTimeSelection was being lost on rotation.

---

### TC-002: Screen Rotation - Multiple Times
**Pre-conditions**:
- Open TimeTable for any trip
- Select custom date/time
- Expand a journey card

**Steps**:
1. Rotate device from portrait → landscape
2. Rotate device from landscape → portrait
3. Repeat 2-3 times

**Expected Results**:
- ✅ Date/time selection persists across all rotations
- ✅ Expanded journey card state persists
- ✅ No data reload or API calls
- ✅ No UI flicker or loading states

---

## 2. State Persistence - Navigation

### TC-003: Navigate Back to SavedTrips and Return - With Date/Time
**Pre-conditions**:
- From SavedTrips, open "Town Hall → Seven Hills"
- Select date/time: "Arrive by 10:30 PM, Dec 29"
- Journey cards loaded successfully

**Steps**:
1. Navigate back to SavedTrips screen (hardware back or gesture)
2. Immediately tap the same trip card ("Town Hall → Seven Hills")
3. Observe TimeTable screen

**Expected Results**:
- ✅ Journey cards appear immediately (from cache)
- ✅ Date/time selection is preserved: "Arrive by 10:30 PM, Dec 29"
- ✅ **NO** "Plan Your Trip" text shown
- ✅ **NO** API call is made
- ✅ **NO** loading spinner
- ✅ Same ViewModel instance

**Why This Matters**: Returning to the same trip should not lose state or reload unnecessarily.

**Related Issues**: 
- Fixed on 2025-12-28 - Date/time was lost when navigating back
- Root cause: `previousTripId` was stored in composable state, now in ViewModel

---

### TC-004: Navigate Back and Return - Default Time
**Pre-conditions**:
- From SavedTrips, open "Town Hall → Seven Hills"
- **DO NOT** select custom date/time (use default "Leave Now")
- Journey cards loaded

**Steps**:
1. Navigate back to SavedTrips
2. Open the same trip again
3. Observe TimeTable screen

**Expected Results**:
- ✅ Journey cards appear immediately (from cache)
- ✅ Shows "Plan Your Trip" (because no custom date/time was selected)
- ✅ Journey times reflect current time
- ✅ **NO** API call on return
- ✅ Same ViewModel instance

---

### TC-005: Navigate to Different Trip
**Pre-conditions**:
- Open "Town Hall → Seven Hills" with custom date/time
- Journey cards loaded

**Steps**:
1. Navigate back to SavedTrips
2. Select a **DIFFERENT** trip (e.g., "Roseville → Seven Hills")
3. Observe TimeTable screen

**Expected Results**:
- ✅ Shows loading spinner
- ✅ Date/time selection is **RESET** to default
- ✅ Shows "Plan Your Trip"
- ✅ **API call IS made** for the new trip
- ✅ Old journey cards are cleared
- ✅ New journey cards are loaded
- ✅ Same ViewModel instance (retained)

**Why This Matters**: Different trip should clear state and fetch fresh data.

---

### TC-006: Process Death Recovery
**Pre-conditions**:
- Open TimeTable with custom date/time
- Enable "Don't keep activities" in Developer Options

**Steps**:
1. Select date/time: "Arrive by 11:00 PM, Dec 30"
2. Navigate to another app (e.g., Settings)
3. Return to KRAIL app

**Expected Results**:
- ✅ Date/time selection is restored from saved state
- ✅ Trip info (from/to stops) is visible
- ✅ Journey cards may need to reload (acceptable for process death)

---

## 3. Date/Time Selection Behavior

### TC-007: Select Date/Time - First Time
**Pre-conditions**:
- Open TimeTable for any trip
- Default state (no custom date/time)

**Steps**:
1. Tap on "Plan Your Trip" button
2. Select "Arrive" option
3. Select date: Tomorrow
4. Select time: 10:30 PM
5. Confirm selection
6. Observe UI

**Expected Results**:
- ✅ Loading spinner appears
- ✅ API call is made with selected parameters
- ✅ Journey cards update with new results
- ✅ Date/time chip shows: "Arrive by 10:30 PM, [Date]"
- ✅ Reset button (X) is visible

---

### TC-008: Change Date/Time - Multiple Times
**Pre-conditions**:
- Open TimeTable with custom date/time already set

**Steps**:
1. Change time from "10:30 PM" to "11:00 PM"
2. Immediately change date to next day
3. Change option from "Arrive" to "Leave"

**Expected Results**:
- ✅ Only ONE API call per change (not multiple)
- ✅ Rate limiter prevents rapid API calls (1 second delay)
- ✅ UI updates correctly after each change
- ✅ Previous journey cards are cleared

---

### TC-009: Reset Date/Time Selection
**Pre-conditions**:
- TimeTable with custom date/time selected

**Steps**:
1. Tap the X (reset) button on date/time chip
2. Observe UI

**Expected Results**:
- ✅ Date/time chip changes to "Plan Your Trip"
- ✅ Loading spinner appears
- ✅ API call is made with current time
- ✅ Journey cards update to current time results
- ✅ Analytics event: `DateTimeSelectEvent` with `isReset=true`

---

## 4. Cache Management

### TC-010: Cache Preservation on Rotation
**Pre-conditions**:
- Open TimeTable
- Journey cards loaded

**Steps**:
1. Note the journey card details (times, stops)
2. Rotate device
3. Compare journey cards

**Expected Results**:
- ✅ Exact same journey cards displayed
- ✅ Same expanded/collapsed state
- ✅ Same order
- ✅ No API call

---

### TC-011: Cache Cleared on Trip Change
**Pre-conditions**:
- Open "Town Hall → Seven Hills"
- Journey cards loaded and cached

**Steps**:
1. Navigate back to SavedTrips
2. Open "Roseville → Seven Hills" (different trip)
3. Observe loading

**Expected Results**:
- ✅ Old journey cards are **NOT** visible
- ✅ Loading spinner appears
- ✅ Cache is cleared
- ✅ New API call is made
- ✅ New journey cards appear

---

### TC-012: Cache Cleared on Date/Time Change
**Pre-conditions**:
- TimeTable with journey cards for "Leave Now"

**Steps**:
1. Change date/time to tomorrow 10:00 AM
2. Observe during loading

**Expected Results**:
- ✅ Old journey cards disappear
- ✅ Loading spinner appears
- ✅ Cache is cleared
- ✅ New journey cards for tomorrow appear

---

## 5. Theme Persistence

### TC-013: Theme Persists on Rotation
**Pre-conditions**:
- Set theme to "Green" (Ferry theme) in Settings
- Open any TimeTable

**Steps**:
1. Verify theme color is Green
2. Rotate device
3. Check theme color

**Expected Results**:
- ✅ Theme remains Green after rotation
- ✅ No flicker to Train theme
- ✅ Theme loaded from database correctly

**Related Issues**: Fixed on 2025-12-28 - Theme was defaulting to Train on rotation.

---

### TC-014: Theme Persists on Navigation Back
**Pre-conditions**:
- Set theme to "Green"
- Open TimeTable

**Steps**:
1. Navigate back to SavedTrips
2. Navigate back to TimeTable
3. Check theme

**Expected Results**:
- ✅ Theme remains Green
- ✅ No default to Train theme

---

## 6. Recent Stops Updates

### TC-015: Selected Stop Shows in Recent List
**Pre-conditions**:
- Open Search Stop screen
- Select a stop that's NOT in recent list

**Steps**:
1. Select stop "Central Station"
2. Observe SearchStopSection in SavedTrips
3. Check recent stops list

**Expected Results**:
- ✅ "Central Station" appears in recent stops
- ✅ Recent stops list updates immediately
- ✅ Stop can be tapped to start new trip

**Related Issues**: Fixed on 2025-12-28 - Added `RefreshRecentStopsList` event.

---

### TC-016: Recent Stops Persist After App Restart
**Pre-conditions**:
- Select 3-4 stops from search

**Steps**:
1. Kill app completely
2. Reopen app
3. Check SearchStopSection

**Expected Results**:
- ✅ Recent stops are still visible
- ✅ Order is preserved (most recent first)
- ✅ All stop details are correct

---

## 7. Service Alerts Navigation

### TC-017: Open Service Alert - Modal Behavior
**Pre-conditions**:
- Open TimeTable with journey cards

**Steps**:
1. Tap on a service alert icon/button
2. Observe navigation
3. Use gesture back
4. Use hardware back button
5. Use back button in TopAppBar

**Expected Results**:
- ✅ Service alert opens as modal (overlay)
- ✅ TimeTable is visible underneath (if space allows)
- ✅ Gesture back closes modal → returns to TimeTable
- ✅ Hardware back closes modal → returns to TimeTable
- ✅ TopAppBar back closes modal → returns to TimeTable
- ✅ All three back actions behave identically

**Related Issues**: Fixed on 2025-12-28 - Gesture/hardware back was closing TimeTable instead of just the modal.

---

### TC-018: Service Alert on Two-Pane Layout (Tablet)
**Pre-conditions**:
- Open TimeTable on tablet/large screen
- Two-pane layout is active

**Steps**:
1. Tap service alert
2. Observe layout

**Expected Results**:
- ✅ TimeTable in left pane
- ✅ Service Alert in right pane (if implemented)
- ❓ OR: Service Alert as modal overlay (current behavior)

---

## 8. Search Stop Integration

### TC-019: Search Stop from SavedTrips
**Pre-conditions**:
- On SavedTrips screen

**Steps**:
1. Tap "From" field in SearchStopSection
2. Search for a stop
3. Select a stop
4. Observe SavedTrips screen

**Expected Results**:
- ✅ Selected stop appears in "From" field
- ✅ SearchStopSection updates immediately
- ✅ Recent stops list updates

**Related Issues**: 
- Mentioned in NAV3_TODO.md #1
- "When SearchStopScreen is open in Detail Pane, the selected stops are not updated in the SavedTripScreen"

---

### TC-020: Search Stop Result Returns Correctly
**Pre-conditions**:
- SearchStopScreen is open

**Steps**:
1. Search for "Town Hall"
2. Select "Town Hall Station"
3. Check if result is passed back correctly

**Expected Results**:
- ✅ Selected stop data is passed back
- ✅ Stop ID, name, and location are correct
- ✅ UI updates with selection

---

## 9. Two-Pane Layout Behavior (Tablets)

### TC-021: TimeTable Opens in Right Pane from SavedTrips
**Pre-conditions**:
- Tablet/large screen with two-pane layout
- On SavedTrips screen

**Steps**:
1. Tap a saved trip card
2. Observe navigation

**Expected Results**:
- ✅ SavedTrips remains in left pane
- ✅ TimeTable opens in right pane
- ✅ Both screens visible simultaneously

---

### TC-022: Rotation Changes Layout from Single to Two-Pane
**Pre-conditions**:
- Phone in portrait mode (single pane)
- TimeTable open with custom date/time

**Steps**:
1. Rotate to landscape (triggers two-pane)
2. Observe UI state

**Expected Results**:
- ✅ Layout changes to two-pane
- ✅ Date/time selection is preserved
- ✅ Journey cards remain visible
- ✅ No API call
- ✅ No loading state

**Related Issues**: Fixed on 2025-12-28 - State was lost when layout changed.

---

## 10. Edge Cases & Error Scenarios

### TC-023: No Internet - Cached Data Available
**Pre-conditions**:
- Load TimeTable with internet ON
- Journey cards are cached

**Steps**:
1. Turn OFF internet/airplane mode
2. Rotate device
3. Navigate back and return

**Expected Results**:
- ✅ Cached journey cards remain visible
- ✅ No error messages for cached content
- ✅ Date/time selection persists

---

### TC-024: No Internet - Fresh Load
**Pre-conditions**:
- Turn OFF internet
- No cached data

**Steps**:
1. Try to load TimeTable
2. Observe error state

**Expected Results**:
- ✅ Error message displayed
- ✅ Retry button available
- ❌ No crash

---

### TC-025: API Error - Retry Functionality
**Pre-conditions**:
- API returns error (simulate with bad network)

**Steps**:
1. Load TimeTable → error occurs
2. Fix network
3. Tap "Retry" button
4. Observe loading

**Expected Results**:
- ✅ Loading spinner appears
- ✅ API call is retried
- ✅ Journey cards load successfully
- ✅ Error state is cleared

---

## 11. ViewModel Lifecycle

### TC-026: ViewModel Retained on Navigation
**Pre-conditions**:
- Open TimeTable
- Note ViewModel instance hash code (from logs)

**Steps**:
1. Navigate to another screen
2. Return to TimeTable
3. Check ViewModel hash code in logs

**Expected Results**:
- ✅ Same ViewModel instance (same hash code)
- ✅ State is preserved
- ✅ No unnecessary recreation

---

### TC-027: ViewModel Cleared on Different Trip
**Pre-conditions**:
- Open "Trip A"
- Navigate back
- Open "Trip B"

**Steps**:
1. Check if new ViewModel is created (may depend on implementation)
2. Verify Trip B has fresh state

**Expected Results**:
- ✅ Trip B loads fresh data
- ✅ No state leak from Trip A
- ✅ Correct trip info displayed

---

## 12. Analytics Events

### TC-028: Screen View Event Fired
**Pre-conditions**:
- Analytics logging enabled

**Steps**:
1. Open TimeTable
2. Check logs for analytics event

**Expected Results**:
- ✅ `ScreenViewEvent(screen=TimeTable)` logged
- ✅ Event fired only ONCE on first load
- ✅ NOT fired on rotation

---

### TC-029: Date/Time Selection Event Fired
**Pre-conditions**:
- TimeTable open

**Steps**:
1. Select date/time
2. Check logs

**Expected Results**:
- ✅ `DateTimeSelectEvent` logged
- ✅ Contains: dayOfWeek, time, journeyOption
- ✅ `isReset=false` for selection
- ✅ `isReset=true` for reset action

---

## 13. UI/UX Validation

### TC-030: Loading States Are Appropriate
**Pre-conditions**:
- Various scenarios

**Validation Points**:
- ✅ Loading spinner shows ONLY when API call is in progress
- ✅ NO loading spinner on rotation
- ✅ NO loading spinner on navigation back to same trip
- ✅ Loading spinner shows on trip change
- ✅ Loading spinner shows on date/time change

---

### TC-031: Journey Card Interactions
**Pre-conditions**:
- Journey cards loaded

**Steps**:
1. Tap to expand a journey card
2. Rotate device
3. Tap to collapse
4. Navigate back and return

**Expected Results**:
- ✅ Expand/collapse works smoothly
- ✅ Expanded state persists on rotation
- ✅ Service alerts are accessible
- ✅ Journey details are correct

---

## 14. Date/Time Selector Modal (Future Enhancement)

### TC-032: Date/Time as Modal vs Detail Pane
**Status**: ⚠️ TODO (See NAV3_TODO.md #3)

**Current Behavior**:
- Date/Time selector opens in detail pane
- May occupy full screen on small devices

**Proposed Behavior**:
- Date/Time selector as modal bottom sheet
- Simplifies navigation logic
- Better data passing

**Test When Implemented**:
1. Open date/time selector
2. Should appear as modal overlay
3. Dismiss should return to TimeTable with selection

---

## 📊 Test Execution Log Template

Use this template to track test execution:

```markdown
## Test Execution: [Date]
**Tester**: [Name]
**App Version**: [Version]
**Device**: [Device Model]
**OS Version**: [OS Version]

| Test ID | Status | Notes |
|---------|--------|-------|
| TC-001  | ✅ PASS | |
| TC-002  | ✅ PASS | |
| TC-003  | ❌ FAIL | Date/time lost on nav back |
| ...     | ...    | ... |

**Summary**: X/Y tests passed
**Blockers**: [List any blockers]
**Follow-up**: [Actions needed]
```

---

## 🔧 Automation Roadmap

**Priority 1 - Critical Flows**:
- TC-001: Rotation state preservation
- TC-003: Navigation back state preservation
- TC-005: Different trip state reset
- TC-013: Theme persistence

**Priority 2 - User Journeys**:
- TC-007: Date/time selection
- TC-009: Date/time reset
- TC-015: Recent stops update
- TC-017: Service alert navigation

**Priority 3 - Edge Cases**:
- TC-023: Offline behavior
- TC-025: Error retry
- TC-031: Journey card interactions

---

## 📝 Notes for Automation

When automating these tests, consider:

1. **State Verification**: 
   - Use ViewModel state as source of truth
   - Verify UI reflects state correctly

2. **Cache Validation**:
   - Check in-memory cache (journeys map)
   - Verify database queries

3. **Network Mocking**:
   - Mock API responses for consistent tests
   - Test offline scenarios

4. **Analytics Verification**:
   - Mock analytics tracker
   - Verify correct events fired

5. **Multi-Configuration**:
   - Test on different screen sizes
   - Test orientation changes
   - Test theme variations

---

**Document Maintainer**: AI Assistant  
**Review Frequency**: After each major TimeTable feature change  
**Version**: 1.0

