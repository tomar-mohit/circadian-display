package com.circadiandisplay.app.data.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeModeTest {
    @Test
    fun system_followsDeviceSetting() {
        assertTrue(ThemeMode.SYSTEM.isDark(systemInDarkTheme = true))
        assertFalse(ThemeMode.SYSTEM.isDark(systemInDarkTheme = false))
    }

    @Test
    fun explicitModes_ignoreDeviceSetting() {
        assertTrue(ThemeMode.DARK.isDark(systemInDarkTheme = false))
        assertFalse(ThemeMode.LIGHT.isDark(systemInDarkTheme = true))
    }

    @Test
    fun fromName_roundTripsEveryMode() {
        ThemeMode.entries.forEach { mode ->
            assertEquals(mode, ThemeMode.fromName(mode.name))
        }
    }

    @Test
    fun fromName_missingOrUnknown_fallsBackToSystem() {
        // A missing key must not require a migration for installs that predate the setting.
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromName(null))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromName(""))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromName("SEPIA"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.default)
    }
}
