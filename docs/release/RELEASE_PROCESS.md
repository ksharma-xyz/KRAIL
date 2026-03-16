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
  │                            TF build     TF build     TF build
  │
  │                           (happy with RC3? run Finalize)
  │
  │  ◄── Finalize ships v1.18.0 tag + GP Production + GitHub Release
  │  ◄── Finalize auto-bumps main to 1.19.0
  │
main (versionName = 1.19.0)   ← ready for next cycle
```

Both **Android** and **iOS** are fully automated on every push to `prod/**`.

**Key principle**: `main` always carries the *next* version. `versionName` is bumped
automatically on `main` right after a release ships — you never bump it manually.

---

## Workflows at a Glance

| Workflow file | UI name | Trigger | What it does |
|---|---|---|---|
| `build.yml` | Krail App CI | Push to `main`, PRs | Quality gate + debug/release build + Firebase |
| `release-1-cut.yml` | 1. Cut Release Branch | Manual | Reads version from `main`, creates `prod/{version}` branch |
| `release-2-deploy-rc.yml` | 2. Deploy RC | Auto on `prod/**` push | Android RC tag + Google Play Internal + iOS TestFlight |
| `release-3-ship.yml` | 3. Ship to Production | Manual | Final tag + Google Play + GitHub Release + bumps `main` |
| `distribute-testflight.yml` | Distribute TestFlight | Auto (called by release-2) or Manual | iOS build + TestFlight upload |

---

## Step-by-Step: Starting a Release

### 1. Kick off the release branch

`main` already has the correct `versionName` (bumped automatically when the previous
release shipped). Just go to **Actions → Android: 1. Cut Release Branch → Run workflow**.

| Field | Example |
|---|---|
| Base branch | `main` (default) |

This workflow:
- Reads `versionName` from `main`'s `build.gradle.kts` (e.g. `1.19.0`)
- Creates and pushes `prod/1.19.0`
- Pushing the branch automatically triggers `release-2-deploy-rc.yml`

No version input needed. No version bump happens here.

### 2. Watch the automatic release pipeline

`release-2-deploy-rc.yml` runs on every push to `prod/**`:

```
code-quality
    └── build-android-release (signed AAB)
            └── tag-release-candidate  →  creates v1.19.0-RC1
                    └── distribute-google-play  →  uploads to GP Internal track

distribute-testflight  →  builds IPA + uploads to TestFlight  (runs in parallel)
```

The first push creates `v1.19.0-RC1`. Every subsequent push to the same branch
increments the counter: `v1.19.0-RC2`, `v1.19.0-RC3`, etc.

### 3. Test on Google Play Internal and TestFlight

- Install via Google Play internal track and TestFlight
- Validate both Android and iOS builds
- If a fix is needed: commit to `prod/1.19.0` and push → automatic RC bump + new GP + TF upload

---

## Step-by-Step: Shipping to Production

Once internal testing passes, run **Actions → Android: 3. Ship to Production → Run workflow**.

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
- Submit the latest TestFlight build for App Store review from App Store Connect

---

## Step-by-Step: iOS App Store Submission

TestFlight upload is automatic. App Store submission is manual from App Store Connect:

1. Go to **App Store Connect → TestFlight** and verify the latest build
2. Go to **App → App Store →+ Version** → select the TestFlight build
3. Submit for App Store review

---

## Branching Convention

| Branch | Purpose | CI/CD triggered |
|---|---|---|
| `main` | Active development | Build + Firebase distribution |
| `prod/{version}` | Release stabilisation | Android RC + Google Play Internal + iOS TestFlight |
| `{date}-{description}` | Feature/fix branches | Nothing (only on PR to main) |

**Never commit directly to `main` for a release.** Always go through a `prod/*` branch so
the RC tagging and Google Play upload pipeline runs.

---

## Versioning

### Android

| Property | Where | Who updates it |
|---|---|---|
| `versionName` | `androidApp/build.gradle.kts` | `release-3-ship.yml` (auto-bumps `main` after shipping) |
| `versionCode` | GitHub repo variable `ANDROID_VERSION_CODE` | CI (increments on every release build) |

Each release build reads `ANDROID_VERSION_CODE`, increments it by 1, writes the new
value back to the variable, and passes it to Gradle. Debug builds read the value
without incrementing. The counter survives workflow renames because it lives in GitHub
Settings, not inside any workflow file.

### iOS

| Property | Where | Who updates it |
|---|---|---|
| `CFBundleShortVersionString` | `iosApp/iosApp/Info.plist` | `release-1-cut.yml` (automated) |
| `CFBundleVersion` | GitHub repo variable `IOS_BUILD_NUMBER` | CI (increments on every TestFlight upload) |

`CFBundleVersion` must be globally and strictly increasing across all App Store / TestFlight
uploads for this app — it does **not** reset when the marketing version changes.

### Git tags

| Pattern | Meaning | Created by |
|---|---|---|
| `v1.19.0-RC1` | First release candidate | `release-2-deploy-rc.yml` (automatic) |
| `v1.19.0-RC2` | Second RC after a fix | `release-2-deploy-rc.yml` (automatic) |
| `v1.19.0` | Final production release | `release-3-ship.yml` (manual) |

---

## Google Play Tracks

| Track | Use for | How to target |
|---|---|---|
| `internal` | Team dogfooding during RC cycle | Automatic via `release-2-deploy-rc.yml` |
| `alpha` | Closed group testing | `release-3-ship.yml` → choose `alpha` |
| `beta` | Open beta / staged rollout | `release-3-ship.yml` → choose `beta` |
| `production` | Full release | `release-3-ship.yml` → choose `production` |

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
3. Push to `prod/1.19.0` triggers `release-2-deploy-rc.yml` → new RC tag → GP Internal + TestFlight
4. Validate on both platforms, then run `release-3-ship.yml` when ready

**Patch release** (e.g. a critical fix after `v1.19.0` is already in production):

1. Fix on `main` first
2. Run **1. Cut Release Branch** — it reads the current `versionName` from `main`
   (already `1.20.0` if the previous release auto-bumped it)
3. If you need a `1.19.x` patch instead, manually create `prod/1.19.1` from the
   fix commit and set `versionName = "1.19.1"` in `build.gradle.kts` on that branch

---

## Checklist Before Running `release-3-ship.yml`

- [ ] Latest RC has been installed from Google Play Internal and validated
- [ ] Latest TestFlight build validated on iOS
- [ ] All required commits are on `prod/{version}` (no pending fixes)
- [ ] Release notes / changelog reviewed
- [ ] Decided on Google Play rollout track (`production` for full, `beta` for staged)

---

## One-Time Setup: GitHub Repository Variables

Both `versionCode` (Android) and `CFBundleVersion` (iOS) are stored as **GitHub repository
variables** so they persist across workflow renames and file restructures.

### ANDROID_VERSION_CODE

1. Open Google Play Console → your app → a recent internal build → note the versionCode
2. Go to GitHub → Settings → Secrets and variables → Actions → **Variables** tab
3. Click **New repository variable**
   - Name: `ANDROID_VERSION_CODE`
   - Value: *(last deployed versionCode + 10, as a buffer)*
4. Done — all future release builds will auto-increment this and write it back

### IOS_BUILD_NUMBER

1. Open App Store Connect → your app → note the highest CFBundleVersion ever uploaded
2. Go to GitHub → Settings → Secrets and variables → Actions → **Variables** tab
3. Click **New repository variable**
   - Name: `IOS_BUILD_NUMBER`
   - Value: *(last uploaded build number + 10, as a buffer)*
4. Done — all future TestFlight uploads will auto-increment this and write it back

**How both work at build time:**
- Reads the variable, adds 1, writes the new value back via GitHub API, uses it for the build
- Gaps are fine — both Google Play and App Store only require each upload to be strictly greater than the last

---

## Required GitHub Secrets

All secrets are already configured. Reference only:

| Secret | Used by |
|---|---|
| `PAT_KRAIL_GITHUB` | RC/final tag creation, GitHub Release, version code write-back |
| `ANDROID_KEYSTORE_FILE` | Android AAB signing |
| `ANDROID_KEYSTORE_PASSWORD` | Android AAB signing |
| `ANDROID_KEY_ALIAS` | Android AAB signing |
| `ANDROID_KEY_PASSWORD` | Android AAB signing |
| `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` | Google Play upload |
| `FIREBASE_GOOGLE_SERVICES_JSON_RELEASE` | Firebase (release) |
| `APPSTORE_PRIVATE_KEY` | iOS TestFlight |
| `APPSTORE_KEY_ID` | iOS TestFlight |
| `APPSTORE_ISSUER_ID` | iOS TestFlight |
| `IOS_DIST_SIGNING_KEY_BASE64` | iOS code signing certificate |
| `IOS_DIST_SIGNING_KEY_PASSWORD` | iOS code signing certificate |
| `IOS_PROVISIONING_PROFILE_NAME` | iOS provisioning profile |
| `FIREBASE_IOS_GOOGLE_INFO` | Firebase iOS |

## Required GitHub Variables

| Variable | Used by |
|---|---|
| `ANDROID_VERSION_CODE` | Android versionCode (auto-incremented) |
| `IOS_BUILD_NUMBER` | iOS CFBundleVersion (auto-incremented) |
| `DEVELOPMENT_TEAM` | iOS Xcode signing (Apple Team ID) |
| `APPLE_TEAM_ID` | iOS provisioning profile download |

---

## Troubleshooting

**RC tag already pushed but Google Play upload failed**
The RC tag is already created — just re-run only the `distribute-google-play` job
from the failed workflow run (GitHub Actions → re-run failed jobs).

**`release-1-cut.yml` fails with "branch already exists"**
The branch `prod/{version}` was already created. Either push directly to that branch
or choose a new version number.

**`release-3-ship.yml` fails with "tag already exists"**
`v{version}` was already tagged. Check the existing tag — if it was a mistake, delete
it manually (`git push origin :refs/tags/v{version}`) then re-run.

**versionName in build not matching expected version**
`release-1-cut.yml` commits the bump. If you created the branch manually without
running `release-1-cut.yml`, update `versionName` in `androidApp/build.gradle.kts`
and `iosApp/iosApp/Info.plist` by hand and push.

**TestFlight upload rejected: bundle version must be higher**
The `IOS_BUILD_NUMBER` variable is behind the last uploaded build. Update the variable
in GitHub → Settings → Variables to a value higher than the last uploaded `CFBundleVersion`.

---

## CI/CD Status

### Android — ✅ Confirmed working (tested 2026-03-15)

End-to-end test on `prod/0.0.1-test` ([run #23094566868](https://github.com/ksharma-xyz/KRAIL/actions/runs/23094566868)):

| Job | Result |
|---|---|
| code-quality (Detekt) | ✅ passed |
| build-android-release (signed AAB) | ✅ passed |
| tag-release-candidate | ✅ created `v0.0.1-test-RC1` |
| distribute-google-play (Internal) | ✅ uploaded |

**Pending before June 2, 2026**: update two actions away from Node.js 20:
- `filippoLeporati93/android-release-signer@v1`
- `r0adkll/upload-google-play@v1`

### iOS TestFlight — ✅ Confirmed working (tested 2026-03-16)

End-to-end test on `03-14-autoamte_release_branch_ci___cd_strategy` ([run #23155733162](https://github.com/ksharma-xyz/KRAIL/actions/runs/23155733162)):

| Job | Result |
|---|---|
| Import Code-Signing Certificates | ✅ passed |
| Download Provisioning Profiles | ✅ passed |
| Resolve Swift Packages | ✅ passed |
| Build iOS App for Release | ✅ passed |
| Upload to TestFlight | ✅ passed |

Key fixes applied:
- Pinned Xcode to `~16` (latest-stable picked up pre-release Xcode 26.3)
- Separated SPM resolution step before Fastlane to avoid signing conflicts
- Used `update_code_signing_settings` to scope provisioning profile to `iosApp` target only (prevents SPM packages from rejecting it)
- Pre-build MapLibre via Gradle (IosArm64) before Xcode archive
