package com.circadiandisplay.app.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.circadiandisplay.app.util.minutesToTimeString
import com.circadiandisplay.core.curve.DisplayState
import kotlin.math.roundToInt

/**
 * Shared preview body used by both the full Preview screen and the in-editor
 * preview dialog. Renders a color swatch, a time scrubber, and value readouts.
 */
@Composable
fun PreviewContent(
    displayState: DisplayState,
    timeMinutes: Int,
    currentTimeMinutes: Int,
    onTimeChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(previewColor(displayState.warmth, displayState.dimming)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = minutesToTimeString(timeMinutes),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Color.Black.copy(alpha = 0.65f),
            )
        }

        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Simulated time", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = minutesToTimeString(timeMinutes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Slider(
                value = timeMinutes.toFloat(),
                onValueChange = { onTimeChange(it.roundToInt()) },
                valueRange = 0f..1439f,
            )
        }

        Text(
            text = "Actual time now: ${minutesToTimeString(currentTimeMinutes)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PreviewValueCard("Warmth", displayState.warmth, Modifier.weight(1f))
            PreviewValueCard("Dimming", displayState.dimming, Modifier.weight(1f))
        }
    }
}

@Composable
private fun PreviewValueCard(
    label: String,
    value: Float,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${(value * 100).toInt()}%",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/** Approximates the on-screen result: amber warmth layered over black dimming. */
fun previewColor(
    warmth: Float,
    dimming: Float,
): Color {
    val neutral = Color(0xFFFFFFFF)
    val amber = Color(0xFFFFB74D)
    val warm = lerp(neutral, amber, warmth.coerceIn(0f, 1f))
    val factor = 1f - dimming.coerceIn(0f, 1f)
    return Color(warm.red * factor, warm.green * factor, warm.blue * factor, 1f)
}
