package com.elendheim.vocalrange.music

import kotlin.math.log2

object Notes {
    private val NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    /** Fractional MIDI note number for a frequency, anchored at A4 = 440 Hz = MIDI 69. */
    fun midiFromHz(hz: Double): Double = 69.0 + 12.0 * log2(hz / 440.0)

    /** Note name with octave, for example MIDI 57 is "A3". */
    fun nameFor(midi: Int): String {
        val name = NAMES[((midi % 12) + 12) % 12]
        val octave = midi / 12 - 1
        return "$name$octave"
    }
}
