package com.circadiandisplay.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.circadiandisplay.core.curve.DisplayMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

/**
 * Flat key-value settings backed by Jetpack DataStore (Preferences).
 *
 * Stores app-level configuration that has no relational structure:
 * display mode, master toggle, active profile ID, and diagnostics.
 */
@Singleton
class AppSettings @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val MODE = stringPreferencesKey("mode")
        val IS_ENABLED = booleanPreferencesKey("is_enabled")
        val ACTIVE_PROFILE_ID = longPreferencesKey("active_profile_id")
        val LAST_EVALUATED_AT = longPreferencesKey("last_evaluated_at")
    }

    // ── Display Mode ──────────────────────────────────────────────────────

    val displayMode: Flow<DisplayMode> = context.dataStore.data.map { prefs ->
        val name = prefs[Keys.MODE] ?: DisplayMode.OVERLAY.name
        runCatching { DisplayMode.valueOf(name) }.getOrDefault(DisplayMode.OVERLAY)
    }

    suspend fun getDisplayModeSnapshot(): DisplayMode {
        val prefs = context.dataStore.data.first()
        val name = prefs[Keys.MODE] ?: DisplayMode.OVERLAY.name
        return runCatching { DisplayMode.valueOf(name) }.getOrDefault(DisplayMode.OVERLAY)
    }

    suspend fun setDisplayMode(mode: DisplayMode) {
        context.dataStore.edit { it[Keys.MODE] = mode.name }
    }

    // ── Master Toggle ─────────────────────────────────────────────────────

    val isEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.IS_ENABLED] ?: true
    }

    suspend fun isEnabledSnapshot(): Boolean {
        val prefs = context.dataStore.data.first()
        return prefs[Keys.IS_ENABLED] ?: true
    }

    suspend fun setEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.IS_ENABLED] = enabled }
    }

    // ── Active Profile ────────────────────────────────────────────────────

    val activeProfileId: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[Keys.ACTIVE_PROFILE_ID] ?: -1L
    }

    suspend fun getActiveProfileIdSnapshot(): Long {
        val prefs = context.dataStore.data.first()
        return prefs[Keys.ACTIVE_PROFILE_ID] ?: -1L
    }

    suspend fun setActiveProfileId(id: Long) {
        context.dataStore.edit { it[Keys.ACTIVE_PROFILE_ID] = id }
    }

    // ── Diagnostics ───────────────────────────────────────────────────────

    val lastEvaluatedAt: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[Keys.LAST_EVALUATED_AT] ?: 0L
    }

    suspend fun setLastEvaluatedAt(timestampMs: Long) {
        context.dataStore.edit { it[Keys.LAST_EVALUATED_AT] = timestampMs }
    }
}

