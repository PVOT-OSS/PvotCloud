// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.prauga.cloud.repo.crypto.KdfParams
import org.prauga.cloud.repo.pack.BlobType

class RepositoryTest {

    private val params = KdfParams(n = 1024, r = 8, p = 1)
    private val compressor = ZstdCompressor()

    private fun create(backend: Backend) =
        Repository.create(
            backend,
            compressor,
            PASSWORD,
            hostname = "phone",
            username = "user",
            params = params,
        )

    @Test
    fun createdRepositoryOpensWithThePassword() {
        val backend = InMemoryBackend()
        val created = create(backend)

        val opened = Repository.open(backend, compressor, PASSWORD)

        assertEquals(created.config, opened.config)
        assertEquals(Repository.VERSION, opened.config.version)
        assertEquals(64, opened.config.id.length)
        assertEquals(53, opened.chunkerPolynomial.degree)
    }

    @Test(expected = RepositoryException.WrongPassword::class)
    fun wrongPasswordIsRejected() {
        val backend = InMemoryBackend()
        create(backend)

        Repository.open(backend, compressor, "not the password")
    }

    @Test(expected = RepositoryException.AlreadyExists::class)
    fun refusesToOverwriteARepository() {
        val backend = InMemoryBackend()
        create(backend)

        create(backend)
    }

    @Test
    fun unpackedFilesRoundTripCompressed() {
        val backend = InMemoryBackend()
        val repository = create(backend)
        val json = """{"paths":["/sdcard/DCIM"],"hostname":"phone"}""".toByteArray()

        val name = repository.saveUnpacked(FileType.SNAPSHOT, json)

        assertEquals(listOf(name), repository.list(FileType.SNAPSHOT))
        assertArrayEquals(
            json,
            Repository.open(backend, compressor, PASSWORD).loadUnpacked(FileType.SNAPSHOT, name),
        )
    }

    @Test(expected = RepositoryException.Corrupted::class)
    fun corruptedUnpackedFileIsDetected() {
        val backend = InMemoryBackend()
        val repository = create(backend)
        val name = repository.saveUnpacked(FileType.SNAPSHOT, "{}".toByteArray())
        backend.corrupt(FileType.SNAPSHOT, name)

        repository.loadUnpacked(FileType.SNAPSHOT, name)
    }

    @Test
    fun storedPackReadsBack() {
        val backend = InMemoryBackend()
        val repository = create(backend)
        val writer = repository.newPackWriter(BlobType.DATA)
        val plaintext = "a chunk of a photo".toByteArray()
        writer.add(plaintext)
        val pack = writer.finish()

        repository.savePack(pack)
        val reopened = Repository.open(backend, compressor, PASSWORD)

        assertEquals(listOf(pack.id.hex), reopened.list(FileType.PACK))
        val header = reopened.readPackHeader(pack.id)
        assertEquals(pack.blobs, header)
        assertArrayEquals(plaintext, reopened.readBlob(pack.id, header.single()))
    }

    @Test(expected = RepositoryException.Corrupted::class)
    fun corruptedStoredPackIsDetected() {
        val backend = InMemoryBackend()
        val repository = create(backend)
        val pack = repository.newPackWriter(BlobType.DATA).apply { add(ByteArray(100)) }.finish()
        repository.savePack(pack)
        backend.corrupt(FileType.PACK, pack.id.hex)

        repository.readPackHeader(pack.id)
    }

    private companion object {
        const val PASSWORD =
            "abandon ability able about above absent absorb abstract absurd abuse access accident"
    }
}
