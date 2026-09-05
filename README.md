# Circadian Display 🌙📱

An open-source Android application designed to improve evening screen comfort and sleep hygiene through highly customizable, adaptive screen warmth and dimming curves.

Unlike standard "night light" solutions that only offer static on/off schedules or fixed temperatures, **Circadian Display** continuously adjusts screen characteristics throughout the evening based on user-defined dynamic curves.

---

## 🎯 Key Features

- **Dynamic Comfort Curves:** Plot your custom evening transition with precise warmth and dimming levels over time.
- **Dual-Engine Modes:**
  - **Native Mode:** Leverages system-level display adjustment APIs where supported for premium hardware-level shifting.
  - **Overlay Mode:** Highly customizable, high-compatibility software overlay filtering warmth and brightness.
- **Background Scheduler:** Lightweight, efficient execution that automatically adjusts the display state in the background.
- **App Exclusions:** Define lists of applications (e.g., cameras, photo editors, games) where screen tinting and dimming should temporarily deactivate.
- **Simulation/Preview Mode:** Quickly scrub through a 24-hour timeline to see and feel how your display transitions throughout the night.
- **Privacy & Performance First:** Completely open source, offline-first, tracker-free, with zero background battery drain.

---

## 📂 Project Architecture & Documentation

This project is meticulously designed and planned around a modern, modular Android architecture. Below is a guide to the project's documentation:

*   **[ARCHITECTURE.md](ARCHITECTURE.md):** Multi-module layout, tech stack, domain models, module dependency graph, data flow diagrams, and service contracts.
*   **[TESTING.md](TESTING.md):** Coverage targets, fake/mock strategy, CurveEngine edge case test matrix, Compose UI testing, device matrix, and full manual QA checklist.
*   **[RELEASE_PROCESS.md](RELEASE_PROCESS.md):** Branching strategy, semantic versioning guide, changelog management, release pipeline, and F-Droid / Google Play distribution requirements.
*   **[CONTRIBUTING.md](CONTRIBUTING.md):** Development setup, code style, and pull request process.

---

## 🗺️ Project Status

Core engine, scheduler, and both display controllers are implemented end-to-end.
The Curve Editor's graph editing and App Exclusions are still in progress.

| Phase | Status |
|---|---|
| Phase 0 — Foundation (Gradle, Hilt, Room, Compose scaffold) | ✅ Done |
| Phase 1 — Core Curve Engine (`CurveEngine` + domain models) | ✅ Done |
| Phase 2 — Scheduler (WorkManager background evaluation) | ✅ Done |
| Phase 3 — Overlay Mode (`OverlayDisplayController`) | ✅ Done |
| Phase 4 — Native Mode (`NativeDisplayController`) | ✅ Done |
| Phase 5 — Curve Editor UI | 🟡 Partial |
| Phase 6 — App Exclusions | 🟡 Partial |

Shipped so far:
- [x] Core curve interpolation engine with 100% unit-test coverage.
- [x] Room + DataStore persistence (profiles, points, settings).
- [x] WorkManager scheduler (~15 min) with boot re-enqueue.
- [x] Overlay display controller (amber warmth + black dimming layers).
- [x] Native display controller (system night light via `WRITE_SECURE_SETTINGS`).
- [x] Dashboard and Settings screens.
- [x] Profiles list (create, activate, delete).
- [ ] Curve Editor graph editing (add/move points, save) and Preview.
- [ ] App Exclusions (foreground-app detection via Usage Access).
- [x] CI (build, unit tests, ktlint, detekt, lint).

---

## 🛠️ Getting Started (For Contributors)

To set up the development environment, please see **[CONTRIBUTING.md](CONTRIBUTING.md)**. 

### Quick Build & Test

To run the project checks and tests local command line:

```bash
# Build the project
./gradlew build

# Run unit tests
./gradlew test
```

---

## 📄 License

This project is licensed under the **Apache License 2.0**. See [LICENSE](LICENSE) for the full text.
