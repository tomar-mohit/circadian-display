package com.circadiandisplay.app

import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.circadiandisplay.app.data.settings.AppSettings
import com.circadiandisplay.app.data.settings.ThemeMode
import com.circadiandisplay.app.ui.AppNavHost
import com.circadiandisplay.app.ui.theme.CircadianDisplayTheme
import com.circadiandisplay.app.ui.theme.isDarkTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Single activity host.
 *
 * Resolves the appearance preference, applies the colour scheme, and keeps the window chrome in
 * step with it. The chrome must always match the *app's* theme, never the system night mode on its
 * own - that mismatch is what made the status bar unreadable in dark mode (DECISIONS.md #009).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var appSettings: AppSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Provisional chrome from the system setting so the first frame is already legible. The
        // saved preference is read asynchronously, so a user who overrides *against* the system
        // may see one frame of the wrong chrome before the effect below corrects it. Accepted in
        // DECISIONS.md #010 rather than blocking the first frame on a disk read.
        applySystemBars(isDark = systemInDarkTheme())

        setContent {
            val themeMode by appSettings.themeMode.collectAsStateWithLifecycle(
                initialValue = ThemeMode.default,
            )
            val isDark = themeMode.isDarkTheme()

            LaunchedEffect(isDark) { applySystemBars(isDark) }

            CircadianDisplayTheme(themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavHost()
                }
            }
        }
    }

    /** The device's dark-mode setting, readable outside composition. */
    private fun systemInDarkTheme(): Boolean =
        (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

    /**
     * Applies system-bar styles that match the resolved scheme: light glyphs on the dark scheme,
     * dark glyphs on the light one.
     *
     * [enableEdgeToEdge]'s default [SystemBarStyle.auto] derives the glyph colour from the system
     * night mode, which disagrees with the app whenever the user overrides the theme. Passing the
     * styles explicitly means the chrome cannot drift from the content.
     */
    private fun applySystemBars(isDark: Boolean) {
        // API 26 cannot render dark navigation-bar glyphs at all (the capability arrived in API
        // 27), so it always gets a dark bar with light glyphs.
        val api26NavBarScrim = Color.argb(0x80, 0x1B, 0x1B, 0x1B)

        val navigationBarStyle =
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
                SystemBarStyle.dark(api26NavBarScrim)
            } else {
                glyphStyle(isDark)
            }

        enableEdgeToEdge(
            statusBarStyle = glyphStyle(isDark),
            navigationBarStyle = navigationBarStyle,
        )
    }

    /** Transparent bars with glyphs that contrast against the app's own surfaces. */
    private fun glyphStyle(isDark: Boolean): SystemBarStyle =
        if (isDark) {
            SystemBarStyle.dark(Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        }
}
