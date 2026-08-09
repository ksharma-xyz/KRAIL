# Store Listing Agent Guide

This file is the handoff contract for future coding-agent sessions. Do not rely
on previous chat context. Read these files in order:

1. `store-listing/framework/README.md`
2. `store-listing/framework/QA-CHECKLIST.md`
3. `store-listing/<app>/README.md`
4. `store-listing/<app>/listing-qa.json`
5. `store-listing/<app>/manifest.json`

## Required behaviour

- Inspect the existing report and all raw captures before changing copy or layout
- Preserve app-specific canonical routes, labels, themes and data
- Use committed Maestro flows for state arrangement
- Pin device identifiers in every automation command
- Keep exactly one iOS simulator booted and use `simctl` for final pixels
- Verify source dimensions immediately after every capture
- Generate a horizontal report row for every supported device class
- Make device class the prominent section heading; store and count are secondary
- Display a QA score and its exact deductions for every platform
- Keep foreground content inside safe borders with visible field around devices
- Enforce headline, subline, punctuation and alignment limits from the framework
- Render all store-size outputs after any report-copy or source-capture change
- Run `verify-listing.py` and visually inspect full-size outputs before completion
- Return live timetable screens to a non-polling state or stop the emulator
- Commit raw captures, flow changes, report, generated assets and upload files

## Feedback loop

When the reviewer comments on a panel:

1. Translate the comment into a reusable rule when it applies beyond one app
2. Update app-specific copy, source state or styling
3. Add or strengthen an automated check where possible
4. Rerender every affected platform, not only the reported image
5. Compare phone, tablet, iPhone and iPad together
6. Update scores and deductions honestly
7. Commit the checkpoint before moving to another class of change

## Report generation contract

The app report is a generated/reproducible project artifact, not an informal
mockup. It must contain:

- one prominent section per device class
- a horizontal carousel of full panel previews
- direct links to native source captures
- platform score and deductions
- canonical capture baseline
- parity findings and unresolved recaptures
- store submission readiness and source links

The app renderer may be a thin adapter while this framework remains embedded in
the product repository. It must consume the report/configuration and produce
deterministic PNG filenames, exact dimensions, an output index and a dated
upload directory. When this framework moves to its own repository, app adapters
should retain only their manifest, capture flows and app-specific content.

## Non-negotiable blockers

Do not call an asset ready when any of these is true:

- native source dimensions identify a different device class
- a permission banner, system dialog, keyboard, loading state or error is visible
- copy touches a badge, screen edge or another text block
- the app screen is cropped without an intentional documented reason
- a headline or subline contains a full stop
- a claim is not visible or defensible from the screenshot
- output size, opacity, count or byte limit fails validation
- the submitted build does not contain the shown feature or label
