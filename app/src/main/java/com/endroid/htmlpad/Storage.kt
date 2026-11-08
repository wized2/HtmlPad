package com.endroid.htmlpad

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

/** The text (and caret) that is restored when the app is opened again. */
data class Draft(val text: String, val cursor: Int)

/** Autosave of the current text, kept private to the app. */
class DraftStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("htmlpad_draft", Context.MODE_PRIVATE)

    fun load(): Draft? {
        val text = prefs.getString(KEY_TEXT, null) ?: return null
        val cursor = prefs.getInt(KEY_CURSOR, text.length).coerceIn(0, text.length)
        return Draft(text, cursor)
    }

    fun save(text: String, cursor: Int) {
        prefs.edit()
            .putString(KEY_TEXT, text)
            .putInt(KEY_CURSOR, cursor.coerceIn(0, text.length))
            .apply()
    }

    private companion object {
        const val KEY_TEXT = "text"
        const val KEY_CURSOR = "cursor"
    }
}

/** Reading and writing .html files chosen with the system file picker. */
object FileIo {
    /** Files bigger than this are refused so a wrong pick can't exhaust memory. */
    const val MAX_CHARS = 2_000_000

    /** Returns the file text, or null if it couldn't be read or is too large. */
    fun read(context: Context, uri: Uri): String? = try {
        context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { reader ->
            val sb = StringBuilder()
            val buf = CharArray(8192)
            var tooLarge = false
            while (true) {
                val n = reader.read(buf)
                if (n < 0) break
                sb.append(buf, 0, n)
                if (sb.length > MAX_CHARS) {
                    tooLarge = true
                    break
                }
            }
            if (tooLarge) null else sb.toString()
        }
    } catch (e: Exception) {
        null
    }

    /** Overwrites the file with [text]. Returns false if it couldn't be written. */
    fun write(context: Context, uri: Uri, text: String): Boolean = try {
        val out = context.contentResolver.openOutputStream(uri, "wt")
        if (out == null) {
            false
        } else {
            out.bufferedWriter(Charsets.UTF_8).use { it.write(text) }
            true
        }
    } catch (e: Exception) {
        false
    }

    fun displayName(context: Context, uri: Uri): String? = try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    } catch (e: Exception) {
        null
    }
}
