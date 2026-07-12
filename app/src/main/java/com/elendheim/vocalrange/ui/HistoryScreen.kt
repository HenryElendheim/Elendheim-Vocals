package com.elendheim.vocalrange.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.elendheim.vocalrange.music.Notes
import com.elendheim.vocalrange.session.Session
import com.elendheim.vocalrange.session.SessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var sessions by remember { mutableStateOf<List<Session>>(emptyList()) }

    // Reload every time this tab opens so new saves show up right away
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            val loaded = SessionStore.load(context).sortedByDescending { it.timestampMillis }
            withContext(Dispatchers.Main) { sessions = loaded }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text("History", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))

        if (sessions.isEmpty()) {
            Text(
                "No sessions yet. Record one on the Sing tab and save it.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(sessions) { session -> SessionRow(session) }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }
}

@Composable
private fun SessionRow(session: Session) {
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                dateFormat.format(Date(session.timestampMillis)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${Notes.nameFor(session.absoluteLow)} - ${Notes.nameFor(session.absoluteHigh)}" +
                    "   (${session.absoluteHigh - session.absoluteLow} semitones)",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
            RangeBar(session.absoluteLow, session.absoluteHigh, MaterialTheme.colorScheme.primary)
            if (session.comfortableLow in 0..session.comfortableHigh) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Comfortable ${Notes.nameFor(session.comfortableLow)} - " +
                        Notes.nameFor(session.comfortableHigh),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                RangeBar(
                    session.comfortableLow,
                    session.comfortableHigh,
                    MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}
