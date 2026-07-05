package com.elendheim.vocalrange.session

/** One recorded session. Comfortable values are -1 when nothing was sustained long enough. */
data class Session(
    val timestampMillis: Long,
    val absoluteLow: Int,
    val absoluteHigh: Int,
    val comfortableLow: Int,
    val comfortableHigh: Int,
)
