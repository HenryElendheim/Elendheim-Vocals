package com.elendheim.vocalrange.audio

/**
 * YIN fundamental frequency estimator (de Cheveigne and Kawahara, 2002).
 * Operates on one window of mono samples in the range [-1, 1].
 */
class YinPitchDetector(
    private val sampleRate: Int,
    windowSize: Int,
    private val threshold: Float = 0.15f,
) {
    private val half = windowSize / 2
    private val diff = FloatArray(half)

    /** Returns the detected pitch, or null when the window has no clear pitch. */
    fun detect(window: FloatArray): Pitch? {
        for (tau in 1 until half) {
            var sum = 0f
            for (i in 0 until half) {
                val delta = window[i] - window[i + tau]
                sum += delta * delta
            }
            diff[tau] = sum
        }

        // Cumulative mean normalized difference
        diff[0] = 1f
        var running = 0f
        for (tau in 1 until half) {
            running += diff[tau]
            diff[tau] = if (running > 0f) diff[tau] * tau / running else 1f
        }

        // First dip below the threshold, walked down to its local minimum
        var estimate = -1
        var tau = 2
        while (tau < half) {
            if (diff[tau] < threshold) {
                while (tau + 1 < half && diff[tau + 1] < diff[tau]) tau++
                estimate = tau
                break
            }
            tau++
        }
        if (estimate <= 0) return null

        val confidence = 1f - diff[estimate]
        val refined = interpolate(estimate)
        if (refined <= 0f) return null
        return Pitch(sampleRate / refined, confidence)
    }

    private fun interpolate(tau: Int): Float {
        if (tau < 1 || tau + 1 >= half) return tau.toFloat()
        val a = diff[tau - 1]
        val b = diff[tau]
        val c = diff[tau + 1]
        val denom = 2f * (a - 2f * b + c)
        if (denom == 0f) return tau.toFloat()
        return tau + (a - c) / denom
    }
}

data class Pitch(val hz: Float, val confidence: Float)
