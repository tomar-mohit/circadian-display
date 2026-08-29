# Data Model

This document describes the full persistent data model for Circadian Display, including Room entities, DataStore keys, their relationships, and the rationale behind key design choices.

---

## Entity Relationship Overview

```
CurveProfile (1) ──────── (N) CurvePoint
                                
ExcludedApp  (standalone)

AppSettings  (DataStore — not a Room entity)
```

`CurveProfile` has a one-to-many relationship with `CurvePoint`. Deleting a `CurveProfile` cascades to delete all of its `CurvePoints`.

`ExcludedApp` has no relationship to other entities. It is a simple list of package names.

`AppSettings` are stored in Jetpack DataStore (Preferences), not in Room. They have no relational structure.

---

## Room Entities

### CurveProfile

Represents a named comfort curve schedule.

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `INTEGER` | PRIMARY KEY, AUTOINCREMENT | Unique identifier. |
| `name` | `TEXT` | NOT NULL | User-defined display name. Must not be empty. |
| `is_active` | `INTEGER` (Boolean) | NOT NULL, DEFAULT 0 | Whether this profile is currently selected. `1` = active, `0` = inactive. |
| `created_at` | `INTEGER` (Long) | NOT NULL | Unix timestamp in milliseconds. Used for display ordering (newest first). |

**Invariant:** Only one `CurveProfile` row may have `is_active = 1` at any time. This is enforced at the **repository layer** in a single transaction: deactivate all profiles, then activate the selected one. It is not enforced with a database constraint to avoid complex trigger logic.

---

### CurvePoint

A single node on a `CurveProfile`'s adjustment schedule.

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `INTEGER` | PRIMARY KEY, AUTOINCREMENT | Unique identifier. |
| `profile_id` | `INTEGER` | NOT NULL, FOREIGN KEY → CurveProfile.id ON DELETE CASCADE | Owning profile. |
| `time_minutes` | `INTEGER` | NOT NULL | Minutes since midnight. Valid range: `0–1439`. |
| `warmth` | `REAL` | NOT NULL | Warmth level. Valid range: `0.0–1.0`. |
| `dimming` | `REAL` | NOT NULL | Dimming level. Valid range: `0.0–1.0`. |

**On `time_minutes`:**
Stored as a plain integer (minutes since midnight) rather than a timestamp or `LocalTime` string. This choice is intentional:
- No type converter required for Room.
- Integer comparison and sorting is trivially fast.
- Completely decoupled from timezones — all evaluations use device local time.
- Human-readable in debug logs (`time_minutes=1320` = 22:00).

**Range validation** (`0.0–1.0` for warmth/dimming, `0–1439` for time) is enforced at the domain/repository layer, not the database layer. Invalid values must be rejected before they are written.

**Ordering:** `CurvePoints` belonging to a profile are stored in insertion order but always sorted ascending by `time_minutes` before being passed to `CurveEngine`. The database does not guarantee order.

---

### ExcludedApp

An application whose package name has been added to the exclusion list. When an excluded app is in the foreground, display adjustments are suspended.

| Column | Type | Constraints | Description |
|---|---|---|---|
| `package_name` | `TEXT` | PRIMARY KEY | Android package name (e.g., `com.google.android.camera`). Used as the primary key — package names are unique per device. |
| `display_name` | `TEXT` | NOT NULL | Human-readable app name at the time of exclusion. Stored for display purposes only — the `package_name` is the authoritative identifier. |
| `added_at` | `INTEGER` (Long) | NOT NULL | Unix timestamp in milliseconds. Used for display ordering. |

**Note:** `display_name` may become stale if the user renames an app or updates it. The exclusion remains valid as long as `package_name` matches. UI should refresh `display_name` from the package manager on list load where possible.

---

## DataStore Keys (App Settings)

These are Preferences DataStore keys. They are flat, independent values with no relational structure.

| Key Name | Type | Default | Description |
|---|---|---|---|
| `mode` | `String` (enum name) | `"OVERLAY"` | Active display mode: `"OVERLAY"` or `"NATIVE"`. Stored as a string for human-readable DataStore inspection. Parsed to `DisplayMode` enum on read. |
| `is_enabled` | `Boolean` | `true` | Master on/off toggle. When `false`, the scheduler runs but always calls `DisplayController.clear()`. |
| `active_profile_id` | `Long` | `-1` | ID of the active `CurveProfile`. `-1` means no profile is selected. The scheduler treats `-1` as a disabled state. |
| `last_evaluated_at` | `Long` | `0` | Unix timestamp (ms) of the last successful scheduler evaluation. Used in the dashboard to show "last updated" status and in diagnostics. |

---

## Room Database Configuration

| Property | Value |
|---|---|
| Database name | `circadian_display.db` |
| Schema version | `1` (initial) |
| Export schema | `true` — schema JSON files must be committed to the repository for migration history tracking. |
| Migrations | Required for every version increment. Destructive migration is never acceptable in production builds. |

---

## Migration Policy

- Every schema change (adding a column, adding a table, renaming a column) requires a numbered `Migration` object.
- Migration scripts must be tested with `MigrationTestHelper` before release.
- Schema export JSON files (auto-generated by Room) must be committed alongside the migration code.
- Default values for new columns must be safe and non-breaking for existing users.

---

## Seed / Default Data

On first launch (fresh install with an empty database), the app creates one default `CurveProfile`:

```
Name:      "Evening"
isActive:  true
Points:
  - timeMinutes: 1080  (18:00)  warmth: 0.0   dimming: 0.0
  - timeMinutes: 1200  (20:00)  warmth: 0.4   dimming: 0.1
  - timeMinutes: 1320  (22:00)  warmth: 0.8   dimming: 0.3
  - timeMinutes: 1380  (23:00)  warmth: 1.0   dimming: 0.5
```

This default gives new users an immediately useful experience without requiring any setup. It can be edited or deleted like any other profile.
