# KRAIL Store Listing Decisions

This is the durable feedback record for the approved 2026-08-09 KRAIL store
listing set. Future sessions must read it before changing captures, copy,
composition or upload folders. The rendered report remains the visual source of
truth; `manifest.json` is its machine-readable seven-panel contract.

## What worked

- Use real native app captures instead of generated, reconstructed or generic imagery
- Lead with a seven-panel benefit story: Saved Trips, Live Times, Parking, Service Alerts, Live Delays, Planning and Dark Mode
- Make the device class the prominent report heading and keep store/count metadata secondary
- Show genuine phone, Android tablet and iPad layouts rather than scaling one device class into another
- Use KRAIL product colours as a sequence: train orange, bus blue, Metro teal, alert amber, Light Rail red, ferry green and a dark finish
- Keep the default appearance light and reserve genuine dark mode for the final panel
- Expand the route to prove the complete journey and expand the first service alert on tablet and iOS
- Keep canonical labels, routes, parking data and themes consistent across platforms
- Prefer concise everyday language such as `Start to finish` over technical transport terms such as `leg`
- Accent one useful word or phrase with colour while preserving strong contrast
- Vary the circular ring position between panels so the composition does not feel stamped out
- Integrate the white Metro-coloured `P` badge with the parking headline alignment
- Show a platform score only with passed checks, deductions, fixes and store-review risk
- Use one horizontal HTML report to compare all four device classes together

## What did not work

- Squiggly or decorative underlines made the typography less legible and were removed
- Repeating `delays` across both panels 04 and 05 blurred the difference between service alerts and live delays
- `Live times every leg` was technical and unclear for everyday passengers
- Full stops in listing copy added visual noise and are forbidden by the QA contract
- Long parking copy and Tallawong-only claims understated KRAIL's broader Park and Ride coverage
- Text touching the Alerts or Any time pill made the hierarchy look accidental
- Placing the parking icon at the extreme left disconnected it from the title
- Putting the circular ring in the bottom-right of every panel made the set repetitive
- Phone UI presented as iPad or tablet evidence could be misleading and risk store rejection
- Dark mode across most panels reduced clarity; it is now one deliberate final proof point
- Location-permission banners made captures look unfinished and obscured product value
- Routes such as Schofields to Central contradicted the approved Bondi Junction to Circular Quay story
- Placeholder labels such as `Work 1` looked like test data and were removed
- Leaving a timetable screen open continued API polling after capture
- Generated or inferred app imagery was not acceptable when native captures were available

## Copy and layout rules

- One benefit and one visual proof per panel
- Headlines contain at most seven words, sublines at most eight words and neither uses a full stop
- Phone and iPad headlines use no more than two lines; landscape tablet copy uses one or two compact lines
- Portrait copy is centred above the app frame
- Landscape tablet copy aligns with the wider app composition and does not collide with pills or badges
- Foreground copy, badges and devices stay inside the safe border with visible colour around every device edge
- Badges have clear separation from copy; no label may touch or overlap another element
- The parking `P` badge sits directly before or above the title and follows the title alignment
- Decorative accents support hierarchy but never replace readable type or authentic app evidence

## Canonical capture state

- Journey: Bondi Junction Station to Circular Quay Station
- Service: route 333 with 15 stops and an expanded journey for the timetable panel
- Saved labels: Home to Work and Home to Uni
- Parking: Tallawong and Schofields, with Tallawong P1, P2 and P3 visible in detail
- Themes: train orange for Saved Trips, bus blue for Live Times, Metro teal for Parking, ferry green for Planning and train dark for Dark Mode
- Appearance: light except the explicit final dark-mode panel
- Location: permission granted with Sydney GPS at `-33.8688,151.2093`
- Alerts: first card expanded on Android tablet, iPhone and iPad
- Exit state: Saved Trips or a stopped emulator, never an actively polling timetable

## Device decisions

- Android phone: portrait 1080 x 1920 store panels from 1080 x 2400 native captures
- Android tablet: genuine landscape 1920 x 1080 output from 2560 x 1600 native tablet captures
- iPhone: accepted 1242 x 2688 6.5-inch assets using genuine iPhone captures
- iPad: accepted 2064 x 2752 13-inch assets using the native split-view app layout
- The Android tablet's added marketing copy has a documented Google large-screen crop warning; retain a UI-only fallback if Play Console previewing crops it

## Workflow and release decisions

- Commit raw captures, generated panels, report assets and upload files through Git LFS
- Treat dated `upload-ready/<date>/` folders as immutable release artifacts
- Upload store assets only from the matching dated `upload-ready` subfolder
- Boot one iOS simulator, pin its UDID and use `xcrun simctl io` for final pixels
- Use committed Maestro flows for repeatable state arrangement and assertions
- Rerender all affected platforms after source, copy or layout changes
- Run `verify-listing.py`, inspect full-size panels and compare all platforms before sign-off
- Record scores as review aids, with positives, exact deductions, remediation and policy impact
- Cite official Apple and Google policy with a checked date for any rejection-risk claim
- Commit checkpoints as work progresses and use one intentional Graphite/GitHub PR per coherent change

## Accepted non-blocking caveats

- Android phone panel 04 has a collapsed first alert; the claim remains visible and accurate
- Android phone Saved Trips contains an older parking state than the newer tablet and iOS captures
- Android tablet marketing copy may crop on some Google Play promotional surfaces

These are documented quality or presentation issues, not current upload
blockers. The mandatory release gate remains that the submitted build must
contain the screens, labels, themes and behaviour shown in the listing.
