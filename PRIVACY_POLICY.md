# Privacy Policy

**Last updated:** July 2026

Circadian Display ("the app") is a free, open-source Android application that
adjusts your screen's warmth and dimming throughout the evening. This policy
explains how the app handles your data.

## The short version

The app collects **nothing**. It has no analytics, no ads, no trackers, and no
network code. Everything it does happens locally on your device, and nothing is
ever transmitted anywhere.

## Permissions and why they're used

### Draw over other apps (`SYSTEM_ALERT_WINDOW`)

Used by **Overlay Mode** to draw a tint/dimming layer over the screen. This is
the app's core function. It does not read, collect, or transmit the contents of
your screen.

### Usage Access (`PACKAGE_USAGE_STATS`)

Used **only** for the App Exclusions feature: the app checks which app is
currently in the foreground so it can pause adjustments while an excluded app
(e.g. a camera or photo editor) is open. This check happens locally and
immediately; the foreground package name is **not stored, logged to disk, or
transmitted**.

### Query installed apps (`<queries>`)

Used **only** to populate the App Exclusions picker with the list of launchable
apps on your device. The list is read once to display names and icons, and is
**not stored or transmitted**.

### Battery optimization exemption (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`)

Requested so the background scheduler runs reliably. No data is involved.

### `WRITE_SECURE_SETTINGS` (Native Mode, optional)

An optional, power-user feature granted manually via ADB. When granted, the app
writes to Android's night-light settings to shift color temperature at the
hardware level. This is a device setting only — no personal data.

## Data stored on your device

The app stores only what you configure, in app-private storage:

- Curve profiles and their points (Room database)
- Settings such as display mode and enabled state (DataStore preferences)
- The list of apps you've excluded (Room database)

None of this data leaves your device. It is deleted when you uninstall the app.

## Changes to this policy

If the app's data handling ever changes, this policy will be updated and the
"Last updated" date revised.

## Contact

Because the app is open source, issues and questions are best raised on the
project's issue tracker (see the repository README).
