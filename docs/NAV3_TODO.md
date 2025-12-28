# Navigation 3.0 TODO List

## ✅ Completed (2025-12-28)

- ✅ **Stale Journey Cards with Null DateTimeSelection** (See TC-005 in TIMETABLE_TEST_SCENARIOS.md)
  - Fixed: When navigating back to same trip, dateTimeSelection and journey cards now stay in sync
  - Root cause: ViewModel preserved dateTimeSelection while UI rememberSaveable started with null
  - Solution: Added sync LaunchedEffect to match UI state with ViewModel state on composition
  - Result: No more "Plan Your Trip" text showing with time-specific cached journey cards

- ✅ **Date/Time Selection Persistence on Navigation Back** (See TC-003)
  - Fixed: Date/time selection now persists when navigating back from SavedTrips to TimeTable
  - Root cause: `previousTripId` was stored in composable state, moved to ViewModel
  - Result: Cached data is reused, no unnecessary API calls

- ✅ **Date/Time Selection Persistence on Rotation** (See TC-001, TC-002)
  - Fixed: Date/time selection survives screen rotation
  - Custom Saver implemented for DateTimeSelectionItem

- ✅ **Theme Persistence** (See TC-013, TC-014)
  - Fixed: Theme no longer defaults to Train theme on rotation
  - Theme correctly loaded from database

- ✅ **Recent Stops Update** (See TC-015, TC-016)
  - Fixed: Selected stop from SearchStop now appears in recent stops list
  - Added `RefreshRecentStopsList` event

- ✅ **Service Alert Back Navigation** (See TC-017)
  - Fixed: Gesture/hardware back now correctly closes modal instead of TimeTable
  - NavigationBackHandler implemented

## 🔄 In Progress / To Be Done

1. **SearchStopScreen Detail Pane Updates** (See TC-019, TC-020)
   - When SearchStopScreen is open in Detail Pane, the selected stops are not updated in the SavedTripScreen (SearchStopSection)
   - Status: Needs investigation
   - Related Test Cases: TC-019, TC-020

2. ~~Check When Date time is selected in the detail pane, if the time is updated when nav back to timetable screen or not.~~
   - Status: ✅ COMPLETED - See TC-003

3. **Date/Time Selector as Modal** (Future Enhancement)
   - Should date time screen be instead a modal as service alert screen? (Will simplify nav logic / data passing)
   - Status: To be evaluated
   - Related Test Case: TC-032
   - Benefits: Simpler navigation, better data passing
   - Trade-offs: Need to evaluate UX impact

## 📋 Test Scenarios

All test scenarios are documented in: **TIMETABLE_TEST_SCENARIOS.md**

Key test categories:
- State Persistence (Configuration Changes & Navigation)
- Date/Time Selection Behavior
- Cache Management
- Theme Persistence
- Service Alerts Navigation
- Search Stop Integration
- Two-Pane Layout Behavior
- ViewModel Lifecycle
- API Rate Limiting
- Error Scenarios
- Edge Cases

Total: 33 manual test scenarios documented for future automation.

## 🐛 Known Issues

1. ~~TC-005: Stale journey cards showing with "Plan Your Trip" text~~ ✅ FIXED (2025-12-28)
   - Added sync LaunchedEffect to match UI state with ViewModel on composition
2. TC-019, TC-020: SearchStop in detail pane updates need verification (Two-pane layout)

## 📝 Notes

- All fixes from 2025-12-28 session focus on state management and navigation
- ViewModel-based state tracking (previousTripId) is key to fixing navigation issues
- rememberSaveable used for UI state that needs to survive rotation
- Sync between ViewModel and UI state critical for consistency
- Most critical flows now have documented test scenarios

## TimeTable
1. How auto refresh time table works, how time text is changed
2. 