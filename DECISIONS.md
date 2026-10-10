# Architectural Decisions Log

This file records significant technical and product decisions made throughout the life of the project.

Purpose:

* Preserve context
* Explain tradeoffs
* Avoid repeating old discussions
* Help contributors understand why choices were made

Each entry should include:

* Decision ID
* Date
* Status
* Context
* Decision
* Consequences

Template:

---

Decision ID: XXX

Date:

Status:
Proposed | Accepted | Deprecated | Superseded

Context:

What problem or situation led to this decision?

Decision:

What was chosen?

Consequences:

What are the benefits, costs, or limitations?

---

# Decision 001

Date:
2026-06-04

Status:
Accepted

Context:

The project targets modern Android devices and aims to minimize maintenance burden while supporting a large portion of active Android users.

Decision:

Minimum SDK version is Android 8.0 (API 26).

Consequences:

Benefits:

* Cleaner APIs
* Simpler implementation
* Reduced compatibility testing
* Better support for modern Android components

Costs:

* Some older devices are not supported

Rationale:

This project prioritizes maintainability and developer productivity over maximum device coverage.

---

# Decision 002

Date:
2026-06-04

Status:
Accepted

Context:

Many Android devices already provide built-in night light functionality, but these implementations vary by manufacturer and often lack advanced control.

Decision:

Support both Native Mode and Overlay Mode.

Consequences:

Benefits:

* Native mode offers better compatibility
* Overlay mode offers greater flexibility
* Users can choose their preferred tradeoff

Costs:

* Additional implementation complexity
* More testing scenarios

Rationale:

Flexibility is a core project goal.

---

# Decision 003

Date:
2026-06-04

Status:
Accepted

Context:

The scheduler must periodically evaluate the active curve and update the display state in the background. The app cannot require the user to manually open it for adjustments to take effect. Three background execution strategies were evaluated: WorkManager PeriodicWorkRequest, AlarmManager (exact), and a persistent Foreground Service with an internal timer.

Decision:

WorkManager PeriodicWorkRequest with a ~15-minute repeat interval, combined with a BOOT_COMPLETED BroadcastReceiver to re-enqueue work after device restart.

Consequences:

Benefits:

* Battery-efficient — system-managed execution respects Doze mode and power-saving states.
* Survives process death — WorkManager state is persisted in its own internal Room database.
* Survives device reboot when paired with BOOT_COMPLETED handling.
* Integrates cleanly with Hilt via HiltWorker annotation.
* No persistent user-facing notification required for the scheduler itself.

Costs:

* Minimum interval is 15 minutes — sub-minute precision is architecturally impossible with WorkManager.
* WorkManager may defer execution under low battery or heavy system load conditions.
* Exact execution timing is never guaranteed.

Alternatives Rejected:

AlarmManager (exact alarms):
Provides higher precision but requires SCHEDULE_EXACT_ALARM permission on Android 12+, which triggers an additional user-facing permission dialog. Exact timing is not justified by the use case and adds unnecessary permission friction.

Foreground Service with internal timer:
Provides near-real-time precision but requires a persistent notification visible to the user at all times. The continuous battery and UX cost are not justified. Circadian transitions unfold over hours. A foreground service was considered excessive for a task that needs to run a few dozen times per evening.

Rationale:

Circadian transitions occur over hours, not seconds. A 15-minute update granularity yields approximately 24–32 evaluation points during a typical 6–8 hour evening transition. The perceptible difference between a 1-minute and a 15-minute adjustment interval is negligible for gradual warmth and dimming shifts. WorkManager's battery efficiency, process-death resilience, and Hilt integration make it the correct tool. Precision is not the priority — reliability and efficiency are.

---

# Decision 004

Date:
2026-06-04

Status:
Accepted

Context:

The app needs durable storage for three distinct data shapes:
1. CurveProfiles containing ordered collections of CurvePoints (structured, relational, one-to-many).
2. App settings: mode, enabled state, active profile ID (flat key-value pairs).
3. Excluded app package names (simple list of strings).

A unified persistence strategy or a split strategy was needed.

Decision:

Room for CurveProfile, CurvePoint, and ExcludedApp entities. Jetpack DataStore (Preferences) for flat app settings.

Consequences:

Benefits:

* Room handles the one-to-many CurveProfile → CurvePoint relationship naturally with type-safe DAOs and foreign key enforcement.
* Room provides a structured migration path as the schema evolves over versions.
* DataStore is coroutine and Flow-native, lightweight, and exactly appropriate for key-value settings with no relational structure.
* Each tool is used strictly within its intended scope.

Costs:

* Two persistence libraries to understand and maintain.
* Room requires explicit migration scripts for any schema change — developer discipline required.
* DataStore Preferences is not type-safe at the key level (requires careful key management).

Alternatives Rejected:

DataStore for everything:
Would require manual serialization of CurvePoint collections (e.g., JSON strings). Loses relational integrity, type safety, and queryability. Maintenance burden grows proportionally with data complexity.

Room for everything:
Using Room entities and DAOs for a handful of boolean and enum settings is over-engineered. Adds schema complexity and migration risk for data that has no relational structure.

SharedPreferences:
Deprecated. Not coroutine-friendly. Not type-safe. Not an option for new development.

Rationale:

The curve data is inherently relational. The settings data is inherently flat. Using the correct tool for each shape avoids unnecessary complexity on both sides.

---

# Decision 005

Date:
2026-06-04

Status:
Accepted

Context:

The project spans multiple Gradle modules with many injectable components: ViewModels, WorkManager Workers, Room DAOs, Repositories, and DisplayController implementations. A dependency injection strategy must be established before implementation begins, as retrofitting DI into a multi-module project is expensive.

Three options were evaluated: Hilt, Koin, and manual dependency injection.

Decision:

Hilt for all dependency injection across all modules.

Consequences:

Benefits:

* Compile-time dependency graph validation — misconfigured bindings are caught at build time, not at runtime on a user's device.
* Native Android lifecycle awareness — ViewModel injection, HiltWorker for WorkManager, and Service injection work without boilerplate.
* First-class multi-module support via @InstallIn and component hierarchies.
* Strongly documented and supported by Google — low risk of abandonment.
* Familiar to most Android contributors, reducing onboarding friction.

Costs:

* Annotation-heavy code — more visible boilerplate than Koin.
* Code generation increases incremental build times.
* Initial setup (Application class annotation, module annotations) adds upfront work.

Alternatives Rejected:

Koin:
Simpler to set up and less annotation-heavy. However, dependency resolution is runtime-evaluated. A missing or misconfigured binding fails silently until the relevant code path is executed. For a multi-module project where bindings are spread across modules, this is an unacceptable risk. Build-time safety is not optional.

Manual DI:
Full control with zero overhead. However, a manually maintained dependency wiring class does not scale across module boundaries. Every new injectable component requires manual updates to the wiring layer. This becomes a maintenance burden and a source of contributor confusion as the project grows.

Rationale:

Compile-time safety is non-negotiable for a multi-module Android project. Discovering a missing binding at build time is always preferable to discovering it at runtime. Hilt's native Android component integration and strong documentation make it the most sustainable choice for open-source contribution.

---

# Decision 006

Date:
2026-06-04

Status:
Accepted

Context:

The UI architecture pattern for feature modules must be defined before any screen is built. With Jetpack Compose as the UI toolkit, two well-established patterns are viable: MVVM with StateFlow-backed UiState, and MVI (Model-View-Intent) with sealed Intent/Action/Effect classes per screen.

Decision:

MVVM with a single structured UiState data class per screen, emitted via StateFlow from each ViewModel. User interactions are expressed as direct ViewModel function calls. One-time UI events (navigation, snackbars) are handled via a separate SharedFlow-backed UiEvent stream.

Consequences:

Benefits:

* Lower learning curve — MVVM with StateFlow is the dominant Android pattern. Most contributors will already be familiar with it.
* Less boilerplate — no sealed Intent, Action, or Effect class required per screen.
* StateFlow + Compose collectAsStateWithLifecycle() provides practical unidirectional data flow without strict MVI ceremony.
* Android developer documentation defaults to this pattern.

Costs:

* Without sealed Intent classes, ViewModel public APIs can accumulate many individual event functions (onToggleEnabled(), onProfileSelected(), etc.).
* Requires intentional discipline to keep state mutations centralized inside the ViewModel.
* One-time events (navigation, error toasts) require extra care to avoid double-delivery on recomposition — a Channel or SharedFlow must be used instead of regular StateFlow.

Alternatives Rejected:

Full MVI:
Stronger unidirectional guarantees via sealed Intent types. More naturally self-documenting — every possible user action is enumerated as a sealed class. An excellent fit with Compose's reactive model. Rejected because the application's screens are relatively straightforward, and the overhead of defining separate Intent, State, and Effect sealed hierarchies per screen is not justified by the state complexity present. The Curve Editor — the most stateful screen — can naturally adopt MVI-inspired sealed action patterns within the MVVM ViewModel without requiring a dedicated MVI framework.

Rationale:

Simplicity and open-source contributor accessibility take priority at this stage. MVVM with disciplined UiState modeling achieves the same practical result as MVI for this application's screen complexity. If a future screen introduces substantially more complex state interactions, MVI patterns can be adopted locally on that screen without changing the project-wide convention.

---

# Decision 007

Date:
2026-06-04

Status:
Accepted

Context:

A circadian display app's primary use case involves nighttime transitions that cross midnight (e.g., a curve point at 23:00 and another at 06:00). The question is whether `CurveEngine` should interpolate across the midnight boundary (treating time as circular/modular) or clamp to the nearest endpoint (treating time as linear within 0–1439).

Circular interpolation would produce smoother overnight transitions but introduces significant complexity:
- The "before" and "after" points swap roles depending on which side of midnight the query time falls.
- Interpolation across midnight requires modular arithmetic, which is harder to reason about and test.
- The user's mental model may not match — a point at 23:00 and 06:00 might not mean "transition overnight" but rather "these are two unrelated evening and morning settings."

Decision:

Linear (non-circular) interpolation. Time is treated as a linear scale from 0 to 1439. If the query time falls before the first point or after the last point, the engine clamps to the nearest endpoint. Midnight wrap-around is **not** automatically interpolated.

Consequences:

Benefits:

* Simpler, more predictable interpolation logic — no modular arithmetic.
* Easier to test exhaustively — the edge case matrix is linear, not circular.
* The user can explicitly model overnight transitions by placing points at 0 (midnight) if they want a value to apply in the early morning hours.

Costs:

* A curve with points only at 22:00 and 02:00 will clamp at 22:00's values from 22:00 until 02:00, then jump to 02:00's values. There is no smooth transition between the two.
* Users who want smooth overnight transitions must add explicit points at midnight (0) or along the overnight period.

Rationale:

Predictability and testability take priority over automatic overnight smoothness. The current `CurveEngine` implementation already handles this correctly via clamping. If user feedback indicates overnight transitions are a critical need, a future `InterpolationMode.CIRCULAR` option can be added to `CurveEngine` without breaking the existing API.

---

# Decision 008

Date:
2026-06-04

Status:
Accepted

Context:

The `NativeDisplayController` is documented as using Android's `ColorDisplayManager` (API 29+) for hardware-level color temperature shifting. However, `ColorDisplayManager` has significant API restrictions that affect the implementation strategy:

1. `ColorDisplayManager.setNightDisplayMode()` and `setNightDisplayCustomEndTime()` are **hidden APIs** — they are not accessible via the public SDK. They require reflection, `WRITE_SECURE_SETTINGS` permission (granted via ADB), or a system-signed app.
2. The public `ColorDisplayManager` API only exposes getters (e.g., `isNightDisplayActivated()`) and `setSaturationInt()` — it does not allow arbitrary color temperature setting.
3. This means "Native Mode" as described in the architecture cannot simply call a public Android API to set warmth.

Decision:

Native Mode will be implemented as a **best-effort** feature using the following layered strategy:

1. **Primary: `ColorDisplayManager` via `WRITE_SECURE_SETTINGS`** — If the user grants `WRITE_SECURE_SETTINGS` via ADB (`adb shell pm grant`), the controller will use reflection to access the hidden `setNightDisplayMode` and color temperature APIs. This provides true hardware-level shifting.
2. **Fallback: `Settings.Secure` via `WRITE_SECURE_SETTINGS`** — If the above APIs are unavailable on the device, write directly to `Settings.Secure` night display keys (`night_display_activated`, `night_display_color_temperature`, `night_display_auto_mode`).
3. **Graceful degradation** — If `WRITE_SECURE_SETTINGS` is not granted, `isSupported()` returns `false` and the user is directed to use Overlay Mode. A settings screen explains how to grant the permission via ADB with copy-pasteable instructions.

Consequences:

Benefits:

* Provides true hardware-level color shifting on devices that support it, without requiring root.
* Does not silently fail — the user is informed when Native Mode is unavailable.
* Does not require the app to be a system app.

Costs:

* `WRITE_SECURE_SETTINGS` cannot be granted via a standard runtime permission dialog — it requires ADB or a device owner/profile owner. This is a significant friction point for non-technical users.
* Reflection-based access to hidden APIs may break on future Android versions if Google further restricts hidden API access.
* The "Native Mode" experience is effectively a power-user feature, not a one-tap setup.

Alternatives Rejected:

Root access:
Could directly write to `Settings.Secure` or use `su` commands. Rejected because the app is privacy-first and should not require root.

Accessibility Service for color tinting:
An Accessibility Service can apply a system-wide color filter via `AccessibilityService` capabilities. However, this requires the user to enable an Accessibility Service (a sensitive permission often abused by malware), and the color control is limited to display inversion and color correction, not arbitrary warmth. Rejected as overly complex and permission-heavy for the result it provides.

Rationale:

Honesty with the user is more important than pretending Native Mode works with a single tap. By making `WRITE_SECURE_SETTINGS` an explicit, documented requirement, we set correct expectations. Overlay Mode remains the primary, zero-setup experience for all users. Native Mode is an opt-in enhancement for users willing to grant the permission via ADB.

---

# Decision 009

Date:
2026-10-07

Status:
Accepted

Context:

While scoping an in-app appearance option (GitHub issue #5, see Decision 010), an existing defect was found in how the app's *window chrome* is themed.

The Compose UI is pinned to the light colour scheme: `MainActivity` wraps its content in a bare
`MaterialTheme { }`, whose default is `lightColorScheme()`. Compose never reads the XML theme for
colours. The window chrome, however, follows the **system** night mode:

* `enableEdgeToEdge()`'s default `SystemBarStyle.auto` derives the glyph colour from
  `Configuration.uiMode` — the system setting — not from the app's actual theme.
* The XML theme parent was `Theme.Material3.DayNight.NoActionBar`, which also follows the system.

On a device with the system in dark mode this produces chrome drawn for a dark app on top of a light
one. Measured on a Realme RMX3853 running Android 16 (API 36), system dark mode enabled:

* The whole status bar strip is painted with the app's own `surface` colour (`#FFFBFE`).
* The status bar glyphs are drawn pure white: 2,999 pixels brighter than that background across the
  strip, and no dark glyph pixels at all.
* Contrast between glyphs and their background: **~1.03 : 1**. The accessibility floor is 3:1 for UI
  components and 4.5:1 for text. (For comparison, the app's own body text reaches ~16.7:1.)

The icons were therefore effectively invisible for every user with the system in dark mode, on every
build since 1.0.0. It went unnoticed because it is invisible on a light-mode device and produces no
crash or log. A secondary symptom of the same cause: a dark splash/window background that snaps to a
light app on launch.

Decision:

The window chrome must match the app's Compose content, never the system night mode.

1. `MainActivity` calls `enableEdgeToEdge()` with explicit `SystemBarStyle`s (light appearance /
   dark glyphs) instead of relying on the `auto` default. On API 26, which cannot render dark
   navigation-bar glyphs at all, a dark scrim is kept so those glyphs stay legible.
2. The XML theme parent is `Theme.Material3.Light.NoActionBar` while the UI is light-only, so the
   pre-Compose window frame matches the content.

Both of these encode the "app is light" assumption and are replaced by theme-derived values when the
in-app theme override lands (Decision 010).

Consequences:

Benefits:

* Status bar glyphs are legible on every API level and every system night mode.
* The launch flash is gone: the window background matches the app.
* Chrome and content can no longer disagree silently — there is a single source of truth.

Costs:

* The light-only assumption is now expressed in two places (`MainActivity`, `themes.xml`); both must
  change together in Decision 010's work.
* The API 26 navigation bar path is a platform limitation, not a preference, and could not be tested
  on API 26 hardware — no such device or emulator image is available in this environment.

Alternatives Rejected:

Leaving `SystemBarStyle.auto` in place: it is only correct while the app's theme matches the system,
which is exactly the assumption that will break again in Decision 010.

Making the app dark to match the system: not requested, and no dark palette exists yet (Decision 010).

`AppCompatDelegate.setDefaultNightMode()`: would add an AppCompat dependency (the app is a
`ComponentActivity` with a Compose-only stack), and it recreates the activity rather than resolving
the theme during composition.

Setting `android:windowLightStatusBar` in the XML theme: ignored where the runtime appearance hint is
applied, and it would split the source of truth across two mechanisms.

Rationale:

When the chrome and the content disagree, the platform draws glyphs for the wrong background — a
silent, measurable accessibility failure. Deriving the chrome from the app's real theme, even when the
theme is "always light", is the only invariant that stays correct.

---

# Decision 010

Date:
2026-10-07

Status:
Proposed

Context:

GitHub issue #5 requests an in-app appearance option so users can choose a dark UI rather than only
following the system setting. Today the app renders the Material 3 baseline light scheme on every
device (`MaterialTheme { }` with no colour scheme), and no brand palette exists — light mode is
Material's default purple.

The implementation itself is small, but the work also requires choosing a palette for both schemes
and re-shooting the F-Droid phone screenshots. Those are design and release costs, not engineering
risk, so the feature is scheduled for the next major release (2.0.0) instead of a patch release.

Decision:

Implement in-app theme selection in 2.0.0 — System / Light / Dark, defaulting to System:

* `ThemeMode` enum (`SYSTEM`, `LIGHT`, `DARK`) with `isDark(systemInDarkTheme: Boolean)`, living in
  `data/settings/`. Stored in `AppSettings` under a `theme_mode` DataStore key; a missing key must
  resolve to `SYSTEM` so no migration or seeding is needed for existing installs.
* `CircadianDisplayTheme(themeMode)` in `ui/theme/` resolves `SYSTEM` through
  `isSystemInDarkTheme()`, so it continues to follow the system live, and selects the light or dark
  `ColorScheme`. Light stays on the current baseline scheme (no visual change for existing users);
  dark is a new warm-toned scheme. Dynamic colour (API 31+) is evaluated separately.
* `MainActivity` resolves the mode and drives `applySystemBars()` from it, replacing the constant
  light styles from Decision 009, and re-applies `enableEdgeToEdge()` when it changes.
  `themes.xml` returns to `Theme.Material3.DayNight.NoActionBar`.
* Settings gains an "Appearance" section, reusing the existing choice-row pattern.

Traps (recorded so they are not rediscovered):

1. Changing the theme must **not** trigger `SchedulerWorker.triggerNow()`. It is presentation-only;
   unlike `setDisplayMode()`, no display state changes.
2. `CurveEditorScreen` holds dark-blind drawing constants at file level: `GridColor =
   Color(0x22000000)` is invisible on a dark surface, and `DimLineColor = Color(0xFF757575)` is too
   dim. `DimLineColor` is used by both the graph `Canvas` and the legend row, so the colours must be
   computed once in the screen composable and threaded through, not read in situ.
3. `PreviewContent`'s hardcoded white/amber values must stay hardcoded — they simulate a physical
   screen panel, not app chrome. A comment should say so, or a future contributor will "fix" them.
4. DataStore reads are asynchronous, so the first frame cannot know a saved override. Defaulting to
   `SYSTEM` keeps the window background correct for most users; a user who overrides against the
   system sees one frame of the window background on cold start. Accepted — a blocking read on the
   main thread is not worth one frame.
5. Unit tests cannot catch scheme-dependent contrast defects. Verify with screenshots/contrast
   measurement (see `TESTING.md` § Screenshot Testing). Decision 009 is the worked example.

Consequences:

Benefits:

* Users control the app's appearance instead of being tied to the system setting.
* The two "the app is light" assumptions collapse into one resolved value.
* The palette is chosen once, deliberately, instead of accumulating ad-hoc colours.

Costs:

* A new DataStore key and a new `ui/theme/` package.
* Every screen needs a dark-mode pass and visual QA; the app uses many hardcoded drawing colours.
* The F-Droid phone screenshots must be retaken.
* `minSdk` is 26, so dynamic colour is unavailable below API 31.

Alternatives Rejected:

`AppCompatDelegate.setDefaultNightMode()`: see Decision 009.

Per-profile theme: the theme is a device/UI preference, not curve data. No evidence users want it.

Synchronous first read of the setting: adds a main-thread disk read to avoid one frame.

Shipping the override in a patch release: the palette decision and screenshot work would be rushed or
dropped, which is how visually inconsistent releases happen.

Out of scope: anything affecting the overlay/native display controllers or the scheduler (the theme is
chrome only), and time-based "auto at sunset" behaviour (the issue asks for three fixed options).

Rationale:

The engineering is cheap; the design and release work is not. Deferring to a major release lets the
palette be decided deliberately and keeps patch releases free of visual churn, while Decision 009
removes the defect that users are actually feeling today.

This is versioned 2.0.0 rather than 1.1.0 because `RELEASE_PROCESS.md` counts a presentation overhaul
as a major change, even though no stored data format changes.
