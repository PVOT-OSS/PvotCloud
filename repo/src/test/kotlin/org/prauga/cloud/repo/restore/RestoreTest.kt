// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo.restore

import kotlin.random.Random
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.prauga.cloud.repo.FileType
import org.prauga.cloud.repo.Id
import org.prauga.cloud.repo.InMemoryBackend
import org.prauga.cloud.repo.Repository
import org.prauga.cloud.repo.RepositoryException
import org.prauga.cloud.repo.ZstdCompressor
import org.prauga.cloud.repo.backup.Backup
import org.prauga.cloud.repo.crypto.KdfParams
import org.prauga.cloud.repo.index.MasterIndex
import org.prauga.cloud.repo.source.MemoryDirectory
import org.prauga.cloud.repo.source.MemoryFile
import org.prauga.cloud.repo.source.MemorySymlink
import org.prauga.cloud.repo.tree.NodeType

class RestoreTest {

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

    private val files =
        mapOf(
            "DCIM/Camera/IMG_0001.jpg" to Random(1).nextBytes(5 * 1024 * 1024),
            "DCIM/Camera/IMG_0002.jpg" to Random(2).nextBytes(700 * 1024),
            "Documents/notes.txt" to "remember the phrase\n".toByteArray(),
            "empty" to ByteArray(0),
        )

    private val source =
        MemoryDirectory(
            "root",
            listOf(
                MemoryDirectory(
                    "DCIM",
                    listOf(
                        MemoryDirectory(
                            "Camera",
                            listOf(
                                MemoryFile(
                                    "IMG_0001.jpg",
                                    files.getValue("DCIM/Camera/IMG_0001.jpg"),
                                ),
                                MemoryFile(
                                    "IMG_0002.jpg",
                                    files.getValue("DCIM/Camera/IMG_0002.jpg"),
                                ),
                            ),
                        )
                    ),
                ),
                MemoryDirectory(
                    "Documents",
                    listOf(MemoryFile("notes.txt", files.getValue("Documents/notes.txt"))),
                ),
                MemoryFile("empty", files.getValue("empty")),
                MemorySymlink("latest", "DCIM/Camera/IMG_0002.jpg"),
            ),
        )

    private fun backupAndOpen(): Pair<Restore, Id> {
        val index = MasterIndex.empty()
        val result = Backup(repository, index).run(source, "phone", listOf("/"))
        val reopened = Repository.open(backend, ZstdCompressor(), "phrase")
        return Restore(reopened, MasterIndex.load(reopened)) to result.tree
    }

    @Test
    fun restoredFilesAreByteIdentical() {
        val (restore, tree) = backupAndOpen()

        val entries = restore.entries(tree).toList()
        val restored =
            entries
                .filter { it.node.type == NodeType.FILE }
                .associate { it.path to restore.open(it.node).readBytes() }

        assertEquals(files.keys, restored.keys)
        files.forEach { (path, data) -> assertArrayEquals(path, data, restored.getValue(path)) }
        assertEquals(
            listOf("DCIM", "DCIM/Camera", "Documents"),
            entries.filter { it.node.type == NodeType.DIR }.map { it.path },
        )
        assertEquals(
            "DCIM/Camera/IMG_0002.jpg",
            entries.single { it.node.type == NodeType.SYMLINK }.node.linkTarget,
        )
    }

    @Test(expected = RepositoryException.MissingBlob::class)
    fun missingBlobIsReported() {
        val (_, tree) = backupAndOpen()

        Restore(repository, MasterIndex.empty()).entries(tree).toList()
    }

    @Test(expected = RepositoryException.Corrupted::class)
    fun damagedPackIsDetectedWhileReading() {
        val (restore, tree) = backupAndOpen()
        val photo = restore.entries(tree).single { it.path == "DCIM/Camera/IMG_0001.jpg" }.node
        repository.list(FileType.PACK).forEach { backend.corrupt(FileType.PACK, it) }

        restore.open(photo).readBytes()
    }
}
