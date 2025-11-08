# Snapshot Testing - Progress & TODOs

## 🎉 Current Status (Updated: November 9, 2025)

### ✅ Completed
- **Infrastructure:** BaseSnapshotTest class created - makes adding tests super easy!
- **Configuration:** All defaults configured (font scales, dark mode, etc.)
- **Documentation:** 3 comprehensive README files created
- **Git LFS:** Configured for all screenshot PNGs
- **Taj Module:** Test class created and ready to run

### 🎯 Next Milestone: First Screenshots Generated
**Goal:** Validate the entire setup by generating and committing first real screenshots

**Immediate Actions:**
1. Run `./gradlew :taj:testDebugUnitTest --tests "*TajSnapshotTest*"`
2. Verify screenshots are generated in `taj/screenshots/`
3. Commit screenshots to Git
4. Test another module to prove scalability

### 📊 Progress Overview
- **Priority 1 (Setup):** ✅ 100% Complete
- **Priority 2 (Coverage):** 🔄 10% Complete (infrastructure ready, need to generate snapshots)
- **Priority 3 (Optimization):** ⏸️ Not Started (waiting for initial data)
- **Priority 4 (Advanced):** ⏸️ Not Started
- **Priority 5 (Monitoring):** ⏸️ Not Started

---

## Priority 1: Setup & Foundation ✅ COMPLETED
- [x] Choose testing library (Roborazzi + ComposablePreviewScanner) ✅
- [x] Add dependencies to `libs.versions.toml` ✅
- [x] Add Roborazzi plugin ✅
- [x] Configure `taj/build.gradle.kts` with Roborazzi ✅
- [x] Create `@ScreenshotTest` annotation in core/snapshot-testing ✅
- [x] Create `BaseSnapshotTest` base class for reusability ✅
- [x] Create test file in `androidUnitTest/` (TajSnapshotTest) ✅
- [x] Create `SnapshotDefaults` for default settings ✅
- [x] Setup Git LFS for screenshot storage ✅
- [x] Create `.gitattributes` file ✅
- [x] Fix file path issues (screenshots/ not taj/screenshots/) ✅
- [x] Document usage and workflow ✅
  - [x] core/snapshot-testing/README.md
  - [x] taj/screenshots/README.md
  - [x] docs/snapshot-test/snapshot-testing-quick-reference.md

## Priority 2: Coverage & Validation 🔄 IN PROGRESS

### ✅ Infrastructure Ready
- [x] BaseSnapshotTest class working
- [x] Test execution working (TajSnapshotTest)
- [x] File paths fixed
- [x] Documentation complete

### 🎯 Immediate Next Steps (This Week)

#### 1. Verify Test Execution & Generate First Screenshots
- [ ] Run test and verify screenshots are generated:
  ```bash
  ./gradlew :taj:testDebugUnitTest --tests "*TajSnapshotTest*" --rerun-tasks
  ```
- [ ] Verify screenshots appear in `taj/screenshots/`
- [ ] Check screenshot file sizes (<150KB target)
- [ ] Inspect screenshot quality manually
- [ ] Run `recordRoborazziDebug` task:
  ```bash
  ./gradlew :taj:recordRoborazziDebug
  ```
- [ ] Commit initial screenshots to Git with LFS:
  ```bash
  git add taj/screenshots/
  git commit -m "test: add initial Taj component snapshots"
  ```

#### 2. Add More Taj Component Previews
- [ ] Identify existing Taj components without snapshots
- [ ] Add `@ScreenshotTest` annotations to existing `@Preview` functions
- [ ] Create new preview functions for components without them
- [ ] Target: 5-10 Taj components initially

#### 3. Test Another Module (Proof of Concept)
- [ ] Choose a feature module (e.g., `discover/ui` or `info-tile/ui`)
- [ ] Add Roborazzi plugin to module's `build.gradle.kts`
- [ ] Add `implementation(projects.core.snapshotTesting)` dependency
- [ ] Create `ModuleSnapshotTest` extending `BaseSnapshotTest`
- [ ] Add `@ScreenshotTest` to 2-3 previews
- [ ] Run and verify it works
- [ ] Document any issues or improvements needed

#### 4. Create Monitoring Script
- [ ] Create `scripts/snapshot-stats.sh` to report:
  - Total screenshot count
  - Average file size
  - Largest files
  - Screenshots per module
  ```bash
  #!/bin/bash
  # Report snapshot statistics
  find . -name "screenshots" -type d -exec sh -c '
    module=$(dirname {})
    count=$(find {} -name "*.png" | wc -l)
    size=$(du -sh {} | cut -f1)
    echo "$module: $count files, $size"
  ' \;
  ```

### Annotate Components by Module

#### Design System (Taj) - Target: Complete Coverage
- [ ] TextField variants
- [ ] Button variants
- [ ] Divider types
- [ ] CookieShapeBox
- [ ] SeparatorIcon
- [ ] Text components
- [ ] Card components
- [ ] Icon components
- [ ] Layout components

#### Trip Planner Module - Target: Critical Components
- [ ] SavedTripCard
- [ ] SearchStopRow
- [ ] StopSearchListItem
- [ ] ErrorMessage
- [ ] TransportModeIcon (all variants)
- [ ] Loading states
- [ ] Empty states

#### Other Modules (Lower Priority)
- [ ] Discover UI components
- [ ] Info Tile components
- [ ] Social UI components
- [ ] Park & Ride components

### CI/CD Integration (After Initial Validation)
- [ ] Review existing CI/CD workflow files
- [ ] Add screenshot verification job to existing workflow
- [ ] Configure PR checks for screenshot tests
- [ ] Setup artifact upload for diff images on failure
- [ ] Add screenshot size check (fail if >200KB average)
- [ ] Test CI/CD flow with dummy PR
- [ ] Document CI/CD integration in main README

## Priority 3: Optimization (Month 2)

### File Size Optimization
- [ ] Run initial size audit
  ```bash
  ./scripts/snapshot-stats.sh
  ```
- [ ] Review current average screenshot size
- [ ] Target: <150KB per screenshot
- [ ] Identify oversized screenshots (>200KB)
- [ ] Investigate causes of large files
- [ ] Consider PNG compression tools
- [ ] Test `resizeScale = 0.8` for non-critical tests
- [ ] Add automated size checks in CI
- [ ] Document size optimization guidelines

### Organization & Structure
- [ ] Organize screenshots by feature module
  ```
  screenshots/
    ├── trip-planner/
    ├── park-ride/
    ├── taj/
    └── social/
  ```
- [ ] Create naming conventions document
- [ ] Implement hierarchical directory structure
- [ ] Add screenshot count limits per feature
- [ ] Create guidelines for when to split test files
- [ ] Document organization strategy

### Performance Optimization
- [ ] Measure baseline test execution time
- [ ] Target: <5 minutes for all screenshot tests
- [ ] Identify slow tests
- [ ] Parallelize test execution if needed
- [ ] Cache Robolectric dependencies in CI
- [ ] Optimize scanner configuration
- [ ] Document performance benchmarks

## Priority 4: Advanced Features (Month 3)

### Multi-Device Testing
- [ ] **Tablet Configurations**
  - [ ] Add 10-inch tablet config
  - [ ] Add 7-inch tablet config
  - [ ] Test adaptive layouts on tablets
  - [ ] Document tablet testing guidelines
- [ ] **Landscape Orientation**
  - [ ] Identify components needing landscape tests
  - [ ] Add landscape configs for navigation
  - [ ] Add landscape configs for content screens
  - [ ] Document landscape testing strategy
- [ ] **Foldable Devices**
  - [ ] Research foldable device configs
  - [ ] Add foldable device tests for adaptive UI
  - [ ] Document foldable testing approach
- [ ] **Documentation**
  - [ ] Create guide: "When to test different devices"
  - [ ] Add device configuration examples
  - [ ] Document device selection criteria

### Accessibility Testing
- [ ] **Font Scale Testing**
  - [ ] Verify 1.0f, 1.5f, 2.0f work automatically
  - [ ] Add 2.5f for extreme cases
  - [ ] Test text truncation at large scales
  - [ ] Verify touch target sizes at all scales
- [ ] **High Contrast Mode**
  - [ ] Research high contrast testing approach
  - [ ] Add high contrast color schemes
  - [ ] Test critical components in high contrast
- [ ] **Locale Testing**
  - [ ] Add RTL language tests (Arabic, Hebrew)
  - [ ] Test long text languages (German)
  - [ ] Test CJK character rendering
  - [ ] Add date/time format tests
- [ ] **Documentation**
  - [ ] Create accessibility testing guide
  - [ ] Document WCAG compliance checks
  - [ ] Add accessibility checklist

### Animation Testing
- [ ] **Identify Animated Components**
  - [ ] Loading indicators
  - [ ] Progress bars
  - [ ] Transitions
  - [ ] Pull-to-refresh
  - [ ] Expandable cards
- [ ] **Frame-by-Frame Capture**
  - [ ] Setup AnimationScreenshotTest.kt
  - [ ] Test loading states (0ms, 300ms, 600ms)
  - [ ] Test transitions (start, middle, end)
  - [ ] Test state changes
- [ ] **Animation Guidelines**
  - [ ] Document when to test animations
  - [ ] Create animation test examples
  - [ ] Add threshold guidelines for animations
  - [ ] Document best practices

### Dark Mode Coverage
- [ ] **Audit Current Coverage**
  - [ ] List components with dark mode support
  - [ ] Identify missing dark mode tests
  - [ ] Prioritize critical components
- [ ] **Add Dark Mode Tests**
  - [ ] Trip Planner components
  - [ ] Park & Ride components
  - [ ] Navigation components
  - [ ] Design system components
- [ ] **Automatic Comparison**
  - [ ] Setup side-by-side light/dark comparison
  - [ ] Create dark mode test suite
  - [ ] Document dark mode testing strategy

## Priority 5: Monitoring & Metrics (Ongoing)

### Quality Metrics Dashboard
- [ ] **Setup Tracking**
  - [ ] Track total screenshot count over time
  - [ ] Monitor average file size trends
  - [ ] Track test execution time
  - [ ] Monitor storage usage (Git LFS)
  - [ ] Track coverage percentage
  - [ ] Track failed tests frequency
- [ ] **Visualization**
  - [ ] Create spreadsheet/dashboard
  - [ ] Add graphs for trends
  - [ ] Setup alerts for thresholds
  - [ ] Share with team regularly

### Automated Reporting
- [ ] **Weekly Reports**
  - [ ] Snapshot health report
  - [ ] New screenshots added
  - [ ] Modified screenshots
  - [ ] Large files identified
  - [ ] Coverage changes
- [ ] **PR Integration**
  - [ ] Add PR comment with screenshot changes
  - [ ] Show before/after comparison links
  - [ ] Highlight new tests added
  - [ ] Flag large file additions
- [ ] **Team Notifications**
  - [ ] Setup Slack/Discord bot
  - [ ] Send notifications for failures
  - [ ] Weekly summary to team channel
  - [ ] Monthly coverage report
- [ ] **Email Digests**
  - [ ] Weekly team digest
  - [ ] Monthly stakeholder report
  - [ ] Quarterly review summary

### Cost Monitoring
- [ ] **Git LFS Usage**
  - [ ] Track bandwidth usage
  - [ ] Monitor storage growth
  - [ ] Setup cost alerts
  - [ ] Plan for scaling
- [ ] **CI/CD Costs**
  - [ ] Track CI minutes used by screenshot tests
  - [ ] Optimize test execution time
  - [ ] Consider self-hosted runners
  - [ ] Document cost optimization strategies
- [ ] **Storage Analysis**
  - [ ] Estimate future storage needs
  - [ ] Plan archival strategy
  - [ ] Consider compression options
  - [ ] Document storage management

## Code Quality Checks

### Size Monitoring Scripts

```bash
# Track screenshot sizes
find composeApp/screenshots -name "*.png" -exec du -h {} \; | sort -rh | head -20

# Get total size
du -sh composeApp/screenshots

# Average size calculation
total_bytes=$(du -s composeApp/screenshots | cut -f1)
count=$(find composeApp/screenshots -name "*.png" | wc -l)
echo "Average: $((total_bytes / count))KB per screenshot"

# Find outliers (>200KB)
find composeApp/screenshots -name "*.png" -size +200k -exec du -h {} \;
```

### Count Tracking Scripts

```bash
# Total screenshots
find composeApp/screenshots -name "*.png" | wc -l

# By feature directory
for dir in composeApp/screenshots/*/; do
  count=$(find "$dir" -name '*.png' | wc -l)
  echo "$(basename "$dir"): $count screenshots"
done

# By variant type
echo "Normal (1.0x): $(find composeApp/screenshots -name '*_normal.png' | wc -l)"
echo "Large (1.5x): $(find composeApp/screenshots -name '*_large.png' | wc -l)"
echo "XLarge (2.0x): $(find composeApp/screenshots -name '*_xlarge.png' | wc -l)"
echo "Landscape: $(find composeApp/screenshots -name '*_landscape*.png' | wc -l)"
echo "Tablet: $(find composeApp/screenshots -name '*_tablet*.png' | wc -l)"
echo "Dark: $(find composeApp/screenshots -name '*_dark*.png' | wc -l)"
```

### Coverage Tracking

```bash
# Total @Preview annotations in commonMain
total_previews=$(grep -r "@Preview" composeApp/src/commonMain --include="*.kt" | wc -l)
echo "Total @Preview: $total_previews"

# @ScreenshotTest annotations
screenshot_tests=$(grep -r "@ScreenshotTest" composeApp/src/commonMain --include="*.kt" | wc -l)
echo "With @ScreenshotTest: $screenshot_tests"

# Coverage percentage
coverage=$((screenshot_tests * 100 / total_previews))
echo "Coverage: ${coverage}%"

# By module
for module in composeApp/src/commonMain/kotlin/xyz/ksharma/krail/*/; do
  module_name=$(basename "$module")
  previews=$(find "$module" -name "*.kt" -exec grep -l "@Preview" {} \; | wc -l)
  tests=$(find "$module" -name "*.kt" -exec grep -l "@ScreenshotTest" {} \; | wc -l)
  echo "$module_name: $tests/$previews"
done
```

### Quality Thresholds

| Metric | Target | Warning | Critical |
|--------|--------|---------|----------|
| Max screenshot size | <150KB | 150-200KB | >200KB |
| Total directory size | <50MB | 50-100MB | >100MB |
| Average screenshot size | <120KB | 120-150KB | >150KB |
| Test execution time | <5min | 5-10min | >10min |
| Coverage | >70% | 50-70% | <50% |
| Max screenshots per preview | 6 | 6-12 | >12 |
| Failed tests | 0 | 1-3 | >3 |

## Maintenance Tasks

### Daily
- [ ] Monitor CI build status for screenshot tests
- [ ] Review failed screenshot tests
- [ ] Check PR comments for screenshot changes

### Weekly
- [ ] Run `./scripts/snapshot-stats.sh`
- [ ] Review new large screenshots (>200KB)
- [ ] Update screenshots for merged UI changes
- [ ] Check for duplicate or unused screenshots
- [ ] Review test execution time trends

### Monthly
- [ ] Run full screenshot audit
- [ ] Review Git LFS storage usage
- [ ] Check coverage metrics (target: 70%)
- [ ] Update documentation
- [ ] Clean up unused screenshots
- [ ] Optimize large screenshots
- [ ] Review and update thresholds
- [ ] Team sync on screenshot testing

### Quarterly
- [ ] Review testing strategy effectiveness
- [ ] Update dependencies (Roborazzi, scanner)
- [ ] Evaluate new features from libraries
- [ ] Performance optimization review
- [ ] Team training/workshop
- [ ] Review and update processes
- [ ] Cost analysis and optimization

## Investigation Triggers

### When to Investigate
- ❌ Total size >100MB
- ❌ Average size >200KB
- ❌ Test execution >15 minutes
- ❌ Coverage drops below 50%
- ❌ More than 3 failed tests
- ❌ Frequent false positives (>10%)
- ❌ CI failures >20% of runs

### Investigation Steps
1. Run comprehensive analysis
   ```bash
   ./scripts/snapshot-stats.sh
   ```
2. Identify outliers (large files, slow tests)
3. Review recent changes (last 7 days)
4. Check CI logs for patterns
5. Analyze failed test screenshots
6. Review team feedback
7. Document findings
8. Create improvement tasks
9. Update documentation
10. Share learnings with team

## Future Enhancements

### Tool Improvements
- [ ] **IntelliJ/Android Studio Plugin**
  - [ ] Auto-suggest @ScreenshotTest for @Preview
  - [ ] Quick action to generate screenshot
  - [ ] View screenshot in IDE
  - [ ] Compare with previous version
  - [ ] Navigate between code and screenshot
- [ ] **Gradle Plugin**
  - [ ] Custom Roborazzi plugin for KRAIL
  - [ ] Automate common tasks
  - [ ] Enforce quality thresholds
  - [ ] Generate reports
  - [ ] Simplify configuration
- [ ] **Web Dashboard**
  - [ ] View all screenshots
  - [ ] Search and filter
  - [ ] Compare versions
  - [ ] Track history
  - [ ] Team collaboration
- [ ] **Visual Diff Viewer**
  - [ ] Side-by-side comparison
  - [ ] Overlay mode
  - [ ] Highlight differences
  - [ ] Approve/reject changes
  - [ ] Comment on screenshots
- [ ] **Design Tool Integration**
  - [ ] Figma plugin for comparison
  - [ ] Export designs as baseline
  - [ ] Sync design tokens
  - [ ] Automated design QA

### Process Improvements
- [ ] **PR Template Enhancement**
  - [ ] Add screenshot review checklist
  - [ ] Include screenshot change summary
  - [ ] Link to visual diffs
  - [ ] Require design approval
- [ ] **Best Practices Guide**
  - [ ] When to add screenshots
  - [ ] How to organize tests
  - [ ] Naming conventions
  - [ ] Common pitfalls
  - [ ] Performance tips
- [ ] **Team Training**
  - [ ] Onboarding documentation
  - [ ] Video tutorials
  - [ ] Live workshops
  - [ ] Pair programming sessions
  - [ ] Office hours for questions
- [ ] **Documentation**
  - [ ] Comprehensive guide
  - [ ] FAQs
  - [ ] Troubleshooting guide
  - [ ] Examples repository
  - [ ] Migration guides

### Integration Ideas
- [ ] **Project Management**
  - [ ] Jira/Linear integration
  - [ ] Link screenshots to tickets
  - [ ] Design QA workflow
  - [ ] Automated status updates
- [ ] **Design Tools**
  - [ ] Figma integration
  - [ ] Sketch integration
  - [ ] Compare with designs
  - [ ] Export from design tools
- [ ] **Communication**
  - [ ] Slack bot for updates
  - [ ] Discord notifications
  - [ ] Email digests
  - [ ] Weekly summaries
- [ ] **Testing**
  - [ ] A/B testing with screenshots
  - [ ] User testing integration
  - [ ] Analytics integration
  - [ ] Performance monitoring

## Success Metrics

### Short-term (3 months)
- [ ] 50% of @Preview composables have @ScreenshotTest
- [ ] Average screenshot size <150KB
- [ ] Test execution time <5 minutes
- [ ] Zero critical bugs caught by screenshot tests
- [ ] Git LFS storage <50MB
- [ ] Team adoption: All developers using snapshot tests

### Medium-term (6 months)
- [ ] 70% coverage of @Preview composables
- [ ] <2% false positive rate
- [ ] <5 minute test execution time
- [ ] CI success rate >95%
- [ ] 100% of PRs include screenshot review
- [ ] Documentation complete and up-to-date

### Long-term (12 months)
- [ ] 90% coverage of critical UI components
- [ ] Automated visual regression testing in place
- [ ] Integration with design tools (Figma)
- [ ] Team confidence in visual testing
- [ ] Zero design-related bugs in production
- [ ] Screenshot testing is standard practice

## Risk Management

### Potential Risks
- **Storage costs** - Git LFS can get expensive
- **CI time** - Tests may slow down CI pipeline
- **False positives** - Tests may fail incorrectly
- **Maintenance burden** - Keeping screenshots updated
- **Team adoption** - Developers may resist change

### Mitigation Strategies
- Use compression and size limits
- Optimize test execution and parallelize
- Fine-tune thresholds and document exceptions
- Automate updates and make it easy
- Training, documentation, and support

## Questions to Answer

### Technical
- [ ] What's the optimal resizeScale for our screenshots?
- [ ] Should we test iOS screenshots separately?
- [ ] How to handle dynamic content (dates, times)?
- [ ] Best way to test animations?
- [ ] How to handle flaky tests?

### Process
- [ ] Who approves screenshot changes?
- [ ] When should screenshots be updated?
- [ ] How often to run full test suite?
- [ ] What's the review process?
- [ ] How to handle conflicts?

### Team
- [ ] How to onboard new developers?
- [ ] What training is needed?
- [ ] How to get buy-in from designers?
- [ ] How to measure success?
- [ ] How to celebrate wins?

---

**Last Updated:** 2025-01-08
**Owner:** Android Team
**Status:** In Progress

**Next Review:** Week of 2025-01-15

