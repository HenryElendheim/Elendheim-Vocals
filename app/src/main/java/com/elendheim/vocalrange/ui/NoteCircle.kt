package com.elendheim.vocalrange.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// The big circle in the middle of the screen. It shows the note being sung
// right now, and can fill a ring around itself as progress for challenges.
@Composable
fun NoteCircle(
    note: String,
    detail: String,
    active: Boolean,
    progress: Float? = null, // 0..1 fills the ring, null -> plain circle
    modifier: Modifier = Modifier,
) {
    val ringColor = if (active) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outline
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val noteColor = if (active) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
    val detailColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(modifier = modifier.size(300.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 10.dp.toPx()
            val inset = stroke / 2
            // Faint full ring as the track
            drawCircle(
                color = trackColor,
                radius = size.minDimension / 2 - inset,
                style = Stroke(stroke),
            )
            if (progress == null) {
                // Sing mode: the whole ring lights up while a note is heard
                if (active) {
                    drawCircle(
                        color = ringColor,
                        radius = size.minDimension / 2 - inset,
                        style = Stroke(stroke),
                    )
                }
            } else {
                // Challenge mode: the ring fills clockwise as the note is held
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = progress * 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = note,
                fontSize = 80.sp,
                fontWeight = FontWeight.Bold,
                color = noteColor,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyLarge,
                color = detailColor,
            )
        }
    }
}
