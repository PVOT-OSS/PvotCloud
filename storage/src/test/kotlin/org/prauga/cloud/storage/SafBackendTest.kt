// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.storage

import java.io.File
import java.io.FileNotFoundException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.prauga.cloud.repo.FileType
import org.prauga.cloud.repo.Repository
import org.prauga.cloud.repo.ZstdCompressor
import org.prauga.cloud.repo.crypto.KdfParams
import org.prauga.cloud.repo.pack.BlobType

class SafBackendTest {

    @get:Rule val folder = TemporaryFolder()

    private val packId = "ab" + "1".repeat(62)

    @Test
    fun usesResticsLayout() {
        val backend = SafBackend(FileDocumentTree(folder.root))

        backend.save(FileType.PACK, packId, byteArrayOf(1, 2, 3))
        backend.save(FileType.SNAPSHOT, "s1", byteArrayOf(4))

        assertArrayEquals(byteArrayOf(1, 2, 3), File(folder.root, "data/ab/$packId").readBytes())
        assertTrue(File(folder.root, "snapshots/s1").exists())
        assertEquals(listOf(packId), backend.list(FileType.PACK))
        assertEquals(emptyList<String>(), backend.list(FileType.INDEX))
    }

    @Test
    fun leavesNoTemporaryFilesBehind() {
        val backend = SafBackend(FileDocumentTree(folder.root))

        backend.save(FileType.PACK, packId, ByteArray(100))

        assertEquals(listOf(packId), File(folder.root, "data/ab").list().orEmpty().toList())
    }

    @Test
    fun ignoresTemporaryFilesFromAnInterruptedRun() {
        File(folder.root, "data/ab").mkdirs()
        File(folder.root, "data/ab/.tmp-123-0").writeBytes(ByteArray(10))

        assertEquals(
            emptyList<String>(),
            SafBackend(FileDocumentTree(folder.root)).list(FileType.PACK),
        )
    }

    @Test
    fun savingAnExistingFileKeepsTheOriginal() {
        val backend = SafBackend(FileDocumentTree(folder.root))
        backend.save(FileType.KEY, "k", byteArrayOf(1))

        backend.save(FileType.KEY, "k", byteArrayOf(2))

        assertArrayEquals(byteArrayOf(1), backend.load(FileType.KEY, "k"))
        assertEquals(listOf("k"), File(folder.root, "keys").list().orEmpty().toList())
    }

    @Test
    fun deleteAndExists() {
        val backend = SafBackend(FileDocumentTree(folder.root))
        backend.save(FileType.LOCK, "l", byteArrayOf(1))

        backend.delete(FileType.LOCK, "l")

        assertFalse(backend.exists(FileType.LOCK, "l"))
        assertFalse(File(folder.root, "locks/l").exists())
    }

    @Test(expected = FileNotFoundException::class)
    fun loadingAMissingFileFails() {
        SafBackend(FileDocumentTree(folder.root)).load(FileType.SNAPSHOT, "missing")
    }

    @Test
    fun cachesFolderListings() {
        val tree = FileDocumentTree(folder.root)
        val backend = SafBackend(tree)
        backend.save(FileType.SNAPSHOT, "a", byteArrayOf(1))
        val before = tree.calls

        repeat(20) { backend.exists(FileType.SNAPSHOT, "a") }

        assertEquals(before, tree.calls)
    }

    @Test
    fun repositoryWorksOnTheBackend() {
        val compressor = ZstdCompressor()
        val backend = SafBackend(FileDocumentTree(folder.root))
        val repository =
            Repository.create(backend, compressor, "phrase", "phone", "user", KdfParams(1024, 8, 1))
        val pack =
            repository.newPackWriter(BlobType.DATA).apply { add("photo".toByteArray()) }.finish()
        repository.savePack(pack)

        val reopened =
            Repository.open(SafBackend(FileDocumentTree(folder.root)), compressor, "phrase")

        assertEquals(repository.config, reopened.config)
        assertArrayEquals(
            "photo".toByteArray(),
            reopened.readBlob(pack.id, reopened.readPackHeader(pack.id).single()),
        )
    }
}
