package com.elendheim.vocalrange.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Pink = Color(0xFFEA66AC)
val PinkLight = Color(0xFFF5A8CE)
val Plum = Color(0xFF6E2A4E)
val Ink = Color(0xFF2A0E1E)

private val DarkColors = darkColorScheme(
    primary = Pink,
    onPrimary = Ink,
    primaryContainer = Plum,
    onPrimaryContainer = PinkLight,
    secondary = PinkLight,
    onSecondary = Ink,
    background = Color(0xFF160B11),
    onBackground = Color(0xFFF7E6EF),
    surface = Color(0xFF201018),
    onSurface = Color(0xFFF7E6EF),
    surfaceVariant = Color(0xFF2C1720),
    onSurfaceVariant = Color(0xFFD9B8C9),
    outline = Color(0xFF7A5468),
)

/** The app is dark mode first: one dark theme, always on. */
@Composable
fun ElendheimTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
