package com.elendheim.vocalrange.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.elendheim.vocalrange.session.SessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    noteOnly: Boolean,
    onNoteOnlyChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var folderName by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val name = SessionStore.folderName(context)
            withContext(Dispatchers.Main) { folderName = name }
        }
    }

    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                SessionStore.setFolderUri(context, uri)
                val name = SessionStore.folderName(context)
                withContext(Dispatchers.Main) {
                    folderName = name
                    status = "Sessions now save to ${name ?: "the picked folder"}"
                }
            }
        }
    }

    // Export writes the session file wherever the user points it
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val ok = SessionStore.exportTo(context, uri)
                withContext(Dispatchers.Main) {
                    status = if (ok) "Sessions exported" else "Export failed"
                }
            }
        }
    }

    // Import pulls sessions in from a file and skips ones already saved
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val added = SessionStore.importFrom(context, uri)
                withContext(Dispatchers.Main) {
                    status = when {
                        added < 0 -> "Could not read that file"
                        added == 0 -> "Nothing new in that file"
                        added == 1 -> "Imported 1 session"
                        else -> "Imported $added sessions"
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Note only mode", style = MaterialTheme.typography.titleMedium)
                Text(
                    "The sing screen shows just the note circle, no range tracking",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = noteOnly, onCheckedChange = onNoteOnlyChange)
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        Text("Sessions", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "Saved to ${folderName ?: "app storage"}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { folderLauncher.launch(null) }) {
                Text("Choose folder")
            }
            OutlinedButton(onClick = {
                exportLauncher.launch("vocal-range-sessions.json")
            }) {
                Text("Export")
            }
            OutlinedButton(onClick = {
                importLauncher.launch(
                    arrayOf("application/json", "text/plain", "application/octet-stream")
                )
            }) {
                Text("Import")
            }
        }
        if (status.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                status,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(Modifier.weight(1f))
        Text(
            "Elendheim Vocal Range 2.0",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
    }
}
