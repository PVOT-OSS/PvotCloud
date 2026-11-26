// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo.backup

import kotlin.random.Random
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.prauga.cloud.repo.FileType
import org.prauga.cloud.repo.InMemoryBackend
import org.prauga.cloud.repo.Repository
import org.prauga.cloud.repo.ZstdCompressor
import org.prauga.cloud.repo.crypto.KdfParams
import org.prauga.cloud.repo.index.MasterIndex
import org.prauga.cloud.repo.pack.BlobType

class BlobStoreTest {

    private val repository =
        Repository.create(
            InMemoryBackend(),
            ZstdCompressor(),
            "phrase",
            "phone",
            "user",
            KdfParams(1024, 8, 1),
        )
    private val index = MasterIndex.empty()

    private fun random(size: Int, seed: Int) = Random(seed).nextBytes(size)

    @Test
    fun duplicateBlobsAreStoredOnce() {
        val store = BlobStore(repository, index)
        val blob = random(1000, 1)

        val first = store.save(BlobType.DATA, blob)
        val second = store.save(BlobType.DATA, blob)
        store.flush()

        assertEquals(first, second)
        assertEquals(1, store.newBlobs)
        assertEquals(1, store.newPacks)
        assertEquals(1, index.size)
    }

    @Test
    fun blobsAlreadyInTheIndexAreSkipped() {
        BlobStore(repository, index).apply { save(BlobType.DATA, random(1000, 2)) }.flush()
        val store = BlobStore(repository, index)

        store.save(BlobType.DATA, random(1000, 2))
        store.flush()

        assertEquals(0, store.newBlobs)
        assertEquals(0, store.newPacks)
    }

    @Test
    fun packsAreCutAtTheTargetSize() {
        val store = BlobStore(repository, index, packSize = 64 * 1024)

        repeat(10) { store.save(BlobType.DATA, random(20 * 1024, 100 + it)) }
        store.flush()

        assertEquals(10, store.newBlobs)
        assertEquals(3, store.newPacks)
        assertEquals(3, repository.list(FileType.PACK).size)
    }

    @Test
    fun dataAndTreeBlobsGoToSeparatePacks() {
        val store = BlobStore(repository, index)
        val data = random(100, 3)
        val tree = """{"nodes":[]}""".toByteArray()

        val dataId = store.save(BlobType.DATA, data)
        val treeId = store.save(BlobType.TREE, tree)
        store.flush()

        val dataLocation = checkNotNull(index.lookup(BlobType.DATA, dataId))
        val treeLocation = checkNotNull(index.lookup(BlobType.TREE, treeId))
        assertTrue(dataLocation.packId != treeLocation.packId)
        assertArrayEquals(data, repository.readBlob(dataLocation.packId, dataLocation.blob))
        assertArrayEquals(tree, repository.readBlob(treeLocation.packId, treeLocation.blob))
    }
}
