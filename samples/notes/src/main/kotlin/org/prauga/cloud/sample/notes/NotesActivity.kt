// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.cloud.sample.notes

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import java.io.File

class NotesActivity : Activity() {

    private lateinit var screen: SampleScreen
    private val notes by lazy { File(filesDir, "notes/notes.txt") }
    private val prefs by lazy { getSharedPreferences("notes", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        screen = SampleScreen(this)
        screen.button(this, R.string.add_note) { addNote() }
        render()
    }

    private fun addNote() {
        val number = prefs.getInt(KEY_COUNT, 0) + 1
        notes.parentFile?.mkdirs()
        notes.appendText(getString(R.string.note_text, number) + "\n")
        database().use { db ->
            db.insert(TABLE, null, ContentValues().apply { put(COLUMN, number) })
        }
        prefs.edit().putInt(KEY_COUNT, number).apply()
        render()
    }

    private fun render() {
        val lines = if (notes.isFile) notes.readLines().filter(String::isNotBlank) else emptyList()
        val rows =
            database().use { db ->
                db.rawQuery("SELECT COUNT(*) FROM $TABLE", null).use {
                    it.moveToFirst()
                    it.getInt(0)
                }
            }
        screen.status.text =
            getString(
                R.string.status,
                lines.size,
                rows,
                prefs.getInt(KEY_COUNT, 0),
                lines.lastOrNull().orEmpty(),
            )
    }

    private fun database(): SQLiteDatabase =
        openOrCreateDatabase("notes.db", Context.MODE_PRIVATE, null).apply {
            execSQL("CREATE TABLE IF NOT EXISTS $TABLE ($COLUMN INTEGER)")
        }

    private companion object {
        const val KEY_COUNT = "count"
        const val TABLE = "note"
        const val COLUMN = "number"
    }
}
