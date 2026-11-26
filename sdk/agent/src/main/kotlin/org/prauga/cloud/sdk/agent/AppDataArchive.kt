// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.cloud.sdk.agent

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.os.Build
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files

internal class AppDataArchive(private val context: Context, private val spec: AppDataSpec) {

    private val dataDir = context.dataDir
    private val roots: Map<String, File?> =
        mapOf(
            FILE to context.filesDir,
            DATABASE to context.getDatabasePath(PROBE).parentFile,
            SHARED_PREFS to File(dataDir, SHARED_PREFS_DIR),
            ROOT to dataDir,
            EXTERNAL to context.getExternalFilesDir(null),
        )

    fun estimateBytes(): Long = entries().sumOf { it.file.length() }

    fun export(out: OutputStream) {
        val writer = TarWriter(out)
        for (entry in entries()) {
            val source = if (entry.domain == DATABASE) consistentCopy(entry.file) else entry.file
            try {
                source.inputStream().use {
                    writer.file(
                        "${entry.domain}/${entry.path}",
                        source.length(),
                        entry.file.lastModified() / MILLIS,
                        it,
                    )
                }
            } finally {
                if (source != entry.file) source.delete()
            }
        }
        writer.finish()
    }

    fun import(input: InputStream) {
        val reader = TarReader(input)
        while (true) {
            val entry = reader.next() ?: break
            val target = resolve(entry.path)
            target.parentFile?.mkdirs()
            val partial = File(target.parentFile, ".${target.name}.$PARTIAL")
            partial.outputStream().use { reader.content().copyTo(it) }
            if (entry.path.startsWith("$DATABASE/")) {
                JOURNALS.forEach { File(target.path + it).delete() }
            }
            if (!partial.renameTo(target)) throw IOException("could not replace ${entry.path}")
            target.setLastModified(entry.modifiedSeconds * MILLIS)
        }
    }

    private fun resolve(path: String): File {
        val domain = path.substringBefore('/')
        val rest = path.substringAfter('/', "")
        val root = roots[domain]?.canonicalFile ?: throw IOException("unknown location $domain")
        val target = File(root, rest).canonicalFile
        val inside = rest.isNotEmpty() && target.path.startsWith(root.path + File.separator)
        val top = if (inside) target.relativeTo(root).path.substringBefore(File.separator) else ""
        if (!inside || (domain == ROOT && top in ROOT_EXCLUDED))
            throw IOException("unsafe path $path")
        return target
    }

    private fun entries(): List<ArchiveEntry> =
        when (spec) {
            AppDataSpec.Everything -> everything()
            is AppDataSpec.Selected -> selected(spec)
        }

    private fun everything(): List<ArchiveEntry> = buildList {
        addAll(walk(FILE, context.filesDir))
        addAll(databases())
        addAll(walk(SHARED_PREFS, File(dataDir, SHARED_PREFS_DIR)))
        dataDir
            .listFiles()
            .orEmpty()
            .filter { it.isDirectory && it.name !in ROOT_EXCLUDED }
            .sortedBy { it.name }
            .forEach { dir -> addAll(walk(ROOT, dir, dir.name)) }
        context.getExternalFilesDir(null)?.let { addAll(walk(EXTERNAL, it)) }
    }

    private fun selected(spec: AppDataSpec.Selected): List<ArchiveEntry> = buildList {
        spec.databases.map(context::getDatabasePath).filter(File::isFile).forEach {
            add(ArchiveEntry(DATABASE, it.name, it))
        }
        spec.sharedPreferences
            .map { File(File(dataDir, SHARED_PREFS_DIR), "$it.xml") }
            .filter(File::isFile)
            .forEach { add(ArchiveEntry(SHARED_PREFS, it.name, it)) }
        spec.files.forEach { path ->
            val file = File(context.filesDir, path)
            if (file.isFile) add(ArchiveEntry(FILE, path, file)) else addAll(walk(FILE, file, path))
        }
    }

    private fun databases(): List<ArchiveEntry> {
        val dir = roots[DATABASE] ?: return emptyList()
        return dir.listFiles()
            .orEmpty()
            .filter { file -> file.isFile && JOURNALS.none { file.name.endsWith(it) } }
            .sortedBy { it.name }
            .map { ArchiveEntry(DATABASE, it.name, it) }
    }

    private fun walk(domain: String, dir: File, prefix: String = ""): List<ArchiveEntry> =
        dir.walkTopDown()
            .onEnter { !it.isSymlink() }
            .filter { it.isFile && !it.isSymlink() && !it.name.endsWith(PARTIAL) }
            .map { file ->
                val relative = file.relativeTo(dir).invariantSeparatorsPath
                ArchiveEntry(domain, if (prefix.isEmpty()) relative else "$prefix/$relative", file)
            }
            .sortedBy { it.path }
            .toList()

    private fun consistentCopy(file: File): File {
        val copy =
            File(File(context.cacheDir, WORK_DIR).apply { mkdirs() }, "${file.name}.$PARTIAL")
        copy.delete()
        try {
            SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    db.execSQL("VACUUM INTO ?", arrayOf(copy.path))
                } else {
                    db.rawQuery("PRAGMA wal_checkpoint(FULL)", null).use { it.moveToFirst() }
                    file.copyTo(copy, overwrite = true)
                }
            }
        } catch (_: SQLiteException) {
            file.copyTo(copy, overwrite = true)
        }
        return copy
    }

    private fun File.isSymlink(): Boolean = Files.isSymbolicLink(toPath())

    private data class ArchiveEntry(val domain: String, val path: String, val file: File)

    private companion object {
        const val FILE = "file"
        const val DATABASE = "db"
        const val SHARED_PREFS = "sp"
        const val ROOT = "root"
        const val EXTERNAL = "ext"
        const val SHARED_PREFS_DIR = "shared_prefs"
        const val PROBE = "probe"
        const val PARTIAL = "pvotcloud-partial"
        const val WORK_DIR = "org.prauga.cloud.sdk"
        const val MILLIS = 1000L
        val JOURNALS = listOf("-journal", "-wal", "-shm")
        val ROOT_EXCLUDED =
            setOf("cache", "code_cache", "no_backup", "files", "databases", SHARED_PREFS_DIR, "lib")
    }
}
