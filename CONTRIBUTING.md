# Contributing

Thank you for your interest in contributing to Circadian Display.

The goal of this project is to create an open-source Android application that improves evening screen comfort through adaptive warmth and dimming curves.

We welcome:

* Bug reports
* Feature suggestions
* Documentation improvements
* Testing feedback
* Code contributions

---

# Development Setup

Requirements:

* Android Studio (latest stable)
* JDK 17+
* Android SDK
* Git

Clone repository:

git clone <repository-url>

Open project in Android Studio.

Build:

./gradlew build

Run tests:

./gradlew test

---

# Architecture

Before making significant changes, please review:

* PROJECT_VISION.md
* PRODUCT_REQUIREMENTS.md
* ARCHITECTURE.md
* DECISIONS.md

These documents explain the reasoning behind architectural choices.

---

# Code Style

General principles:

* Prefer readability over cleverness.
* Keep business logic framework-independent whenever possible.
* Follow Kotlin conventions.
* Keep functions focused and small.
* Write comments explaining why, not what.

Avoid:

* Premature optimization
* Unnecessary abstractions
* Over-engineering

---

# Testing

All new business logic should include tests.

Expected areas:

* Unit tests
* Integration tests where applicable

A feature is not considered complete until tests pass.

Run:

./gradlew test

before opening a pull request.

---

# Pull Request Process

1. Create a feature branch.

feature/<short-description>

Examples:

feature/curve-engine

feature/overlay-controller

2. Make changes.

3. Add or update tests.

4. Ensure project builds successfully.

5. Open pull request.

Please include:

* Problem being solved
* Summary of changes
* Screenshots (if UI changes)
* Testing performed

---

# Feature Requests

Before implementing major features:

* Open an issue
* Discuss design
* Reach agreement on direction

This helps avoid duplicated work.

---

# Philosophy

Circadian Display values:

* Privacy
* Simplicity
* User control
* Open source collaboration

When in doubt, choose the simpler solution.
