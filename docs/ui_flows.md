# UI Flows

This document describes the screen map, navigation structure, and per-screen content for Circadian Display.

---

## Screen Map

```
App Launch
    │
    └──► Dashboard  (home)
              │
              ├──► Curve Editor  ◄──► (add / edit points)
              │         │
              │         └──► Preview Mode  (time scrubber overlay)
              │
              ├──► Profiles List
              │         │
              │         └──► Curve Editor  (for a selected profile)
              │
              └──► Settings
                        │
                        ├──► App Exclusions
                        │
                        └──► Permission Rationale screens (overlay, battery)
```

**Navigation structure:** Bottom navigation bar with three primary destinations:
1. Dashboard
2. Profiles
3. Settings

Curve Editor is reached from either Dashboard (edit active profile shortcut) or Profiles List (select a profile to edit). It is not a bottom nav item.

Preview Mode is a modal overlay launched from within the Curve Editor or from the Dashboard.

---

## Screens

---

### 1. Dashboard

**Purpose:** The primary at-a-glance screen. Shows the current state of the app and provides the most common controls.

**Content:**

| Element | Description |
|---|---|
| Master toggle | On/off switch. Immediately triggers `SchedulerService.triggerNow()`. |
| Active profile name | Name of the currently active `CurveProfile`. Tappable — navigates to Profiles List. |
| Current warmth indicator | Visual representation of the current warmth value (0.0–1.0). |
| Current dimming indicator | Visual representation of the current dimming value (0.0–1.0). |
| Active mode badge | Shows whether `NATIVE` or `OVERLAY` mode is active. |
| "Last updated" timestamp | Time of last scheduler evaluation. Sourced from `last_evaluated_at` DataStore key. Helps users confirm the scheduler is running. |
| Edit curve shortcut | Button linking directly to the Curve Editor for the active profile. |
| Preview button | Opens Preview Mode for the active profile. |

**Empty state:** If no profile is active (no `CurveProfile` row with `is_active = 1`), show a prompt to create or select a profile. The master toggle is disabled in this state.

**UiState fields:**
```
isEnabled: Boolean
activeProfileName: String?
currentWarmth: Float
currentDimming: Float
activeMode: DisplayMode
lastEvaluatedAt: Long
hasActiveProfile: Boolean
```

---

### 2. Curve Editor

**Purpose:** Allows the user to create and edit the warmth and dimming adjustment points that define a profile's schedule.

**Content:**

| Element | Description |
|---|---|
| Curve graph | Time (X axis: 00:00–23:59) vs warmth and dimming (Y axis: 0.0–1.0). Two lines: one for warmth (warm colour), one for dimming (neutral/dark). |
| Curve points | Draggable nodes on the graph. Each node represents one `CurvePoint`. |
| Add point button | Tap on the graph or use a floating action button to add a new `CurvePoint` at the tapped time position. |
| Point detail panel | Appears when a point is selected. Shows exact time, warmth slider, dimming slider. Allows precise numeric entry. |
| Delete point button | Available in the point detail panel. Prompts for confirmation if it would leave the profile with zero points. |
| Save button | Persists all changes. Disabled if there are no unsaved changes. |
| Discard / Cancel | Navigates back without saving. Prompts for confirmation if there are unsaved changes. |
| Profile name (editable) | Header field showing and allowing edit of the profile's name. |

**Empty state:** If the profile has no points, show a visual prompt ("Tap the graph to add your first adjustment point.").

**Preview shortcut:** A clock icon button in the top bar launches Preview Mode scoped to the current (unsaved) curve state, so the user can preview their edits before saving.

**UiState fields:**
```
profileName: String
points: List<CurvePointUi>
selectedPointId: Long?
hasUnsavedChanges: Boolean
isSaving: Boolean
```

---

### 3. Profiles List

**Purpose:** Manage all saved `CurveProfiles`. Select the active profile, create new profiles, delete existing ones.

**Content:**

| Element | Description |
|---|---|
| Profile list | All saved profiles. Active profile shown with a visual indicator. |
| Tap to activate | Tapping a profile sets it as active. |
| Long press / swipe | Reveals delete action. |
| Create new button | Floating action button. Creates a new empty profile with a default name and navigates to the Curve Editor. |
| Edit button per item | Navigates to Curve Editor for that profile. |

**Empty state:** "No profiles yet. Create your first evening curve."

**Confirmation on delete:** Deleting a profile that is currently active asks the user to confirm. After deletion, no profile remains active (all `is_active` flags are cleared).

---

### 4. App Exclusions

**Purpose:** Define a list of installed applications for which display adjustments are automatically suspended when they are in the foreground.

**Content:**

| Element | Description |
|---|---|
| Search bar | Filters the installed app list by name. |
| Installed apps list | All user-installed apps, alphabetically sorted. Shows app icon and name. |
| Exclusion toggle | Per-app toggle. When enabled, the app package name is added to `ExcludedApp`. |
| Excluded count badge | Header showing how many apps are currently excluded. |

**Access path:** Reachable from Settings. Not a bottom nav destination (too infrequently accessed).

**Performance note:** The installed app list can be large. Loading must be done asynchronously. The list must be filtered reactively as the user types in the search field.

---

### 5. Settings

**Purpose:** App-level configuration. Mode selection, permission management, and advanced options.

**Content:**

| Section | Element | Description |
|---|---|---|
| Display Mode | Mode selector | Toggle or radio between `OVERLAY` and `NATIVE`. Native option is greyed out with explanation if device does not support it (`isSupported() = false`). |
| Permissions | Overlay permission | Shows current status. Button to navigate to system permission screen if not granted. |
| Permissions | Battery optimization | Shows current status. Button to request exemption. Explains why it matters. |
| App Exclusions | Link | "Manage excluded apps" — navigates to App Exclusions screen. |
| About | Version number | Displays `versionName`. |
| About | Open source licenses | Link to list of third-party licenses. |
| About | Source code | Link to GitHub repository. |

**Permission rationale:** If the user arrives at Settings from a prompt triggered by a missing permission, the relevant permission row is highlighted and a brief explanation is shown inline. Do not use dialog popups for permission rationale — inline contextual explanations are less disruptive.

---

### 6. Preview Mode

**Purpose:** Simulate any time of day and observe the warmth and dimming levels that would be applied by the active curve, without affecting the live display.

**Content:**

| Element | Description |
|---|---|
| Time scrubber | A horizontal slider representing 00:00–23:59. Dragging updates the preview in real time. |
| Warmth value | Numeric display of the interpolated warmth at the scrubbed time. |
| Dimming value | Numeric display of the interpolated dimming at the scrubbed time. |
| Visual preview | A colour swatch or screen tint preview that visually represents the warmth and dimming at the scrubbed time. Does NOT affect the actual screen overlay. |
| Current time marker | A marker on the scrubber showing the real current time for reference. |
| Close button | Dismisses Preview Mode. Live display state is unchanged. |

**Launched as:** A bottom sheet modal or a full-screen modal overlay, not a standalone navigation destination.

**Implementation note:** Preview Mode uses `CurveEngine.calculateDisplayState()` directly with the scrubbed `timeMinutes` value. It does not invoke `DisplayController`. All output is visual within the preview UI only.

---

## Navigation Events (UiEvents)

These are one-time events emitted from ViewModels via `SharedFlow`. They must not be emitted via `StateFlow` to avoid re-delivery on recomposition.

| Event | Source Screen | Action |
|---|---|---|
| `NavigateToCurveEditor(profileId)` | Dashboard, Profiles List | Open Curve Editor for the given profile |
| `NavigateToProfiles` | Dashboard | Open Profiles List |
| `NavigateToSettings` | Any | Open Settings |
| `NavigateToExclusions` | Settings | Open App Exclusions |
| `ShowDiscardChangesDialog` | Curve Editor | Prompt user before navigating away with unsaved changes |
| `ShowDeleteProfileConfirmation(profileId)` | Profiles List | Prompt user before deleting a profile |
| `NavigateToSystemPermission(permissionType)` | Settings | Open the system permission grant screen |
| `ShowSnackbar(message)` | Any | Display a non-blocking message (save success, error, etc.) |
