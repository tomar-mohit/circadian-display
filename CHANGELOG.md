# Changelog

All notable changes to Circadian Display will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Fixed
- Clarified Native Mode's Android 10+ (API 29+) requirement in the Settings screen and split native-mode error messaging so API-level and ADB-permission issues are reported separately.
- Removed the unused `ACCESS_NETWORK_STATE` permission pulled in by WorkManager.

## [1.0.0] - 2026-09-06

### Added
- Project vision, product requirements, and architecture documentation.
- Architectural Decision Records (ADRs) for SDK target, dual-mode support, WorkManager, Room + DataStore, Hilt, and MVVM.
- Testing strategy with coverage targets, fake/mock approach, and full manual QA checklist.
- Release process documentation including branch strategy, changelog management, and F-Droid metadata requirements.
- Debugging guide with OEM-specific notes and known issue log.
- Contributing guidelines.
- Apache 2.0 License.
- Android project skeleton with multi-module Gradle setup (`app`, `core:curve`).
- Gradle version catalog (`libs.versions.toml`) for centralized dependency management.
- Core domain models: `CurvePoint`, `CurveProfile`, `DisplayState`, `DisplayMode`, `DisplayController` interface.
- `CurveEngine` with linear interpolation and full edge-case handling (empty, single point, clamping, duplicates, out-of-order).
- Comprehensive `CurveEngineTest` suite with 100% coverage of documented edge cases.
- GitHub Actions CI pipeline with build, test, and lint jobs.
- Adaptive launcher icon (crescent moon).
- ADR 007: Midnight wrap-around interpolation decision (linear, not circular).
- ADR 008: Native Mode API constraints (`WRITE_SECURE_SETTINGS` requirement, reflection-based access).
- Static analysis tooling: ktlint and detekt with project-specific configuration.
- `.editorconfig` for consistent code formatting across editors.
- Instrumented test dependencies (AndroidX Test, Compose UI Test, Room Testing, WorkManager Testing).
- MockK and Robolectric dependencies for unit testing Android-dependent code.
- Native Mode (`system:native` module): `NativeDisplayController` using `ColorDisplayManager` reflection with a `Settings.Secure` fallback, gated by `WRITE_SECURE_SETTINGS` (see Decision 008).
- Runtime display-mode routing via `CompositeDisplayController`, switching between overlay and native controllers based on the `mode` setting.
- Settings screen: Overlay/Native mode selector, overlay-permission and battery-optimization status with grant actions, and version/ADB instructions.
- Top-level navigation (`AppNavHost`) with Dashboard and Settings destinations.

### Changed
- CI pipeline now runs `ktlintCheck` and `detekt` before build.
- `.gitignore` now excludes `docs/Internal/` meta content.
- Active profile is now tracked solely via `CurveProfile.isActive` in Room; the redundant `active_profile_id` DataStore key was removed.

### Fixed
- `settings.gradle.kts`: Fixed `dependencyResolution` → `dependencyResolutionManagement` (build-breaking typo).
- Compose Compiler: Migrated from deprecated `composeOptions { kotlinCompilerExtensionVersion }` to the `org.jetbrains.kotlin.plugin.compose` Gradle plugin (required for Kotlin 2.0+).
- App theme: Changed parent from `android:Theme.Material.Light.NoActionBar` to `Theme.Material3.DayNight.NoActionBar` for proper Compose compatibility.
- `compileSdk` and `targetSdk` bumped from 34 to 35 (Android 15).
- ProGuard rules expanded to cover Coroutines, WorkManager, and domain model preservation.
- `ARCHITECTURE.md`: Removed `mode` from `DisplayState` documentation to match implementation (mode is determined by settings, not by the curve).
- `ARCHITECTURE.md`: Data flow sequence diagram updated to include `points` parameter in `calculateDisplayState` call and remove `mode` from return value.
- Overlay dimming now darkens the screen (black layer) instead of washing it out; warmth and dimming are applied as independent layers.

---

<!--
  HOW TO USE THIS FILE
  ====================
  - Every PR that changes user-facing behaviour must include a CHANGELOG.md update under [Unreleased].
  - At release time: rename [Unreleased] to [x.y.z] - YYYY-MM-DD, then add a fresh empty [Unreleased] above it.
  - Use these categories: Added | Changed | Deprecated | Removed | Fixed | Security.
  - Keep the categories in the fixed order above (Added first, Security last) and omit empty ones.
  - Add new entries to the TOP of their category (newest first).
  - Keep entries short and written from the user's perspective, not the implementer's.
    Good:  "Fixed overlay not reappearing after returning from an excluded app on Samsung devices."
    Bad:   "Fixed null pointer exception in OverlayDisplayController.onForegroundAppChanged()."
-->
