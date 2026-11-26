// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.cloud.sample.ledger

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File

internal class Ledger(private val context: Context) {
    val budget = File(context.filesDir, "budget.txt")
    val draft = File(context.filesDir, "draft.txt")
    val settings = context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE)

    fun addExpense(amount: Int) {
        database().use { db ->
            db.insert(TABLE, null, ContentValues().apply { put(COLUMN, amount) })
        }
    }

    fun expenses(): Pair<Int, Int> =
        database().use { db ->
            db.rawQuery("SELECT COUNT(*), COALESCE(SUM($COLUMN), 0) FROM $TABLE", null).use {
                it.moveToFirst()
                it.getInt(0) to it.getInt(1)
            }
        }

    private fun database(): SQLiteDatabase =
        context.openOrCreateDatabase(DATABASE, Context.MODE_PRIVATE, null).apply {
            execSQL("CREATE TABLE IF NOT EXISTS $TABLE ($COLUMN INTEGER)")
        }

    companion object {
        const val DATABASE = "ledger.db"
        const val SETTINGS = "settings"
        const val CURRENCY = "currency"
        private const val TABLE = "expense"
        private const val COLUMN = "amount"
    }
}
