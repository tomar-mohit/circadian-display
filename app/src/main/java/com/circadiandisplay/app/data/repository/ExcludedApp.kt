package com.circadiandisplay.app.data.repository

/**
 * Domain model for an excluded application.
 *
 * When an app with this package name is in the foreground, display
 * adjustments are suspended until the user leaves it.
 *
 * @property packageName Android package name (e.g. `com.google.android.camera`).
 *   Authoritative identifier — package names are unique per device.
 * @property displayName Human-readable name at the time of exclusion.
 *   Stored for display only and may become stale if the app is renamed.
 * @property addedAt     Unix timestamp (ms) of when the exclusion was added.
 */
data class ExcludedApp(
    val packageName: String,
    val displayName: String,
    val addedAt: Long,
)
