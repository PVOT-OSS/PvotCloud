// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud

import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.InputStream
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.random.Random
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.prauga.cloud.repo.FileSystemBackend
import org.prauga.cloud.repo.Repository
import org.prauga.cloud.repo.ZstdCompressor
import org.prauga.cloud.repo.backup.Backup
import org.prauga.cloud.repo.check.Check
import org.prauga.cloud.repo.index.MasterIndex
import org.prauga.cloud.repo.restore.Restore
import org.prauga.cloud.repo.source.SourceDirectory
import org.prauga.cloud.repo.source.SourceEntry
import org.prauga.cloud.repo.source.SourceFile
import org.prauga.cloud.repo.tree.NodeType

@RunWith(AndroidJUnit4::class)
class EngineOnDeviceTest {

    private val workDir =
        File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "engine-test")
    private val sourceDir = File(workDir, "phone")
    private val repoDir = File(workDir, "repository")

    private class DeviceDirectory(private val file: File) : SourceDirectory {
        override val name: String = file.name
        override val modTime: OffsetDateTime = modTime(file)

        override fun children(): List<SourceEntry> =
            file.listFiles().orEmpty().map {
                if (it.isDirectory) DeviceDirectory(it) else DeviceFile(it)
            }
    }

    private class DeviceFile(private val file: File) : SourceFile {
        override val name: String = file.name
        override val modTime: OffsetDateTime = modTime(file)

        override fun open(): InputStream = file.inputStream()
    }

    @Before
    fun createSource() {
        workDir.deleteRecursively()
        val random = Random(2026)
        val camera = File(sourceDir, "DCIM/Camera").apply { mkdirs() }
        repeat(6) {
            File(camera, "IMG_%04d.jpg".format(it)).writeBytes(random.nextBytes(10 * 1024 * 1024))
        }
        val notes = File(sourceDir, "Documents/notes").apply { mkdirs() }
        repeat(400) { File(notes, "note-$it.txt").writeText("note $it\n".repeat(200)) }
        File(sourceDir, "empty").writeBytes(ByteArray(0))
        repoDir.mkdirs()
    }

    @After
    fun cleanUp() {
        workDir.deleteRecursively()
    }

    @Test
    fun backupRestoreAndCheckOnDevice() {
        val backend = FileSystemBackend(repoDir.toPath())
        val compressor = ZstdCompressor()

        val repository =
            timed("create repository") {
                Repository.create(backend, compressor, PHRASE, "pixel", "test")
            }
        val first =
            timed("first backup") {
                Backup(repository, MasterIndex.empty())
                    .run(DeviceDirectory(sourceDir), "pixel", listOf("/"))
            }
        report(
            "first backup: ${first.files} files, ${first.bytes} bytes, ${first.newBlobs} blobs, ${first.newPacks} packs"
        )

        val reopened = timed("open repository") { Repository.open(backend, compressor, PHRASE) }
        val second =
            timed("second backup") {
                Backup(reopened, MasterIndex.load(reopened))
                    .run(DeviceDirectory(sourceDir), "pixel", listOf("/"))
            }
        assertEquals(0, second.newBlobs)
        assertEquals(0, second.newPacks)

        val restore = Restore(reopened, MasterIndex.load(reopened))
        val restoredFiles =
            timed("restore and compare") {
                restore
                    .entries(first.tree)
                    .filter { it.node.type == NodeType.FILE }
                    .onEach { entry ->
                        val original = File(sourceDir, entry.path).readBytes()
                        assertArrayEquals(
                            entry.path,
                            original,
                            restore.open(entry.node).use { it.readBytes() },
                        )
                    }
                    .count()
            }
        assertEquals(first.files, restoredFiles)

        val check =
            timed("check with all data") {
                Check(reopened, MasterIndex.load(reopened)).run(readDataPercent = 100)
            }
        assertTrue(check.problems.toString(), check.ok)
        assertEquals(check.packs, check.packsRead)
    }

    private fun report(message: String) {
        InstrumentationRegistry.getInstrumentation()
            .sendStatus(STATUS_REPORT, Bundle().apply { putString(TAG, message) })
    }

    private fun <T> timed(label: String, block: () -> T): T {
        val start = System.nanoTime()
        val result = block()
        report("$label took ${(System.nanoTime() - start) / 1_000_000} ms")
        return result
    }

    private companion object {
        const val TAG = "PvotCloudEngine"
        const val STATUS_REPORT = 1000
        const val PHRASE =
            "abandon ability able about above absent absorb abstract absurd abuse access accident"

        fun modTime(file: File): OffsetDateTime =
            Instant.ofEpochMilli(file.lastModified()).atOffset(ZoneOffset.UTC)
    }
}
