# Store Listing QA Checklist

Run this checklist for every app, locale and device class.

## Capture identity

- [ ] Build commit and app version are recorded
- [ ] Exactly one target iOS simulator is booted while Maestro runs
- [ ] Maestro interaction command includes the target UDID
- [ ] Final iOS pixels come from `xcrun simctl io <UDID> screenshot`
- [ ] Android interaction and screenshot commands include the target serial
- [ ] Every raw capture matches the native dimensions in `listing-qa.json`
- [ ] No source capture is reused for two distinct panels

## App state

- [ ] Canonical labels, routes, stops and saved items match across platforms
- [ ] Product theme and light or dark appearance match panel intent
- [ ] GPS is pinned to the product's canonical location
- [ ] Required permissions are granted before capture
- [ ] No permission banner, dialog, keyboard, loading state or error is visible
- [ ] Timetable proof shows one expanded journey
- [ ] Service-alert proof shows the intended card expanded
- [ ] The app is returned to a non-polling screen after capture

## Copy

- [ ] One benefit is communicated per panel
- [ ] The claim is visible or defensible from the app screen
- [ ] Headline has at most seven words
- [ ] Subline has at most eight words
- [ ] No headline, subline, sticker or ghost label contains a full stop
- [ ] No technical language is used when ordinary language is available
- [ ] No unsupported price, ranking, usage or performance claim is present
- [ ] Text-bearing artwork is localized with the listing

## Layout

- [ ] Device class is authentic and not resized from another platform capture
- [ ] Full app screen is visible without accidental clipping
- [ ] Foreground content stays inside the safe border
- [ ] Empty colour field remains visible around the device
- [ ] Device slot starts and ends consistently across the carousel
- [ ] Phone and iPad copy is centred and limited to two headline lines
- [ ] Landscape tablet copy and device are aligned as one composition
- [ ] Sticker or badge does not touch or overlap headline copy
- [ ] Longest word fits at desktop and mobile report widths

## Output

- [ ] Exact store pixel dimensions pass `verify-listing.py`
- [ ] PNGs are opaque and under the configured byte limit
- [ ] Panel count and numeric order match the report
- [ ] Every output is reviewed at full size
- [ ] A contact sheet is reviewed across all platforms
- [ ] Dated upload folders contain only approved submission files
- [ ] Raw captures, flows, report, generated panels and upload files are committed

## Scoring and policy evidence

- [ ] Every visible score has an immediate explanation directly below it
- [ ] Every platform has at least three specific statements describing what passed
- [ ] Every deduction records points, category, exact issue, impact, improvement and store risk
- [ ] Quality or parity deductions are not described as policy violations
- [ ] Policy warnings identify the exact asset, likely review outcome and fallback
- [ ] Potential rejection blockers state their trigger and required remediation
- [ ] Every policy claim links to an official Apple or Google source with a checked date
- [ ] Official quotations remain short and are clearly attributed
- [ ] Any active blocker fails automated validation and blocks upload

Any unchecked capture-identity, app-state, layout, output, or active policy-blocker item blocks upload.
