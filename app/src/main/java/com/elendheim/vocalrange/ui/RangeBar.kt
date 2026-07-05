package com.elendheim.vocalrange.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// The bar spans E1 (MIDI 28) to E6 (MIDI 88), which covers just about every voice.
private const val MIDI_MIN = 28f
private const val MIDI_MAX = 88f

/** A horizontal bar showing where a range sits within the span of human voices. */
@Composable
fun RangeBar(lowMidi: Int, highMidi: Int, color: Color, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val startFraction = ((lowMidi - MIDI_MIN) / (MIDI_MAX - MIDI_MIN)).coerceIn(0f, 1f)
        val endFraction = ((highMidi + 1 - MIDI_MIN) / (MIDI_MAX - MIDI_MIN)).coerceIn(0f, 1f)
        Box(
            Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Box(
            Modifier
                .padding(start = maxWidth * startFraction)
                .width((maxWidth * (endFraction - startFraction)).coerceAtLeast(6.dp))
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(color)
        )
    }
}
