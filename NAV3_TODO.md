# Navigation 3.0 TODO List

## ✅ Completed (2025-12-28)

- ✅ **Date/Time Selection Persistence on Navigation Back** (See TC-003 in TIMETABLE_TEST_SCENARIOS.md)
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
- Edge Cases & Error Scenarios

Total: 32+ manual test scenarios documented for future automation.

## 🐛 Known Issues

None currently blocking release.

## 📝 Notes

- All fixes from 2025-12-28 session focus on state management and navigation
- ViewModel-based state tracking (previousTripId) is key to fixing navigation issues
- rememberSaveable used for UI state that needs to survive rotation
- Most critical flows now have documented test scenarios

