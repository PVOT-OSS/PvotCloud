// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo.backup

import java.io.ByteArrayOutputStream
import kotlin.random.Random
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.prauga.cloud.repo.Id
import org.prauga.cloud.repo.InMemoryBackend
import org.prauga.cloud.repo.Repository
import org.prauga.cloud.repo.ZstdCompressor
import org.prauga.cloud.repo.crypto.KdfParams
import org.prauga.cloud.repo.index.MasterIndex
import org.prauga.cloud.repo.pack.BlobType
import org.prauga.cloud.repo.snapshot.Snapshots
import org.prauga.cloud.repo.source.MemoryDirectory
import org.prauga.cloud.repo.source.MemoryFile
import org.prauga.cloud.repo.source.MemorySymlink
import org.prauga.cloud.repo.tree.NodeType
import org.prauga.cloud.repo.tree.Tree

class BackupTest {

    private val backend = InMemoryBackend()
    private val repository =
        Repository.create(
            backend,
            ZstdCompressor(),
            PASSWORD,
            "phone",
            "user",
            KdfParams(1024, 8, 1),
        )

    private val photo = Random(1).nextBytes(3 * 1024 * 1024)
    private val notes = "buy milk\n".toByteArray()

    private fun source(photoBytes: ByteArray = photo) =
        MemoryDirectory(
            "root",
            listOf(
                MemoryDirectory("DCIM", listOf(MemoryFile("IMG_0001.jpg", photoBytes))),
                MemoryFile("notes.txt", notes),
                MemoryFile("empty", ByteArray(0)),
                MemorySymlink("latest", "DCIM/IMG_0001.jpg"),
            ),
        )

    private fun backup(root: MemoryDirectory = source()): BackupResult {
        val reopened = Repository.open(backend, ZstdCompressor(), PASSWORD)
        return Backup(reopened, MasterIndex.load(reopened)).run(root, "phone", listOf("/"))
    }

    private fun readTree(index: MasterIndex, id: Id): Tree {
        val location = checkNotNull(index.lookup(BlobType.TREE, id))
        return Tree.decode(repository.readBlob(location.packId, location.blob))
    }

    private fun readFile(index: MasterIndex, content: List<Id>): ByteArray {
        val out = ByteArrayOutputStream()
        for (id in content) {
            val location = checkNotNull(index.lookup(BlobType.DATA, id))
            out.write(repository.readBlob(location.packId, location.blob))
        }
        return out.toByteArray()
    }

    @Test
    fun backupWritesASnapshotOfTheWholeTree() {
        val result = backup()

        val snapshot = Snapshots(repository).list().single()
        assertEquals(result.snapshotId, snapshot.id)
        assertEquals(result.tree, snapshot.snapshot.tree)
        assertEquals(3, result.files)
        assertEquals(1, result.directories)
        assertEquals(photo.size.toLong() + notes.size, result.bytes)

        val index = MasterIndex.load(repository)
        val root = readTree(index, result.tree)
        assertEquals(listOf("DCIM", "empty", "latest", "notes.txt"), root.nodes.map { it.name })
        val empty = root.nodes.first { it.name == "empty" }
        assertEquals(emptyList<Id>(), empty.content)
        assertEquals(
            "DCIM/IMG_0001.jpg",
            root.nodes.first { it.type == NodeType.SYMLINK }.linkTarget,
        )
        assertArrayEquals(
            notes,
            readFile(index, checkNotNull(root.nodes.first { it.name == "notes.txt" }.content)),
        )
        val dcim = readTree(index, checkNotNull(root.nodes.first { it.name == "DCIM" }.subtree))
        val image = dcim.nodes.single()
        assertEquals(photo.size.toLong(), image.size)
        assertArrayEquals(photo, readFile(index, checkNotNull(image.content)))
    }

    @Test
    fun unchangedDataIsNotStoredAgain() {
        backup()

        val second = backup()

        assertEquals(0, second.newBlobs)
        assertEquals(0, second.newPacks)
        assertEquals(2, Snapshots(repository).list().size)
    }

    @Test
    fun changedFileStoresOnlyItsNewChunks() {
        val first = backup()
        val edited = photo.copyOf().also { it[it.size - 1] = (it.last().toInt() xor 1).toByte() }

        val second = backup(source(edited))

        assertTrue(second.newBlobs < first.newBlobs)
        assertEquals(3, second.newBlobs)
    }

    @Test
    fun reportsProgressUpToTheTotals() {
        val reports = mutableListOf<BackupProgress>()
        val reopened = Repository.open(backend, ZstdCompressor(), PASSWORD)

        val result =
            Backup(reopened, MasterIndex.load(reopened))
                .run(source(), "phone", listOf("/"), progress = reports::add)

        assertEquals(BackupProgress(result.files, result.bytes), reports.last())
        assertTrue(reports.zipWithNext().all { (a, b) -> b.bytes >= a.bytes && b.files >= a.files })
        assertTrue(reports.size > result.files)
    }

    private companion object {
        const val PASSWORD = "phrase"
    }
}
