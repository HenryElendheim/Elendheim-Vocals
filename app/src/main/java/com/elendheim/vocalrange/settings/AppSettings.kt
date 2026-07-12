package com.elendheim.vocalrange.settings

import android.content.Context

// Small wrapper around SharedPreferences so screens never touch raw keys.
object AppSettings {
    private const val PREFS = "app_settings"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // true -> the sing screen shows just the note circle, no range tracking
    fun noteOnly(context: Context): Boolean =
        prefs(context).getBoolean("note_only", false)

    fun setNoteOnly(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean("note_only", value).apply()
    }

    // The note challenges start from, as a MIDI number. A3 = 57 suits most voices.
    fun challengeBase(context: Context): Int =
        prefs(context).getInt("challenge_base", 57)

    fun setChallengeBase(context: Context, midi: Int) {
        prefs(context).edit().putInt("challenge_base", midi).apply()
    }

    // How many times a challenge has been beaten.
    fun completions(context: Context, id: String): Int =
        prefs(context).getInt("done_$id", 0)

    fun addCompletion(context: Context, id: String) {
        prefs(context).edit().putInt("done_$id", completions(context, id) + 1).apply()
    }
}
