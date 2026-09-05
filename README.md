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

We are currently in **Phase 0 (Foundation)** of our [Roadmap](ROADMAP.md). 

At this stage:
- [x] Product Vision and Requirements are defined.
- [x] Architectural Design and ADRs established (SDK target, dual mode, WorkManager, Room + DataStore, Hilt, MVVM, midnight wrap-around, native mode constraints).
- [x] Testing strategy, debugging guide, and release process defined.
- [x] Data model and UI flows documented.
- [x] GitHub issue templates, PR template, and CI workflow configured.
- [x] Apache 2.0 License added.
- [x] Android project skeleton and dependency catalog setup.
- [x] Core Curve Interpolation Engine implementation with 100% test coverage.
- [ ] Room/DataStore persistence layer.
- [ ] Scheduler (WorkManager + BOOT_COMPLETED).
- [ ] Overlay and Native display controllers.
- [ ] Dashboard, Curve Editor, Settings UI.

---

## 🛠️ Getting Started (For Contributors)

To set up the development environment, please see **[CONTRIBUTING.md](CONTRIBUTING.md)**. 

### Quick Build & Test (Once Skeleton is Created)

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
