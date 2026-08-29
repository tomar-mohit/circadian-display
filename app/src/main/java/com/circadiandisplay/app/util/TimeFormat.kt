package com.circadiandisplay.app.util

import java.util.Calendar

/** Current wall-clock time in minutes since midnight (0–1439). */
fun currentTimeMinutes(): Int {
    val cal = Calendar.getInstance()
    return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
}

/** Formats minutes since midnight as a 24-hour "HH:MM" string. */
fun minutesToTimeString(minutes: Int): String {
    val hours = (minutes / 60).coerceIn(0, 23)
    val mins = (minutes % 60).coerceIn(0, 59)
    return "%02d:%02d".format(hours, mins)
}
