---
name: Android Emulator Warm Boot Workflow
alwaysApply: true
---

# Android Emulator Lifecycle Rules

## Environment

This machine uses:

- Windows
- PowerShell 7 (`pwsh`)
- Android SDK location:

D:\Android\SDK

The emulator executable is:

D:\Android\SDK\emulator\emulator.exe


# Default Emulator

Primary development emulator:

Pixel_7


# Prefer warm boot over cold boot

For normal development:

- Always prefer Quick Boot / snapshot restore.
- Avoid cold boot unless troubleshooting.
- Do not wipe emulator data unless explicitly requested.

Warm boot preserves:

- installed apps
- login state
- emulator settings
- test data
- app state


# Starting the emulator

When starting the development emulator, prefer snapshot restore.

Use:

```powershell
D:\Android\SDK\emulator\emulator.exe -avd Pixel_7