# Release Process

---

## Versioning

Semantic Versioning: `MAJOR.MINOR.PATCH`

| Segment | When to increment |
|---|---|
| `MAJOR` | Breaking change to user data (e.g., Room migration that is not backward-compatible, complete UX overhaul). For a consumer app, reserve for complete redesigns or data format breaks. |
| `MINOR` | New user-facing feature added in a backward-compatible way (e.g., new preset system, new curve export feature). |
| `PATCH` | Bug fix, crash fix, performance improvement, or copy change. No new features. |

Version code (Android `versionCode`) must always increment by 1 for every release to any distribution channel, including betas.

Examples:

```
1.0.0 (versionCode 1)  — Initial public release
1.0.1 (versionCode 2)  — Crash fix for Samsung devices
1.1.0 (versionCode 3)  — Presets feature added
2.0.0 (versionCode 4)  — Full UI redesign
```

---

## Branches

| Branch | Purpose |
|---|---|
| `main` | Always reflects the latest stable release. Only merged into from `develop` (via release cut) or `hotfix/*`. Never commit directly. |
| `develop` | Active development branch. All feature branches merge here. |
| `feature/<name>` | Individual feature or improvement work. Branches from `develop`, merges back to `develop`. |
| `hotfix/<name>` | Critical bug fixes for production. Branches from `main`, merges to both `main` and `develop`. |
| `release/<version>` | Release stabilisation branch. Created from `develop` when a release is nearing completion. Only bug fixes allowed. |

Branch naming examples:
```
feature/curve-editor
feature/overlay-controller
hotfix/samsung-crash-1.0.1
release/1.1.0
```

---

## Release Pipeline

```
1. Feature Complete on develop
       ↓
2. Cut release/<version> branch from develop
       ↓
3. All unit and integration tests pass (CI green)
       ↓
4. Manual QA pass against checklist in TESTING.md
       ↓
5. Update CHANGELOG.md (move Unreleased → version heading)
       ↓
6. Update versionName and versionCode in build config
       ↓
7. Beta release to GitHub Releases (pre-release tag) + F-Droid testing track
       ↓
8. Community testing period (minimum 1 week for minor releases, 2 weeks for major)
       ↓
9. Address critical beta feedback
       ↓
10. Merge release/<version> into main and tag: v1.x.x
        ↓
11. Merge release/<version> back into develop
        ↓
12. Stable release: Google Play + GitHub Releases + F-Droid submission
```

---

## Changelog Management

This project uses [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) format maintained in `CHANGELOG.md`.

### Rules

* Every merged pull request that affects user-facing behaviour must include a `CHANGELOG.md` update.
* Changes accumulate under the `[Unreleased]` heading during development.
* At release time, `[Unreleased]` is renamed to the version heading with its date, and a new empty `[Unreleased]` section is created above it.
* Never delete old changelog entries.

### Change Categories

| Category | Use for |
|---|---|
| `Added` | New features. |
| `Changed` | Changes to existing behaviour. |
| `Deprecated` | Features that will be removed in a future release. |
| `Removed` | Features that have been removed. |
| `Fixed` | Bug fixes. |
| `Security` | Security-related fixes. |

---

## Distribution

### Google Play

* Primary distribution channel.
* Use Play App Signing.
* Maintain a What's New text file (500 character limit) per release.
* Target the latest stable `targetSdk` at all times.

### GitHub Releases

* Every release (beta and stable) gets a GitHub Release tag.
* Attach the signed APK as a release asset.
* Release notes are sourced directly from the relevant `CHANGELOG.md` section.
* Beta releases are marked as pre-release in GitHub.

### F-Droid

F-Droid builds the app from source. It does not accept pre-built APKs.

Required setup in the repository:

```
fastlane/
  metadata/
    android/
      en-US/
        title.txt               (app name, max 30 chars)
        short_description.txt   (max 80 chars)
        full_description.txt    (max 4000 chars)
        changelogs/
          <versionCode>.txt     (max 500 chars, one file per release)
        images/
          icon.png
          featureGraphic.png
          phoneScreenshots/     (at least 2 screenshots)
```

The F-Droid build recipe will be maintained in the [F-Droid data repository](https://gitlab.com/fdroid/fdroiddata). It must specify the exact Gradle build command and the signing-free build variant.

---

## Signing

* Use Google Play App Signing for all Play Store releases.
* The upload keystore is generated locally by the release manager and stored securely outside the repository (e.g., an encrypted password manager or a secure local backup).
* Never commit any `.jks`, `.keystore`, or `keystore.properties` files to the repository.
* The `.gitignore` must explicitly exclude these file types.
* F-Droid builds are unsigned at source level — F-Droid applies its own signing. The app must build successfully with no signing configuration present.

---

## Pre-Release Checklist

Before tagging any release:

- [ ] `CHANGELOG.md` updated with all changes since last release
- [ ] `versionName` and `versionCode` updated in build configuration
- [ ] All CI checks passing on the release branch
- [ ] Manual QA checklist completed (see TESTING.md)
- [ ] F-Droid `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` created
- [ ] GitHub Release draft prepared with changelog notes
- [ ] Signing keystore confirmed available (not in repo)
- [ ] `targetSdk` is current stable Android API level
