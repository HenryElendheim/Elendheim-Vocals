package com.elendheim.vocalrange.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.concurrent.thread
import kotlin.math.sqrt

/** Reads the microphone on a worker thread and streams pitch readings. */
class PitchEngine {
    companion object {
        const val SAMPLE_RATE = 44100
        const val WINDOW_SIZE = 4096
        const val HOP_SIZE = 2048
        private const val RMS_GATE = 0.008f
        private const val MIN_HZ = 40f
        private const val MAX_HZ = 2000f
    }

    private val detector = YinPitchDetector(SAMPLE_RATE, WINDOW_SIZE)
    private val _readings = MutableStateFlow<Reading?>(null)
    val readings: StateFlow<Reading?> = _readings

    @Volatile private var running = false
    private var worker: Thread? = null

    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        if (running) return true
        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) return false
        val record = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minBuffer, WINDOW_SIZE * 4)
            )
        } catch (e: IllegalArgumentException) {
            return false
        }
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            return false
        }
        running = true
        worker = thread(name = "pitch-engine") {
            val hop = ShortArray(HOP_SIZE)
            val window = FloatArray(WINDOW_SIZE)
            var filled = 0
            record.startRecording()
            try {
                while (running) {
                    var read = 0
                    while (read < HOP_SIZE && running) {
                        val n = record.read(hop, read, HOP_SIZE - read)
                        if (n < 0) {
                            running = false
                            break
                        }
                        read += n
                    }
                    if (read < HOP_SIZE) continue

                    if (filled < WINDOW_SIZE) {
                        for (i in 0 until HOP_SIZE) window[filled + i] = hop[i] / 32768f
                        filled += HOP_SIZE
                        if (filled < WINDOW_SIZE) continue
                    } else {
                        System.arraycopy(window, HOP_SIZE, window, 0, WINDOW_SIZE - HOP_SIZE)
                        for (i in 0 until HOP_SIZE) {
                            window[WINDOW_SIZE - HOP_SIZE + i] = hop[i] / 32768f
                        }
                    }

                    val now = System.currentTimeMillis()
                    var sum = 0.0
                    for (s in window) sum += (s * s).toDouble()
                    val rms = sqrt(sum / WINDOW_SIZE).toFloat()
                    if (rms < RMS_GATE) {
                        _readings.value = Reading.silence(now)
                        continue
                    }

                    val pitch = detector.detect(window)
                    _readings.value = if (pitch != null && pitch.hz in MIN_HZ..MAX_HZ) {
                        Reading(now, pitch.hz, pitch.confidence, voiced = true)
                    } else {
                        Reading.silence(now)
                    }
                }
            } finally {
                try {
                    record.stop()
                } catch (_: IllegalStateException) {
                }
                record.release()
            }
        }
        return true
    }

    fun stop() {
        running = false
        worker?.join(1000)
        worker = null
        _readings.value = null
    }
}

data class Reading(
    val timestampMillis: Long,
    val hz: Float,
    val confidence: Float,
    val voiced: Boolean,
) {
    companion object {
        fun silence(timestampMillis: Long) = Reading(timestampMillis, 0f, 0f, voiced = false)
    }
}
