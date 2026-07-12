package com.elendheim.vocalrange.challenges

// One challenge is a list of notes to hit in order.
// Steps are semitone offsets from the base note the player picks,
// so every challenge works for deep and high voices alike.
data class Challenge(
    val id: String,
    val name: String,
    val blurb: String,
    val steps: List<Int>,
    val holdMs: Long, // how long each note must be held to count
)

val CHALLENGES = listOf(
    Challenge("warmup", "Warm Up", "Hold your base note for two seconds", listOf(0), 2000),
    Challenge("steps3", "Three Steps", "Climb the first three scale notes", listOf(0, 2, 4), 1000),
    Challenge("five", "Five Alive", "Sing the first five notes of the scale", listOf(0, 2, 4, 5, 7), 800),
    Challenge("bounce", "Bounce Back", "Up a third and back home", listOf(0, 4, 0), 1000),
    Challenge("leap5", "The Leap", "Jump straight up to the fifth", listOf(0, 7), 1500),
    Challenge("descend", "Come Down", "Walk down from the fifth to home", listOf(7, 5, 4, 2, 0), 800),
    Challenge("longhaul", "Long Haul", "One note, five whole seconds", listOf(0), 5000),
    Challenge("octave", "Moon Shot", "Hit the octave above your base note", listOf(0, 12), 1500),
)
