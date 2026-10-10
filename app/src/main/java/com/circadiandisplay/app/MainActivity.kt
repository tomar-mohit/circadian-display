package com.circadiandisplay.app

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.circadiandisplay.app.ui.AppNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySystemBars()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavHost()
                }
            }
        }
    }

    /**
     * Applies system-bar styles that match the app's Compose content, which is pinned to the
     * light colour scheme.
     *
     * [enableEdgeToEdge]'s default [SystemBarStyle.auto] picks the glyph colour from the *system*
     * night mode rather than from the app's own theme. On a device in dark mode that draws light
     * glyphs on this app's near-white surfaces, which made the status bar clock and icons
     * unreadable — measured at roughly 1.03:1 contrast against a 3:1 accessibility floor for UI
     * components.
     *
     * Passing the styles explicitly means the window chrome can never disagree with the content.
     * When the in-app theme override lands, derive them from the resolved theme instead — see
     * DECISIONS.md #009 and #010.
     */
    private fun applySystemBars() {
        // API 26 cannot render dark navigation-bar glyphs at all (the capability arrived in API
        // 27), so a dark scrim is kept there to keep the glyphs legible. Every other API level
        // gets transparent bars with dark glyphs drawn on the app's light surfaces.
        val api26NavBarScrim = Color.argb(0x80, 0x1B, 0x1B, 0x1B)

        val navigationBarStyle =
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
                SystemBarStyle.dark(api26NavBarScrim)
            } else {
                SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
            }

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = navigationBarStyle,
        )
    }
}
