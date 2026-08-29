# Debugging Guide

This document records common troubleshooting procedures, known issues, debugging workflows, and platform-specific quirks encountered during development.

Purpose:

* Speed up bug investigation
* Preserve troubleshooting knowledge
* Help contributors diagnose issues
* Document Android platform limitations

---

# Logging Standards

Use structured logging.

Format:

[Feature] Description

Examples:

[CurveEngine] Interpolated value calculated

[Scheduler] Update triggered

[Overlay] Permission denied

[DisplayController] Native mode activated

Avoid:

* Generic logs
* Unclear messages
* Excessive spam logging

---

# Log Tags

Recommended tags:

CurveEngine

Scheduler

OverlayController

NativeController

Settings

Database

Worker

UI

Example:

Log.d("CurveEngine", "Calculated warmth=45")

---

# Debug Builds

Debug builds may include:

* Additional logging
* Developer diagnostics screen
* Curve preview tools
* Timing information

Release builds should minimize logging.

---

# Troubleshooting Checklist

When investigating a bug:

1. Reproduce issue
2. Record Android version
3. Record device manufacturer
4. Record device model
5. Check logs
6. Check permissions
7. Check battery optimization settings
8. Attempt reproduction on emulator
9. Document findings

---

# Known Android Challenges

The following areas are known to cause issues across Android devices:

* Overlay permissions
* Accessibility permissions
* Background execution limits
* Battery optimization
* OEM customizations
* Notification restrictions

Always verify behavior on real devices.

---

# OEM Notes

## Google Pixel

Status:

No known issues.

---

## Samsung

Potential concerns:

* Battery optimization
* Background task restrictions

Testing required.

---

## Xiaomi

Potential concerns:

* Aggressive process termination
* Autostart restrictions

Testing required.

---

## OnePlus

Potential concerns:

* Background execution policies

Testing required.

---

# Overlay Mode Troubleshooting

Symptoms:

* Overlay not visible
* Overlay unexpectedly disabled
* Touch interactions blocked

Checks:

* Overlay permission granted
* Android version
* Active system dialogs
* Foreground application

Notes:

Some Android security dialogs intentionally disable overlays.

This is expected behavior.

---

# Scheduler Troubleshooting

Symptoms:

* Curve updates not occurring
* Delayed execution

Checks:

* WorkManager status
* Battery optimization settings
* Device idle state

---

# Database Troubleshooting

Checks:

* Room migrations
* Data integrity
* Profile loading

Verify:

* Curve points saved
* Curve points loaded correctly
* Settings persisted across restarts

---

# Performance Investigation

Monitor:

* CPU usage
* Memory usage
* Wake locks
* Background work frequency

Goals:

* Minimal battery impact
* Minimal CPU usage
* No unnecessary wakeups

---

# Known Issues

This section documents confirmed recurring problems, their root causes, and known workarounds. Update this log as issues are discovered and resolved. Do not delete resolved entries — mark them with their fix version for historical reference.

---

Issue:
WorkManager tasks silently killed on Xiaomi devices running MIUI.

Symptoms:
Scheduler does not fire after the device screen turns off. Display state is not updated until the app is manually opened.

Root Cause:
MIUI's aggressive background process termination kills WorkManager's internal scheduling even when battery optimization is disabled via the standard Android settings path. MIUI maintains its own separate autostart permission that is not exposed through standard Android APIs.

Resolution:
No programmatic resolution exists. The standard Android battery optimization exemption request alone is insufficient on MIUI.

Workaround:
1. Direct the user to MIUI Settings → Apps → Manage Apps → [App Name] → Battery Saver → set to No Restrictions.
2. Direct the user to enable Autostart permission for the app in MIUI Security settings.
3. Consider an in-app permissions screen that deep-links to these OEM-specific settings pages (see [dontkillmyapp.com](https://dontkillmyapp.com) for deep link references).

Affected Versions:
All versions on MIUI 12+.

Status:
Open — OEM limitation. No complete fix possible.

---

Issue:
WorkManager periodic work deferred excessively on Samsung devices with Adaptive Battery enabled.

Symptoms:
Scheduler fires significantly less frequently than the configured 15-minute interval. Observed intervals of 45–90 minutes on Samsung Galaxy devices with Adaptive Battery active.

Root Cause:
Samsung's Adaptive Battery feature classifies apps it deems infrequently used and places their background work into a restricted bucket. WorkManager tasks in restricted buckets are deferred by the system, sometimes to hourly or multi-hour intervals.

Resolution:
Request battery optimization exemption via ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS on first launch. This prompts the user directly.

Workaround:
Instruct users to disable Adaptive Battery for the app in Samsung Settings → Battery → Adaptive Battery, or add the app to the unrestricted list.

Affected Versions:
All versions on Samsung One UI 3+.

Status:
Mitigated — battery optimization exemption request reduces frequency. Not fully solvable without a Foreground Service.

---

Issue:
System security dialogs block overlay rendering on all Android devices.

Symptoms:
Overlay disappears or is temporarily suspended when a system security dialog appears (e.g., permission dialogs, biometric prompts, Google Play Protect alerts).

Root Cause:
This is intentional Android platform behaviour. The system intentionally prevents overlays from rendering over security-sensitive UI surfaces to prevent tapjacking attacks. This is not a bug.

Resolution:
None needed. This is correct and expected behaviour.

Workaround:
Document this behaviour in user-facing help text so users are not alarmed when the overlay briefly disappears during system dialogs.

Affected Versions:
All versions. All Android versions.

Status:
Closed — expected platform behaviour. Not a bug.

---

Issue:
Overlay mode requires re-granting SYSTEM_ALERT_WINDOW permission after certain Android OS updates on some devices.

Symptoms:
Overlay stops rendering after an OS update. The user did not change any settings.

Root Cause:
Some OEM OS updates reset the SYSTEM_ALERT_WINDOW permission grant as part of their update process. Observed on Samsung and OnePlus.

Resolution:
Detect the missing permission on each app foreground and prompt the user to re-grant if it was previously granted but is now revoked.

Workaround:
Add a permission state check in the app's onResume() path. Surface a non-intrusive banner informing the user that the overlay permission was lost and must be re-granted.

Affected Versions:
Unknown. Reported pattern on Samsung One UI and OxygenOS.

Status:
Open — pending implementation of permission state monitoring.
