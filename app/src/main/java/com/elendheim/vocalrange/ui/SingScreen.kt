package com.elendheim.vocalrange.ui

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.elendheim.vocalrange.audio.PitchEngine
import com.elendheim.vocalrange.music.Notes
import com.elendheim.vocalrange.session.RangeTracker
import com.elendheim.vocalrange.session.Session
import com.elendheim.vocalrange.session.SessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SingScreen(engine: PitchEngine, noteOnly: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tracker = remember { RangeTracker() }

    var recording by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf("--") }
    var detailText by remember { mutableStateOf(" ") }
    var voicedNow by remember { mutableStateOf(false) }
    var absLow by remember { mutableStateOf(-1) }
    var absHigh by remember { mutableStateOf(-1) }
    var comfLow by remember { mutableStateOf(-1) }
    var comfHigh by remember { mutableStateOf(-1) }
    var saved by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && engine.start()) recording = true
    }

    fun toggleRecording() {
        if (recording) {
            engine.stop()
            recording = false
            voicedNow = false
        } else {
            saved = false
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                if (engine.start()) recording = true
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    LaunchedEffect(recording) {
        if (!recording) return@LaunchedEffect
        engine.readings.collect { reading ->
            if (reading == null) return@collect
            tracker.feed(reading.timestampMillis, reading.hz, reading.voiced)
            voicedNow = reading.voiced
            if (reading.voiced) {
                val midiExact = Notes.midiFromHz(reading.hz.toDouble())
                val midi = midiExact.roundToInt()
                noteText = Notes.nameFor(midi)
                val cents = ((midiExact - midi) * 100).roundToInt()
                val centsLabel = when {
                    cents > 0 -> "+$cents cents"
                    cents < 0 -> "$cents cents"
                    else -> "dead on"
                }
                detailText = String.format(Locale.US, "%.1f Hz   %s", reading.hz, centsLabel)
            } else {
                detailText = "listening"
            }
            if (tracker.hasRange) {
                absLow = tracker.absoluteLow
                absHigh = tracker.absoluteHigh
            }
            if (tracker.hasComfortable) {
                comfLow = tracker.comfortableLow
                comfHigh = tracker.comfortableHigh
            }
        }
    }

    // Let go of the microphone when the app leaves the screen
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && recording) {
                engine.stop()
                recording = false
                voicedNow = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun resetSession() {
        tracker.reset()
        absLow = -1
        absHigh = -1
        comfLow = -1
        comfHigh = -1
        noteText = "--"
        detailText = " "
        saved = false
    }

    fun saveSession() {
        if (!tracker.hasRange || saved) return
        val session = Session(
            timestampMillis = System.currentTimeMillis(),
            absoluteLow = tracker.absoluteLow,
            absoluteHigh = tracker.absoluteHigh,
            comfortableLow = if (tracker.hasComfortable) tracker.comfortableLow else -1,
            comfortableHigh = if (tracker.hasComfortable) tracker.comfortableHigh else -1,
        )
        scope.launch(Dispatchers.IO) { SessionStore.append(context, session) }
        saved = true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(16.dp))
            NoteCircle(
                note = noteText,
                detail = if (recording) detailText else "tap record and sing",
                active = voicedNow,
            )
        }

        // Note only mode hides the range card -> just you and the circle
        if (!noteOnly) {
            RangeCard(absLow, absHigh, comfLow, comfHigh)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(
                onClick = { toggleRecording() },
                shape = CircleShape,
                modifier = Modifier.size(112.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (recording) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.primary,
                    contentColor = if (recording) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(if (recording) "Stop" else "Record", fontSize = 18.sp)
            }
            if (!noteOnly) {
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { saveSession() },
                        enabled = absHigh >= absLow && absLow >= 0 && !saved,
                    ) {
                        Text(if (saved) "Saved" else "Save session")
                    }
                    OutlinedButton(
                        onClick = { resetSession() },
                        enabled = absHigh >= absLow && absLow >= 0,
                    ) {
                        Text("Reset")
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun RangeCard(absLow: Int, absHigh: Int, comfLow: Int, comfHigh: Int) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            if (absLow < 0 || absHigh < absLow) {
                Text(
                    "Your range shows up here. Slide down to your lowest " +
                        "comfortable note, then up to your highest.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    "Range  ${Notes.nameFor(absLow)} - ${Notes.nameFor(absHigh)}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "${absHigh - absLow} semitones",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                RangeBar(absLow, absHigh, MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                if (comfLow in 0..comfHigh) {
                    Text(
                        "Comfortable  ${Notes.nameFor(comfLow)} - ${Notes.nameFor(comfHigh)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(10.dp))
                    RangeBar(comfLow, comfHigh, MaterialTheme.colorScheme.secondary)
                } else {
                    Text(
                        "Hold a note for a second and it counts as comfortable.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
