// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.cloud.sdk.agent

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDataArchiveTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val notes = File(context.filesDir, "notes/today.txt")
    private val extra = File(context.dataDir, "app_extra/state.bin")
    private val cache = File(context.cacheDir, "thumb.bin")
    private val noBackup = File(context.noBackupFilesDir, "token")

    @Before
    fun setUp() {
        wipe()
        notes.parentFile?.mkdirs()
        notes.writeText("milk, eggs")
        extra.parentFile?.mkdirs()
        extra.writeBytes(byteArrayOf(1, 2, 3))
        cache.parentFile?.mkdirs()
        cache.writeBytes(byteArrayOf(9))
        noBackup.parentFile?.mkdirs()
        noBackup.writeText("device-only")
        context
            .getSharedPreferences("prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("theme", "dark")
            .commit()
        context.openOrCreateDatabase("ledger.db", Context.MODE_PRIVATE, null).use { db ->
            db.enableWriteAheadLogging()
            db.execSQL("CREATE TABLE entry (amount INTEGER)")
            repeat(50) { db.execSQL("INSERT INTO entry VALUES ($it)") }
        }
    }

    @After
    fun tearDown() {
        wipe()
    }

    @Test
    fun everythingRestoresFilesPreferencesAndDatabases() {
        val archive = export(AppDataSpec.Everything)
        wipe()

        AppDataArchive(context, AppDataSpec.Everything).import(ByteArrayInputStream(archive))

        assertThat(notes.readText()).isEqualTo("milk, eggs")
        assertThat(extra.readBytes().toList()).containsExactly(1.toByte(), 2.toByte(), 3.toByte())
        assertThat(File(context.dataDir, "shared_prefs/prefs.xml").readText().contains("dark"))
            .isEqualTo(true)
        assertThat(sum()).isEqualTo((0 until 50).sum())
    }

    @Test
    fun everythingLeavesOutCacheAndNoBackup() {
        val paths = paths(export(AppDataSpec.Everything))

        assertThat(paths.any { it.contains("thumb.bin") || it.contains("token") }).isFalse()
        assertThat(paths.any { it.endsWith("-wal") || it.endsWith("-shm") }).isFalse()
    }

    @Test
    fun aSelectionExportsOnlyWhatWasDeclared() {
        val paths =
            paths(export(AppDataSpec.Selected(listOf("ledger.db"), emptyList(), listOf("notes"))))

        assertThat(paths).containsExactly("db/ledger.db", "file/notes/today.txt")
    }

    @Test
    fun pathsOutsideTheAppAreRejected() {
        val out = ByteArrayOutputStream()
        TarWriter(out).apply {
            file("file/../../evil", 1, 0, ByteArrayInputStream(byteArrayOf(1)))
            finish()
        }

        assertFailure {
            AppDataArchive(context, AppDataSpec.Everything)
                .import(ByteArrayInputStream(out.toByteArray()))
        }
            .isInstanceOf<IOException>()
        assertThat(File(context.dataDir.parentFile, "evil").exists()).isFalse()
    }

    private fun export(spec: AppDataSpec): ByteArray =
        ByteArrayOutputStream().also { AppDataArchive(context, spec).export(it) }.toByteArray()

    private fun paths(archive: ByteArray): List<String> {
        val reader = TarReader(ByteArrayInputStream(archive))
        return buildList {
            while (true) add(reader.next()?.path ?: break)
        }
    }

    private fun sum(): Int =
        SQLiteDatabase.openDatabase(
                context.getDatabasePath("ledger.db").path,
                null,
                SQLiteDatabase.OPEN_READONLY,
            )
            .use { db ->
                db.rawQuery("SELECT SUM(amount) FROM entry", null).use {
                    it.moveToFirst()
                    it.getInt(0)
                }
            }

    private fun wipe() {
        listOf(
                context.filesDir,
                File(context.dataDir, "app_extra"),
                context.cacheDir,
                context.noBackupFilesDir,
            )
            .forEach { it.deleteRecursively() }
        context.deleteDatabase("ledger.db")
        context.deleteSharedPreferences("prefs")
    }
}
