# KRAIL Store Listing

This directory is the complete source, capture, review, render and upload
workspace for KRAIL store screenshots. The reusable rules and tools live in
`../framework/`; this directory contains only KRAIL content and configuration.

## Canonical story

- Primary journey: Bondi Junction Station to Circular Quay Station
- Expanded service: route 333 with 15 stops
- Saved labels: Home to Work and Home to Uni
- Park & Ride: Tallawong and Schofields; Tallawong detail shows P1, P2 and P3
- Themes: Train orange, Metro teal, Ferry green and Train dark
- Default appearance: light except the explicit dark-mode panel
- Location: Sydney, with permission granted and GPS set to `-33.8688,151.2093`

Do not introduce Schofields to Central, placeholder labels, arbitrary routes or
permission banners into this listing set.

## What is stored where

- `capture-flows/`: repeatable Maestro interactions and capture runbook
- `screenshots/`: raw native device captures; do not resize these
- `krail-screenshot-listing.html`: horizontal review report and panel source
- `DECISIONS.md`: approved feedback, rejected experiments and capture lessons
- `render-store-images.py`: renders exact store-size panels from the report
- `store-images/`: generated complete seven-panel sets for review
- `upload-ready/2026-08-09/`: approved files grouped by store upload target
- `listing-qa.json`: dimensions, filenames, copy rules and QA deductions
- `manifest.json`: approved seven-panel order, palette, intent and proof contract

Both raw captures and generated panels are committed through Git LFS. The
dated upload directory is intentionally immutable: make a new dated directory
for a later release rather than silently replacing a previously submitted set.
Future sessions must read `DECISIONS.md` before editing and keep the manifest,
report, QA configuration and generated filenames synchronized.

## Rebuild and verify

From the repository root:

```bash
python3 store-listing/krail/render-store-images.py
python3 store-listing/framework/verify-listing.py \
  store-listing/krail/listing-qa.json
open store-listing/krail/krail-screenshot-listing.html
```

`render-store-images.py` recreates `store-images/` and the dated upload folders.
Do not upload if the verifier fails or any full-size panel has not been visually
reviewed.

## Quality score

Scores start at 100 and subtract only documented limitations in
`listing-qa.json`. They are a review aid, not a prediction of store acceptance.
Dimensions, opacity, count, file size, source-device identity and copy rules are
automated. Claim accuracy, visual density and store merchandising caveats remain
explicit review deductions.
