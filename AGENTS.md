# AGENTS.md — Circadian Display Project Context

> Read this first when starting a new chat. It contains everything you need to
> continue development without re-reading the full codebase.

---

## Project Overview

A circadian screen-dimming Android app. Users define comfort curves (warmth +
dimming vs time of day). A background scheduler evaluates the active curve and
applies adjustments via either a full-screen software overlay or Android's
native color temperature APIs.

**Tech stack:** Kotlin, Jetpack Compose, Hilt, Room, DataStore, WorkManager

---

## Current Status — July 2026

| Phase | Status |
|---|---|
| Phase 0 — Foundation (Gradle, Hilt, Room, Compose scaffold) | ✅ Done |
| Phase 1 — Core Curve Engine (`CurveEngine` + domain models) | ✅ Done |
| Phase 2 — Scheduler (WorkManager background evaluation) | ✅ Done |
| Phase 3 — Overlay Mode (`OverlayDisplayController`) | ✅ Done |
| Phase 4 — Native Mode (`NativeDisplayController`) | 🔜 Next |
| Phase 5 — Curve Editor UI | 🔜 Future |
| Phase 6 — App Exclusions | 🔜 Future |

---

## Module Structure

```
circadian-display/
├── app/                              # Main Android app (wiring + UI + scheduler)
│   └── src/main/java/com/circadiandisplay/app/
│       ├── data/entity/              # Room entities + toDomain() mappers
│       ├── data/dao/                 # Room DAOs
│       ├── data/repository/          # CurveRepository
│       ├── data/settings/            # AppSettings (DataStore)
│       ├── data/seed/                # SeedData (first-launch defaults)
│       ├── di/                       # Hilt modules (DatabaseModule, CurveModule, DisplayControllerModule)
│       ├── scheduler/                # SchedulerWorker + BootReceiver
│       └── ui/dashboard/             # DashboardScreen + DashboardViewModel + DashboardUiState
├── core/
│   └── curve/                        # Pure Kotlin: CurveEngine, CurvePoint, CurveProfile,
│       │                             #   DisplayState, DisplayMode, DisplayController (interface)
│       └── src/test/                 # CurveEngineTest (100% coverage)
├── system/
│   └── overlay/                      # OverlayDisplayController (WindowManager full-screen overlay)
└── gradle/
    └── libs.versions.toml            # Version catalog
```

---

## Key Files & Their Roles

### Core Domain (`core/curve`)
- **CurvePoint.kt** — Data class: `id`, `profileId`, `timeMinutes` (0–1439), `warmth` (0–1), `dimming` (0–1). Validates ranges in `init`.
- **CurveProfile.kt** — Data class: `id`, `name`, `isActive`, `createdAt`.
- **CurveEngine.kt** — Pure function `calculateDisplayState(profile, points, timeMinutes)`. Linear interpolation with edge clamping. No Android deps. 100% tested.
- **DisplayState.kt** — Data class: `warmth: Float`, `dimming: Float`.
- **DisplayMode.kt** — Enum: `NATIVE`, `OVERLAY`.
- **DisplayController.kt** — Interface: `apply(state)`, `clear()`, `isSupported()`.

### App Layer (`app`)
- **CurveProfileEntity.kt / CurvePointEntity.kt** — Room entities with `toDomain()` / `toEntity()` extension functions. CASCADE delete on profile → points.
- **CurveProfileDao.kt** — `getAll(): Flow`, `getActive()`, `setActive(id)` (transactional: deactivate all → activate one). `observeActive()` for reactive UI.
- **CurvePointDao.kt** — `getByProfileId(): Flow`, `getByProfileIdOnce(): List`, CRUD.
- **AppDatabase.kt** — Room DB, version 1, exports schema to `app/schemas/`.
- **CurveRepository.kt** — Bridges DAOs ↔ domain models. Handles entity mapping.
- **AppSettings.kt** — DataStore (Preferences): `isEnabled`, `displayMode`, `activeProfileId`, `lastEvaluatedAt`. Exposes both `Flow` and `suspend Snapshot()` methods.
- **SeedData.kt** — Inserts "Evening" profile with 4 points (18:00→23:00) on first launch.
- **SchedulerWorker.kt** — `@HiltWorker`, `CoroutineWorker`. Runs every ~15 min. Companion: `enqueuePeriodic()`, `triggerNow()`, `cancelAll()`.
- **BootReceiver.kt** — Re-enqueues periodic work on `BOOT_COMPLETED`.

### DI (`app/di`)
- **DatabaseModule.kt** — Provides `AppDatabase`, `CurveProfileDao`, `CurvePointDao`.
- **CurveModule.kt** — Provides `CurveEngine` as singleton.
- **DisplayControllerModule.kt** — Binds `DisplayController` → `OverlayDisplayController`.

### UI (`app/ui/dashboard`)
- **DashboardUiState.kt** — Immutable state: `isEnabled`, `activeProfileName`, `currentWarmth`, `currentDimming`, `activeMode`, `currentTimeMinutes`, etc.
- **DashboardViewModel.kt** — `@HiltViewModel`. Combines `AppSettings.isEnabled` + `AppSettings.displayMode` + `CurveRepository.observeActiveProfile()` → computes `DashboardUiState`. `toggleEnabled()` triggers `SchedulerWorker.triggerNow()`.
- **DashboardScreen.kt** — Compose UI: master toggle card, warmth/dimming indicators, current time, mode badge.

### Overlay (`system/overlay`)
- **OverlayDisplayController.kt** — `@Singleton`. Creates full-screen `View` via `WindowManager`. Uses `Handler(Looper.getMainLooper())` to dispatch to main thread. Warmth → amber tint (`lerpColor`), dimming → `view.alpha`. Requires `SYSTEM_ALERT_WINDOW`.

---

## Dev Environment

| Variable | Value |
|---|---|
| **Project root** | `D:\VS\circadian-display` |
| **JDK** | `C:\Program Files\Android\Android Studio\jbr` (JetBrains JDK 21) |
| **Android SDK** | `D:\Android\SDK` |
| **ADB** | `D:\Android\SDK\platform-tools\adb.exe` |
| **Emulator** | `Pixel_7` AVD |

### Build command:
```powershell
cd D:\VS\circadian-display
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug
```

### Install + launch:
```powershell
& "D:\Android\SDK\platform-tools\adb.exe" install -r "app\build\outputs\apk\debug\app-debug.apk"
& "D:\Android\SDK\platform-tools\adb.exe" shell am start -n com.circadiandisplay.app/.MainActivity
```

### Logs:
```powershell
& "D:\Android\SDK\platform-tools\adb.exe" logcat -d -s SchedulerWorker:D
```

---

## Important Architecture Decisions

- **minSdk = 26** (Android 8.0) — modern APIs, reduced maintenance.
- **Linear (non-circular) interpolation** — Time before first point → clamp to first point. Time after last → clamp to last. No midnight wrap-around.
- **Room for relational data, DataStore for flat settings.**
- **Hilt for DI** — compile-time safety.
- **MVVM with single UiState per screen.**
- **WorkManager ~15-min periodic** for background evaluation (not AlarmManager, not Foreground Service).
- **Overlay Mode requires `SYSTEM_ALERT_WINDOW`** — checked at runtime, not granted automatically.
- **Native Mode requires `WRITE_SECURE_SETTINGS` via ADB** — power-user opt-in feature (see Decision 008).

---

## Recent Fixes (July 2026)

1. **HiltWorkerFactory**: `CircadianDisplayApp` must implement `Configuration.Provider` and inject `HiltWorkerFactory`, otherwise WorkManager can't instantiate `@HiltWorker` classes. Also requires removing default `WorkManagerInitializer` in manifest.

2. **Main thread dispatch**: `WindowManager.addView()` crashes on background threads. `OverlayDisplayController` uses `Handler(Looper.getMainLooper()).post {}` to safely dispatch.

3. **`xmlns:tools`** must be on `<manifest>` root element, not on child elements.

---

## What's Running Right Now

- App launches → seeds "Evening" profile (18:00–23:00) if empty
- Dashboard shows warmth/dimming values (0% at noon, ramps to 100% warmth / 50% dimming at 23:00)
- Scheduler evaluates every ~15 min + immediately on launch + on toggle
- OverlayDisplayController creates/updates the overlay when evaluated
- BootReceiver re-enqueues after reboot
- Toggle switch triggers immediate re-evaluation

---

## What's Next

1. **Overlay permission UX** — Check `SYSTEM_ALERT_WINDOW` before applying, show rationale dialog.
2. **Battery optimization exemption** — Guide user to disable for reliability.
3. **Curve Editor UI** — Create/edit/delete profiles and points.
4. **Native Mode** — `NativeDisplayController` using `ColorDisplayManager` reflection (see Decision 008).
5. **App Exclusions** — Pause overlay for specific foreground apps.
6. **Extract `core/scheduler` module** — Move SchedulerWorker out of `app`.
7. **Extract `core/settings` module** — Move AppSettings out of `app`.
