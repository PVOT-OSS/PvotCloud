// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo.index

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.prauga.cloud.repo.FileType
import org.prauga.cloud.repo.Id
import org.prauga.cloud.repo.InMemoryBackend
import org.prauga.cloud.repo.Repository
import org.prauga.cloud.repo.RepositoryException
import org.prauga.cloud.repo.ZstdCompressor
import org.prauga.cloud.repo.crypto.KdfParams
import org.prauga.cloud.repo.pack.BlobType

class MasterIndexTest {

    private val backend = InMemoryBackend()
    private val compressor = ZstdCompressor()
    private val repository =
        Repository.create(backend, compressor, PASSWORD, "phone", "user", KdfParams(1024, 8, 1))

    private fun storePack(type: BlobType, vararg blobs: ByteArray) =
        repository
            .newPackWriter(type)
            .apply { blobs.forEach(::add) }
            .finish()
            .also(repository::savePack)

    @Test
    fun savedIndexLocatesEveryBlob() {
        val photo = "photo chunk".toByteArray()
        val tree = """{"nodes":[]}""".toByteArray()
        val dataPack = storePack(BlobType.DATA, photo, ByteArray(0))
        val treePack = storePack(BlobType.TREE, tree)
        val index = MasterIndex.empty()
        index.addPack(dataPack.id, dataPack.blobs)
        index.addPack(treePack.id, treePack.blobs)

        val names = index.save(repository)
        val loaded = MasterIndex.load(Repository.open(backend, compressor, PASSWORD))

        assertEquals(1, names.size)
        assertFalse(index.hasPending)
        assertEquals(3, loaded.size)
        val location = checkNotNull(loaded.lookup(BlobType.DATA, Id.hash(photo)))
        assertEquals(dataPack.id, location.packId)
        assertArrayEquals(photo, repository.readBlob(location.packId, location.blob))
        assertEquals(treePack.id, loaded.lookup(BlobType.TREE, Id.hash(tree))?.packId)
    }

    @Test
    fun blobTypeIsPartOfTheKey() {
        val content = "same bytes".toByteArray()
        val pack = storePack(BlobType.DATA, content)
        val index = MasterIndex.empty()
        index.addPack(pack.id, pack.blobs)

        assertTrue(index.contains(BlobType.DATA, Id.hash(content)))
        assertFalse(index.contains(BlobType.TREE, Id.hash(content)))
    }

    @Test
    fun indexJsonUsesResticFieldNames() {
        val pack = storePack(BlobType.DATA, "compressible ".repeat(100).toByteArray(), ByteArray(0))
        val index = MasterIndex.empty()
        index.addPack(pack.id, pack.blobs)
        val name = index.save(repository).single()

        val json =
            Json.parseToJsonElement(repository.loadUnpacked(FileType.INDEX, name).decodeToString())
                .jsonObject

        assertNull(json["supersedes"])
        val packJson = json.getValue("packs").jsonArray.single().jsonObject
        assertEquals(pack.id.hex, packJson.getValue("id").jsonPrimitive.content)
        val (compressed, empty) = packJson.getValue("blobs").jsonArray.map { it.jsonObject }
        assertEquals("data", compressed.getValue("type").jsonPrimitive.content)
        assertEquals(1300, compressed.getValue("uncompressed_length").jsonPrimitive.int)
        assertEquals(setOf("id", "type", "offset", "length"), empty.keys)
    }

    @Test
    fun supersededIndexFilesAreIgnored() {
        val old = storePack(BlobType.DATA, "old".toByteArray())
        val oldName =
            MasterIndex.empty().apply { addPack(old.id, old.blobs) }.save(repository).single()
        val current = storePack(BlobType.DATA, "new".toByteArray())
        val json =
            """{"supersedes":["$oldName"],"packs":[{"id":"${current.id.hex}","blobs":[""" +
                """{"id":"${current.blobs.single().id.hex}","type":"data","offset":0,""" +
                """"length":${current.blobs.single().length},""" +
                """"uncompressed_length":${current.blobs.single().uncompressedLength}}]}]}"""
        repository.saveUnpacked(FileType.INDEX, json.toByteArray())

        val loaded = MasterIndex.load(repository)

        assertFalse(loaded.contains(BlobType.DATA, Id.hash("old".toByteArray())))
        assertTrue(loaded.contains(BlobType.DATA, Id.hash("new".toByteArray())))
    }

    @Test(expected = RepositoryException.Corrupted::class)
    fun unknownBlobTypeIsRejected() {
        repository.saveUnpacked(
            FileType.INDEX,
            ("""{"packs":[{"id":"${"a".repeat(64)}","blobs":[""" +
                    """{"id":"${"b".repeat(64)}","type":"x","offset":0,"length":40}]}]}""")
                .toByteArray(),
        )

        MasterIndex.load(repository)
    }

    private companion object {
        const val PASSWORD = "correct horse battery staple"
    }
}
