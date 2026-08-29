package com.circadiandisplay.core.curve

/**
 * The computed screen adjustment to apply at a given moment.
 *
 * Produced by [CurveEngine.calculateDisplayState] and consumed
 * by [DisplayController.apply].
 *
 * @property warmth  Normalized warmth level: `0.0` = no color shift, `1.0` = maximum warm.
 * @property dimming Normalized dimming level: `0.0` = no dimming, `1.0` = maximum dim.
 */
data class DisplayState(
    val warmth: Float,
    val dimming: Float,
)
