package com.elendheim.vocalrange.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.elendheim.vocalrange.audio.PitchEngine
import com.elendheim.vocalrange.challenges.CHALLENGES
import com.elendheim.vocalrange.challenges.Challenge
import com.elendheim.vocalrange.music.Notes
import com.elendheim.vocalrange.settings.AppSettings
import kotlin.math.roundToInt

@Composable
fun ChallengesScreen(engine: PitchEngine, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var playing by remember { mutableStateOf<Challenge?>(null) }
    var base by remember { mutableStateOf(AppSettings.challengeBase(context)) }
    // Bumped when a play round ends so the completion counts refresh
    var refresh by remember { mutableStateOf(0) }

    val current = playing
    if (current != null) {
        ChallengePlay(
            engine = engine,
            challenge = current,
            base = base,
            onExit = {
                playing = null
                refresh++
            },
            modifier = modifier,
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text("Challenges", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))

        // Base note picker: every challenge is sung relative to this note,
        // so drop it for deep voices or raise it for high ones
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Base note  ${Notes.nameFor(base)}",
                style = MaterialTheme.typography.titleMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    if (base > 36) {
                        base -= 1
                        AppSettings.setChallengeBase(context, base)
                    }
                }) { Text("Lower") }
                OutlinedButton(onClick = {
                    if (base < 76) {
                        base += 1
                        AppSettings.setChallengeBase(context, base)
                    }
                }) { Text("Higher") }
            }
        }
        Spacer(Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(CHALLENGES) { challenge ->
                val done = remember(refresh) { AppSettings.completions(context, challenge.id) }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { playing = challenge },
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(challenge.name, style = MaterialTheme.typography.titleMedium)
                            if (done > 0) {
                                Text(
                                    if (done == 1) "beaten once" else "beaten $done times",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            challenge.blurb,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            challenge.steps.joinToString("  ") { Notes.nameFor(base + it) },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun ChallengePlay(
    engine: PitchEngine,
    challenge: Challenge,
    base: Int,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var stepIndex by remember { mutableStateOf(0) }
    var progress by remember { mutableStateOf(0f) }
    var noteText by remember { mutableStateOf("--") }
    var active by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    var micReady by remember { mutableStateOf(false) }
    var micDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) micReady = engine.start() else micDenied = true
    }

    // Start listening the moment the challenge opens
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) micReady = engine.start()
        else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    // Always let the microphone go when this screen closes
    DisposableEffect(Unit) {
        onDispose { engine.stop() }
    }

    LaunchedEffect(micReady) {
        if (!micReady) return@LaunchedEffect
        var heldMs = 0L
        var lastGoodTs = 0L
        engine.readings.collect { reading ->
            if (reading == null || finished) return@collect
            val target = base + challenge.steps[stepIndex]
            if (reading.voiced) {
                val midi = Notes.midiFromHz(reading.hz.toDouble()).roundToInt()
                noteText = Notes.nameFor(midi)
                active = true
                if (midi == target) {
                    // Right note -> keep adding up the time it is held
                    if (lastGoodTs > 0L) heldMs += reading.timestampMillis - lastGoodTs
                    lastGoodTs = reading.timestampMillis
                } else {
                    // Wrong note -> the hold starts over
                    lastGoodTs = 0L
                    heldMs = 0L
                }
            } else {
                active = false
                lastGoodTs = 0L
                heldMs = 0L
            }
            progress = (heldMs.toFloat() / challenge.holdMs).coerceIn(0f, 1f)
            if (heldMs >= challenge.holdMs) {
                heldMs = 0L
                lastGoodTs = 0L
                if (stepIndex + 1 >= challenge.steps.size) {
                    finished = true
                    progress = 1f
                    AppSettings.addCompletion(context, challenge.id)
                } else {
                    stepIndex += 1
                    progress = 0f
                }
            }
        }
    }

    val target = base + challenge.steps[stepIndex.coerceAtMost(challenge.steps.size - 1)]

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(8.dp))
            Text(challenge.name, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                text = when {
                    finished -> "Challenge complete"
                    micDenied -> "Microphone permission needed"
                    else -> "Sing ${Notes.nameFor(target)}"
                },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(12.dp))
            // One dot per note in the challenge, filled once it is done
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                challenge.steps.indices.forEach { index ->
                    Box(
                        Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                if (finished || index < stepIndex) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }
        }

        NoteCircle(
            note = noteText,
            detail = when {
                finished -> "nice singing"
                active -> "hold it steady"
                else -> "waiting for you"
            },
            active = active || finished,
            progress = progress,
        )

        OutlinedButton(onClick = onExit) {
            Text(if (finished) "Back to challenges" else "Give up")
        }
    }
}
