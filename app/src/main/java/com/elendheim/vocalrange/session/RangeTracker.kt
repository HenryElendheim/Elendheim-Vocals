package com.elendheim.vocalrange.session

import com.elendheim.vocalrange.music.Notes
import kotlin.math.roundToInt

/**
 * Turns a stream of pitch readings into a vocal range.
 * A note only counts once it has held steady for a moment, which keeps
 * breath noise and pitch flicker out of the result. Notes sustained for
 * a full second also count toward the comfortable range.
 */
class RangeTracker {
    companion object {
        private const val COMMIT_MS = 150L
        private const val COMFORT_MS = 1000L
    }

    private var currentMidi = Int.MIN_VALUE
    private var currentSince = 0L

    var absoluteLow = Int.MAX_VALUE
        private set
    var absoluteHigh = Int.MIN_VALUE
        private set
    var comfortableLow = Int.MAX_VALUE
        private set
    var comfortableHigh = Int.MIN_VALUE
        private set

    val hasRange: Boolean get() = absoluteHigh >= absoluteLow
    val hasComfortable: Boolean get() = comfortableHigh >= comfortableLow

    fun reset() {
        currentMidi = Int.MIN_VALUE
        currentSince = 0L
        absoluteLow = Int.MAX_VALUE
        absoluteHigh = Int.MIN_VALUE
        comfortableLow = Int.MAX_VALUE
        comfortableHigh = Int.MIN_VALUE
    }

    fun feed(timestampMillis: Long, hz: Float, voiced: Boolean) {
        if (!voiced) {
            currentMidi = Int.MIN_VALUE
            return
        }
        val midi = Notes.midiFromHz(hz.toDouble()).roundToInt()
        if (midi != currentMidi) {
            currentMidi = midi
            currentSince = timestampMillis
            return
        }
        val held = timestampMillis - currentSince
        if (held >= COMMIT_MS) {
            if (midi < absoluteLow) absoluteLow = midi
            if (midi > absoluteHigh) absoluteHigh = midi
            if (held >= COMFORT_MS) {
                if (midi < comfortableLow) comfortableLow = midi
                if (midi > comfortableHigh) comfortableHigh = midi
            }
        }
    }
}
