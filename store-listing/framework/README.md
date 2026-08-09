# Store Listing Framework

App-agnostic groundwork for producing App Store and Google Play screenshot
sets. Application folders provide captures, copy, colours, device metadata and
automation flows. This folder owns reusable geometry, capture discipline and
validation.

The framework is intentionally kept separate from `../krail/` so it can move
to its own repository later without carrying KRAIL data or artwork.

## Repository contract

Each app keeps this structure:

```text
store-listing/<app>/
  README.md                    app-specific runbook and canonical data
  listing-qa.json              machine-readable QA contract and scores
  manifest.json                product story, palette and panel intent
  capture-flows/               committed Maestro state-arrangement flows
  screenshots/<device>/        raw, native, full-screen app captures
  <app>-screenshot-listing.html review report and panel source
  render-store-images.py       thin app renderer or framework adapter
  store-images/<device>/       generated reviewable store panels
  upload-ready/<date>/         immutable files arranged by store upload slot
```

Raw captures and final rendered PNGs are source artifacts and belong in Git
LFS. Scratch browser pages, simulator logs and temporary contact sheets do not.

## End-to-end workflow

1. Build and install the exact branch under review
2. Seed canonical labels, routes and test data
3. Boot only the simulator being captured
4. Set product theme, light or dark appearance, GPS and permissions
5. Use Maestro to arrange and assert the required app state
6. Use the platform-native screenshot command to capture the pinned device
7. Verify native source dimensions immediately
8. Generate the horizontal HTML review report and composed store panels
9. Run `verify-listing.py` before visual review or upload
10. Review every full-size output and a four-platform contact sheet
11. Copy approved outputs into a dated `upload-ready` directory
12. Commit raw captures, automation, report, generated panels and upload files

For iOS, Maestro must not be trusted to select the correct screenshot source
when multiple simulators are booted. Keep one iOS simulator booted, pin the
UDID for interaction, and take the final pixels with `xcrun simctl io`.
`capture-ios.sh` implements that rule.

## Panel design contract

One panel contains one benefit, one full app screen, one fixed text zone and a
small amount of supporting decoration. All panels in a device class use stable
geometry so text, device tops and device bottoms do not jump between panels.

1. Keep foreground content inside the configured safe border
2. Background texture may bleed; text, badges and devices may not
3. Preserve visible colour field around every device edge
4. Show the full app frame and never substitute another device class
5. Use one message and one visual proof per panel
6. Put the benefit first and keep the app evidence directly below it
7. Use no full stops in headlines, sublines, stickers or ghost labels
8. Use no more than seven headline words and eight subline words
9. Keep headlines to two lines on phones and iPad; use one or two on landscape
10. Never display a score without explaining the baseline, passed checks, deductions and fixes directly below it
11. Treat conversion quality, parity, policy warnings and upload blockers as separate severities
12. Quote store policy sparingly, link the official source and record when it was checked
10. Centre portrait copy; align landscape-tablet copy to its device composition
11. Keep badges outside the headline bounding box with a full line of clearance
12. Accent one meaningful word or phrase, consistently across device classes
13. Do not use decorative underlines when they reduce legibility
14. Do not capture permission prompts, banners, keyboards, loading states or errors
15. Distinct panels must use distinct source captures

`template.css`, `template.html` and `devices.json` define the reusable geometry.
Apps may tune sizes by device class, but must preserve safe borders and stable
slots.

## Device-specific placement

- Phone: centred copy, headline at most two lines, app frame dominant, at least
  6% horizontal safe space and visible field above and beside the frame
- iPad portrait: centred copy with a shorter text zone than phone, real iPad
  split-view evidence, badge isolated in the top corner
- Android tablet landscape: compact copy above the wide device, full map and
  details visible together, no phone capture enlarged into the slot
- Dark panels: capture the same canonical Saved Trips state, use the requested
  product theme, and assert that no location banner is visible

## Automated gate

Copy `project.example.json` into the app directory as `listing-qa.json`, fill in
the real paths and run:

```bash
python3 store-listing/framework/verify-listing.py \
  store-listing/<app>/listing-qa.json
```

The command validates source identity by native pixel size, rendered specs,
opacity, file size, counts, duplicate captures, copy limits, punctuation,
device headings and QA scores. Visual claims and crop quality still require a
human or screenshot-based visual review; they are represented as explicit
score deductions instead of being silently ignored. Each deduction must include
its category, visible impact, exact improvement and store-review risk. A report
must also show platform strengths, a policy verdict and conditional rejection
gates. Active blockers fail validation; warnings remain visible without being
misrepresented as guaranteed rejection.

See [QA-CHECKLIST.md](QA-CHECKLIST.md) for the release gate.
