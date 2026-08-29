# Testing Strategy

---

## Philosophy

* Tests are not optional. A feature is not complete until its tests pass.
* Business logic must be testable without an Android device or emulator wherever possible.
* Prefer fakes over mocks. Fakes are more maintainable and produce clearer failure messages.
* `CurveEngine` is pure Kotlin. It has no Android dependencies and must maintain 100% unit test coverage.
* Tests should document behaviour — a failing test should tell you exactly what broke and why.

---

## Coverage Targets

| Layer | Target | Rationale |
|---|---|---|
| `core/curve` (CurveEngine) | 100% | Pure logic. The heart of the app. No excuses. |
| `core/scheduler` | 80%+ | Orchestration logic — testable with fakes for dependencies. |
| `core/settings` | 80%+ | Repository logic and DataStore interactions. |
| `features/*` ViewModels | 80%+ | Business logic in ViewModels. UI state transitions. |
| `system/*` controllers | 60%+ | Android-dependent. Integration tests preferred over unit tests here. |
| UI (Compose) | Key flows | Not line-coverage focused. Cover critical user journeys. |

---

## Fake and Mock Strategy

This project follows a **fakes-first** approach.

### Fakes (preferred)

For each core repository and controller interface, a corresponding in-memory fake implementation must be created and kept in the `test` source set.

| Fake | Replaces | Behaviour |
|---|---|---|
| `FakeCurveRepository` | `CurveRepository` (Room) | In-memory list of profiles and points. Full CRUD support. |
| `FakeSettingsRepository` | `SettingsRepository` (DataStore) | In-memory map of settings keys. |
| `FakeDisplayController` | `DisplayController` | Records the last `DisplayState` passed to `apply()`. Tracks `clear()` call count. Exposes `lastAppliedState` for assertions. |
| `FakeExclusionRepository` | `ExclusionRepository` | In-memory set of excluded package names. |

### Mocks (restricted use)

Mockito or MockK may be used **only** for Android platform classes that cannot be faked (e.g., `Context`, `WindowManager`, `ColorDisplayManager`). Do not mock interfaces or classes that can be replaced with a fake.

### Rationale

Fakes are real implementations of the interface contract. They catch integration bugs that mocks miss. They are also far easier to maintain across refactors — a renamed method breaks a fake at compile time, not silently at runtime.

---

## Unit Tests

### CurveEngine — Required Test Cases

These cases are non-negotiable. Every edge case in the interpolation logic must have a dedicated test.

| Test Case | Input | Expected Output |
|---|---|---|
| Empty profile | Profile with no points, any time | `DisplayState(warmth=0f, dimming=0f)` |
| Single point, any time | One point at 20:00 (1200 min), queried at 21:00 | That point's warmth and dimming values |
| Time exactly on a point | Points at 20:00 and 22:00, queried at 20:00 | Exact values of the 20:00 point |
| Time between two points (midpoint) | Point A at 20:00 (warmth=0.2), Point B at 22:00 (warmth=0.8), queried at 21:00 | `warmth=0.5` (linear midpoint) |
| Time between two points (off-center) | Same points, queried at 20:30 | `warmth=0.35` (25% of the way from A to B) |
| Time before first point | First point at 20:00, queried at 18:00 | First point's values (clamped) |
| Time after last point | Last point at 23:00, queried at 23:30 | Last point's values (clamped) |
| Points given out of order | Points inserted in reverse time order | Same result as if ordered — engine sorts before processing |
| Duplicate timeMinutes | Two points at the same time with different warmth values | First occurrence used, no crash |
| Warmth and dimming interpolated independently | Points with asymmetric warmth/dimming curves | Both values correctly interpolated independently |
| Boundary values | `warmth=0.0` and `warmth=1.0` at boundaries | No clamping artefacts, exact values returned |

### SchedulerService — Required Test Cases

| Test Case | Expected Behaviour |
|---|---|
| isEnabled = false | `DisplayController.clear()` called. `apply()` never called. |
| isEnabled = true, valid profile | `CurveEngine` called with correct time. `DisplayController.apply()` called with result. |
| isEnabled = true, no active profile (id = -1) | `DisplayController.clear()` called. No crash. |
| isEnabled = true, profile exists but has no points | `DisplayController.apply()` called with safe default state. |
| App in exclusion list | `DisplayController.clear()` called for the duration of foreground app match. |
| triggerNow() called | Same evaluation logic runs immediately, same assertions apply. |

### ViewModel Unit Tests

All ViewModels must be tested against their `UiState` contract:
- Initial state is correct.
- Each user action produces the correct state transition.
- Loading states are emitted before async operations complete.
- Error states are emitted on repository failures.
- One-time UiEvents (navigation, snackbars) are emitted exactly once.

---

## Integration Tests

### Room Database

* Run on device/emulator using `@RunWith(AndroidJUnit4)` and an in-memory Room database.
* Required coverage:
  * Insert a `CurveProfile` with multiple `CurvePoints` and read it back — verify ordering.
  * Delete a `CurveProfile` — verify cascading deletion of its `CurvePoints`.
  * Only one `CurveProfile` has `isActive = true` at any time — verify repository enforces this.
  * Schema migration tests for every version increment (once schema is established).

### WorkManager

* Use `WorkManagerTestInitHelper` to test worker execution in isolation.
* Verify the worker correctly invokes the scheduler evaluation path.
* Verify the worker re-enqueues itself after BOOT_COMPLETED.

### DisplayController

* `OverlayDisplayController`: Integration test on a real device to verify overlay renders, updates on state change, and is fully removed on `clear()`.
* `NativeDisplayController`: Verify `isSupported()` returns correct value per API level. Verify graceful fallback when unsupported.

---

## Compose UI Tests

Use `ComposeTestRule` for all UI tests. Do not test layout pixel positions — test semantic behaviour.

### Required UI Test Coverage

| Screen | Test Cases |
|---|---|
| Dashboard | Toggle renders enabled/disabled state. Active profile name displayed. Current warmth/dimming values shown. |
| Curve Editor | Empty state shows prompt to add a point. Adding a point updates the graph. Deleting a point removes it. Save button disabled if no changes. |
| App Exclusions | List populates from repository. Toggling an app updates its exclusion state. Search filters list correctly. |
| Settings | Mode switch triggers correct state change. Permission rationale shown when permission missing. |

### Screenshot Testing

Not required for MVP. Flagged for Phase 5 (User Experience) once UI is stable. Consider Paparazzi or Roborazzi when introduced.

---

## Device Testing Matrix

| Android Version | API Level | Priority | Reason |
|---|---|---|---|
| Android 8.0 | 26 | Required | Minimum SDK. Must not crash. |
| Android 10 | 29 | Required | `ColorDisplayManager` available from here (Native Mode baseline). |
| Android 12 | 31 | Required | Permission model changes, exact alarm restrictions introduced. |
| Android 14 | 34 | Required | Latest permission changes, WorkManager behaviour updated. |
| Latest Android | Current | Required | Always verify on current release. |

Minimum physical device coverage per release:
- One Pixel device (clean Android, no OEM skin)
- One Samsung device (One UI — highest market share, most aggressive battery management)
- One Xiaomi device (MIUI — known autostart and background restrictions)

Emulators are acceptable for unit and integration tests. Physical devices are required for overlay, permission, and scheduler reliability testing.

---

## Manual Testing Checklist

Run before every beta or stable release. Record the Android version and device model for each test session.

### Overlay Mode

| Step | Expected Result |
|---|---|
| Grant SYSTEM_ALERT_WINDOW permission and enable Overlay mode | Overlay renders on screen, warmth and dimming applied |
| Deny SYSTEM_ALERT_WINDOW permission | App shows rationale. No crash. No overlay drawn. |
| Disable Overlay mode | Overlay is fully removed from screen |
| Open an excluded app while overlay is active | Overlay disappears for that app |
| Return from excluded app | Overlay re-appears |
| Revoke permission while overlay is active | Overlay disappears gracefully. No crash. |

### Scheduler

| Step | Expected Result |
|---|---|
| Set a curve, enable the app, wait ~15 minutes | Display state updates to match the current curve position |
| Reboot device with app enabled | Scheduler restarts. Display state is applied within ~15 minutes of boot |
| Disable battery optimization for the app on Samsung/Xiaomi | Scheduler fires reliably. Compare with optimization enabled. |
| Force-stop app and wait ~15 minutes | WorkManager re-triggers the worker on next system window |

### Curve Management

| Step | Expected Result |
|---|---|
| Create a new curve with 3+ points | Profile saved and listed |
| Edit an existing curve point | Change persisted on save |
| Delete a curve point | Point removed from profile |
| Delete a profile | Profile and all its points removed |
| Set a profile as active | Scheduler uses this profile on next evaluation |

### Preview Mode

| Step | Expected Result |
|---|---|
| Scrub timeline to a time before any curve points | Display preview shows default (no adjustment) state |
| Scrub timeline to a time between two points | Warmth and dimming values interpolate correctly |
| Scrub timeline to a time after the last point | Display preview shows last point's values |
| Close preview | Live screen is unchanged |

### Edge Cases

| Step | Expected Result |
|---|---|
| Enable app with no active profile set | App handles gracefully. No crash. No overlay. |
| Delete the active profile | App falls back to no-profile state gracefully |
| Create a curve with only one point | Single point applied at all times |

---

## Beta Testing Channels

| Channel | Purpose |
|---|---|
| GitHub Releases (pre-release tag) | Early access for technical users. Primary bug report channel. |
| F-Droid (testing track) | Privacy-focused user community. Excellent feedback on permissions and battery behaviour. |
| Direct invites | Known Android developers and testers for structured feedback sessions. |
| Reddit (r/androidapps, r/android) | Broader community feedback on UX and feature requests. |
