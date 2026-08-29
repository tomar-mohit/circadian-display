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
