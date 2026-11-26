// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo.backup

import java.io.IOException
import java.io.InputStream
import java.time.OffsetDateTime
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.prauga.cloud.repo.FileType
import org.prauga.cloud.repo.Id
import org.prauga.cloud.repo.InMemoryBackend
import org.prauga.cloud.repo.Repository
import org.prauga.cloud.repo.ZstdCompressor
import org.prauga.cloud.repo.crypto.KdfParams
import org.prauga.cloud.repo.index.MasterIndex
import org.prauga.cloud.repo.snapshot.Snapshots
import org.prauga.cloud.repo.source.MemoryDirectory
import org.prauga.cloud.repo.source.MemoryFile
import org.prauga.cloud.repo.source.SourceFile

class InterruptedBackupTest {

    private val backend = InMemoryBackend()
    private val repository =
        Repository.create(
            backend,
            ZstdCompressor(),
            "phrase",
            "phone",
            "user",
            KdfParams(1024, 8, 1),
        )

    private val photos =
        (1..12).map { MemoryFile("IMG_%04d.jpg".format(it), Random(it).nextBytes(100 * 1024)) }

    private class FailingFile(override val name: String) : SourceFile {
        override val modTime: OffsetDateTime = OffsetDateTime.parse("2026-11-26T12:00:00Z")

        override fun open(): InputStream = throw IOException("storage removed")
    }

    private fun run(files: List<SourceFile>, checkpointPacks: Int = 1): BackupResult {
        val reopened = Repository.open(backend, ZstdCompressor(), "phrase")
        return Backup(
                reopened,
                MasterIndex.load(reopened),
                packSize = 64 * 1024,
                checkpointPacks = checkpointPacks,
            )
            .run(MemoryDirectory("DCIM", files), "phone", listOf("/DCIM"))
    }

    private fun runInterrupted(checkpointPacks: Int) {
        try {
            run(photos.take(8) + FailingFile("IMG_0009.jpg"), checkpointPacks)
            fail("the backup should stop at the failing file")
        } catch (_: IOException) {}
    }

    @Test
    fun resumedBackupDoesNotUploadCheckpointedData() {
        runInterrupted(checkpointPacks = 1)

        assertEquals(0, repository.list(FileType.SNAPSHOT).size)
        assertTrue(repository.list(FileType.INDEX).isNotEmpty())

        val resumed = run(photos)

        assertEquals(4 + 1, resumed.newBlobs)
        assertEquals(1, Snapshots(repository).list().size)
    }

    @Test
    fun packsSavedBeforeACheckpointAreRecovered() {
        runInterrupted(checkpointPacks = 100)

        assertEquals(0, repository.list(FileType.INDEX).size)
        assertEquals(8, repository.list(FileType.PACK).size)

        val resumed = run(photos)

        assertEquals(8, resumed.recoveredPacks)
        assertEquals(4 + 1, resumed.newBlobs)
        assertEquals(emptyList<Id>(), resumed.damagedPacks)
    }

    @Test
    fun damagedOrphanPackIsReportedAndSkipped() {
        val garbage = Random(99).nextBytes(500)
        backend.save(FileType.PACK, Id.hash(garbage).hex, garbage)

        val result = run(photos)

        assertEquals(listOf(Id.hash(garbage)), result.damagedPacks)
        assertEquals(0, result.recoveredPacks)
        assertEquals(1, Snapshots(repository).list().size)
    }
}
