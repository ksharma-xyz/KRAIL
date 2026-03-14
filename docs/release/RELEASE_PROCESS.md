# KRAIL Release Process

## Overview

```
main (versionName = 1.18.0)
  │
  │  ← development happens here
  │
  ├─ Start Android Release ──► prod/1.18.0 ──[fix]──[fix]
  │                                │            │      │
  │                             RC1 tag      RC2 tag  RC3 tag
  │                                │            │      │
  │                            GP Internal  GP Internal  GP Internal
  │
  │                           (happy with RC3? run Finalize)
  │
  │  ◄── Finalize ships v1.18.0 tag + GP Production + GitHub Release
  │  ◄── Finalize auto-bumps main to 1.19.0
  │
main (versionName = 1.19.0)   ← ready for next cycle
```

**Android** is fully automated. **iOS** is manual (Xcode → Archive → TestFlight) for now.

**Key principle**: `main` always carries the *next* version. `versionName` is bumped
automatically on `main` right after a release ships — you never bump it manually.

---

## Workflows at a Glance

| Workflow file | UI name | Trigger | What it does |
|---|---|---|---|
| `build.yml` | Krail App CI | Push to `main`, PRs | Quality gate + debug/release build + Firebase |
| `android-1-cut-release.yml` | Android: 1. Cut Release Branch | Manual | Reads version from `main`, creates `prod/{version}` branch |
| `android-2-deploy-rc.yml` | Android: 2. Deploy RC → Google Play Internal | Auto on `prod/**` push | Release build + RC tag + Google Play Internal |
| `android-3-ship-release.yml` | Android: 3. Ship to Production | Manual | Final tag + Google Play + GitHub Release + bumps `main` |
| `distribute-testflight.yml` | Distribute TestFlight | Manual | iOS TestFlight build + upload |

---

## Step-by-Step: Starting a Release

### 1. Kick off the release branch

`main` already has the correct `versionName` (bumped automatically when the previous
release shipped). Just go to **Actions → Start Android Release → Run workflow**.

| Field | Example |
|---|---|
| Base branch | `main` (default) |

This workflow:
- Reads `versionName` from `main`'s `build.gradle.kts` (e.g. `1.19.0`)
- Creates and pushes `prod/1.19.0`
- Pushing the branch automatically triggers `android-2-deploy-rc.yml`

No version input needed. No version bump happens here.

### 2. Watch the automatic release pipeline

`android-2-deploy-rc.yml` runs on every push to `prod/**`:

```
code-quality
    └── build-android-release (signed AAB)
            └── tag-release-candidate  →  creates v1.19.0-RC1
                    └── distribute-google-play  →  uploads to GP Internal track
```

The first push creates `v1.19.0-RC1`. Every subsequent push to the same branch
increments the counter: `v1.19.0-RC2`, `v1.19.0-RC3`, etc.

### 3. Test on Google Play Internal

- Install via Google Play internal track
- Validate the build
- If a fix is needed: commit to `prod/1.19.0` and push → automatic RC bump + new GP upload

---

## Step-by-Step: Shipping to Production

Once internal testing passes, run **Actions → Finalize Android Release → Run workflow**.

| Field | Example |
|---|---|
| Version | `1.19.0` |
| Google Play track | `production` (or `beta` for staged rollout) |
| Next version | *(leave blank)* auto-increments minor: `1.20.0` |

This workflow:
1. Builds a fresh signed AAB from `prod/1.19.0`
2. Creates and pushes the final `v1.19.0` tag
3. Uploads the AAB to the chosen Google Play track
4. Creates a **draft** GitHub Release with auto-generated notes
5. Bumps `versionName` on `main` to `1.20.0` (commit tagged `[skip ci]`)

After it finishes:
- Review and publish the GitHub Release draft

---

## Step-by-Step: iOS Release (Manual)

iOS is archived and shipped manually via Xcode for now.

1. Checkout `prod/{version}` locally
2. Open `iosApp/iosApp.xcworkspace` in Xcode
3. Product → Archive
4. Distribute App → App Store Connect → Upload
5. Go to App Store Connect → TestFlight → promote build to external testers
6. When ready: submit for App Store review from App Store Connect

> TestFlight can also be triggered via **Actions → Manual Build & Distribute TestFlight**
> if the GitHub Actions iOS signing setup is working.

---

## Branching Convention

| Branch | Purpose | CI/CD triggered |
|---|---|---|
| `main` | Active development | Build + Firebase distribution |
| `prod/{version}` | Release stabilisation | Build + RC tag + Google Play Internal |
| `{date}-{description}` | Feature/fix branches | Nothing (only on PR to main) |

**Never commit directly to `main` for a release.** Always go through a `prod/*` branch so
the RC tagging and Google Play upload pipeline runs.

---

## Versioning

### Android

| Property | Where | Who updates it |
|---|---|---|
| `versionName` | `androidApp/build.gradle.kts` | `finalize-android-2-deploy-rc.yml` (auto-bumps `main` after shipping) |
| `versionCode` | GitHub repo variable `ANDROID_VERSION_CODE` | CI (increments on every release build) |

Each release build reads `ANDROID_VERSION_CODE`, increments it by 1, writes the new
value back to the variable, and passes it to Gradle. Debug builds read the value
without incrementing. The counter survives workflow renames because it lives in GitHub
Settings, not inside any workflow file. See [One-Time Setup](#one-time-setup-android_version_code-variable).

### iOS

| Property | Where | Who updates it |
|---|---|---|
| `CFBundleShortVersionString` | `iosApp/iosApp/Info.plist` | `android-1-cut-release.yml` (automated) |
| `CFBundleVersion` | `iosApp/iosApp/Info.plist` | Manual / Xcode archive |

### Git tags

| Pattern | Meaning | Created by |
|---|---|---|
| `v1.19.0-RC1` | First release candidate | `release.yml` (automatic) |
| `v1.19.0-RC2` | Second RC after a fix | `release.yml` (automatic) |
| `v1.19.0` | Final production release | `android-3-ship-release.yml` (manual) |

---

## Google Play Tracks

| Track | Use for | How to target |
|---|---|---|
| `internal` | Team dogfooding during RC cycle | Automatic via `release.yml` |
| `alpha` | Closed group testing | `android-3-ship-release.yml` → choose `alpha` |
| `beta` | Open beta / staged rollout | `android-3-ship-release.yml` → choose `beta` |
| `production` | Full release | `android-3-ship-release.yml` → choose `production` |

---

## Hotfixes During RC / After Shipping

**Fix flow: `main` first, then cherry-pick to `prod/*`.**

Never commit a fix directly only to the prod branch — it would be lost when the next
release cycle starts from `main`.

1. Fix the bug on `main` (commit normally, goes through PR/review as usual)
2. Cherry-pick the fix commit onto the prod branch:
   ```
   git cherry-pick <commit-sha>
   git push origin prod/1.19.0
   ```
3. Push to `prod/1.19.0` triggers `android-2-deploy-rc.yml` → new RC tag → GP Internal
4. Validate, then run `finalize-android-2-deploy-rc.yml` when ready

**Patch release** (e.g. a critical fix after `v1.19.0` is already in production):

1. Fix on `main` first
2. Run **Start Android Release** — it reads the current `versionName` from `main`
   (already `1.20.0` if the previous release auto-bumped it)
3. If you need a `1.19.x` patch instead, manually create `prod/1.19.1` from the
   fix commit and set `versionName = "1.19.1"` in `build.gradle.kts` on that branch

---

## Checklist Before Running `android-3-ship-release.yml`

- [ ] Latest RC has been installed from Google Play Internal and validated
- [ ] All required commits are on `prod/{version}` (no pending fixes)
- [ ] Release notes / changelog reviewed
- [ ] iOS build archived and submitted to TestFlight (if releasing iOS simultaneously)
- [ ] Decided on Google Play rollout track (`production` for full, `beta` for staged)

---

## One-Time Setup: ANDROID_VERSION_CODE Variable

`versionCode` is stored as a **GitHub repository variable** (not a secret) so it
persists across any workflow renames or restructuring. `github.run_number` is NOT
used for this — it would reset to 1 whenever a workflow file is renamed.

**How to initialise it (do this once):**

1. Open Google Play Console → your app → a recent internal build → note the versionCode
2. Go to GitHub → Settings → Secrets and variables → Actions → **Variables** tab
3. Click **New repository variable**
   - Name: `ANDROID_VERSION_CODE`
   - Value: *(last deployed versionCode + 10, as a buffer)*
4. Done — all future release builds will auto-increment this and write it back

**How it works at build time:**
- Release build: reads the var, adds 1, writes the new value back via GitHub API, uses it
- Debug build: reads the var without modifying it (debug APKs never go to Google Play)
- Gaps are fine — Google Play only requires each upload is strictly greater than the last

---

## Required GitHub Secrets

All secrets are already configured. Reference only:

| Secret | Used by |
|---|---|
| `PAT_KRAIL_GITHUB` | RC/final tag creation, GitHub Release |
| `ANDROID_KEYSTORE_FILE` | Android AAB signing |
| `ANDROID_KEYSTORE_PASSWORD` | Android AAB signing |
| `ANDROID_KEY_ALIAS` | Android AAB signing |
| `ANDROID_KEY_PASSWORD` | Android AAB signing |
| `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` | Google Play upload |
| `FIREBASE_GOOGLE_SERVICES_JSON_RELEASE` | Firebase (release) |
| `APPSTORE_PRIVATE_KEY` | iOS TestFlight |
| `APPSTORE_KEY_ID` | iOS TestFlight |
| `APPSTORE_ISSUER_ID` | iOS TestFlight |

---

## Troubleshooting

**RC tag already pushed but Google Play upload failed**
The RC tag is already created — just re-run only the `distribute-google-play` job
from the failed workflow run (GitHub Actions → re-run failed jobs).

**`android-1-cut-release.yml` fails with "branch already exists"**
The branch `prod/{version}` was already created. Either push directly to that branch
or choose a new version number.

**`android-3-ship-release.yml` fails with "tag already exists"**
`v{version}` was already tagged. Check the existing tag — if it was a mistake, delete
it manually (`git push origin :refs/tags/v{version}`) then re-run.

**versionName in build not matching expected version**
`android-1-cut-release.yml` commits the bump. If you created the branch manually without
running `android-1-cut-release.yml`, update `versionName` in `androidApp/build.gradle.kts`
and `iosApp/iosApp/Info.plist` by hand and push.
