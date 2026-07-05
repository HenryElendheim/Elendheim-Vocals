package com.elendheim.vocalrange.session

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException

/**
 * Saves sessions as a JSON file, either in a folder the user picked
 * (kept across restarts with a persisted permission) or in app storage
 * when no folder has been chosen.
 */
object SessionStore {
    private const val PREFS = "vocal_range"
    private const val KEY_FOLDER = "folder_uri"
    private const val FILE_NAME = "vocal-range-sessions.json"

    fun folderUri(context: Context): Uri? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_FOLDER, null)
            ?.let(Uri::parse)

    fun folderName(context: Context): String? {
        val uri = folderUri(context) ?: return null
        return DocumentFile.fromTreeUri(context, uri)?.name
    }

    fun setFolderUri(context: Context, uri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FOLDER, uri.toString())
            .apply()

        // Carry anything saved in app storage over to the picked folder
        val internal = internalFile(context)
        if (internal.exists()) {
            val carried = parse(internal.readText())
            if (carried.isNotEmpty() && readExternal(context) == null) {
                writeExternal(context, serialize(carried))
            }
            internal.delete()
        }
    }

    fun load(context: Context): List<Session> {
        val external = readExternal(context)
        if (external != null) return parse(external)
        val internal = internalFile(context)
        if (!internal.exists()) return emptyList()
        return parse(internal.readText())
    }

    fun append(context: Context, session: Session) {
        val sessions = load(context) + session
        val json = serialize(sessions)
        if (folderUri(context) == null || !writeExternal(context, json)) {
            internalFile(context).writeText(json)
        }
    }

    private fun internalFile(context: Context): File = File(context.filesDir, FILE_NAME)

    private fun externalFile(context: Context, create: Boolean): DocumentFile? {
        val uri = folderUri(context) ?: return null
        val dir = DocumentFile.fromTreeUri(context, uri) ?: return null
        val existing = dir.findFile(FILE_NAME)
        if (existing != null) return existing
        return if (create) dir.createFile("application/json", FILE_NAME) else null
    }

    private fun readExternal(context: Context): String? {
        val file = externalFile(context, create = false) ?: return null
        return try {
            context.contentResolver.openInputStream(file.uri)?.use {
                it.readBytes().toString(Charsets.UTF_8)
            }
        } catch (e: IOException) {
            null
        } catch (e: SecurityException) {
            null
        }
    }

    private fun writeExternal(context: Context, json: String): Boolean {
        val file = externalFile(context, create = true) ?: return false
        return try {
            context.contentResolver.openOutputStream(file.uri, "wt")?.use {
                it.write(json.toByteArray(Charsets.UTF_8))
                true
            } ?: false
        } catch (e: IOException) {
            false
        } catch (e: SecurityException) {
            false
        }
    }

    private fun serialize(sessions: List<Session>): String {
        val array = JSONArray()
        for (s in sessions) {
            array.put(
                JSONObject().apply {
                    put("time", s.timestampMillis)
                    put("absoluteLow", s.absoluteLow)
                    put("absoluteHigh", s.absoluteHigh)
                    put("comfortableLow", s.comfortableLow)
                    put("comfortableHigh", s.comfortableHigh)
                }
            )
        }
        return array.toString(2)
    }

    private fun parse(text: String): List<Session> {
        return try {
            val array = JSONArray(text)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                Session(
                    timestampMillis = o.getLong("time"),
                    absoluteLow = o.getInt("absoluteLow"),
                    absoluteHigh = o.getInt("absoluteHigh"),
                    comfortableLow = o.optInt("comfortableLow", -1),
                    comfortableHigh = o.optInt("comfortableHigh", -1),
                )
            }
        } catch (e: JSONException) {
            emptyList()
        }
    }
}
