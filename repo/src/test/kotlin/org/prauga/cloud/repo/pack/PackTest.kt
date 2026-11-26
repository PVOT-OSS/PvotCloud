// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo.pack

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.SecureRandom
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.prauga.cloud.repo.Id
import org.prauga.cloud.repo.RepositoryException
import org.prauga.cloud.repo.ZstdCompressor
import org.prauga.cloud.repo.crypto.CryptoKey

class PackTest {

    private val random = SecureRandom()
    private val key = CryptoKey.random(random)
    private val compressor = ZstdCompressor()
    private val reader = PackReader(key, compressor)

    private val blobs =
        listOf(
            ByteArray(1024 * 1024).also(random::nextBytes),
            "photo metadata ".repeat(5_000).toByteArray(),
            ByteArray(0),
        )

    private fun writePack(type: BlobType = BlobType.DATA): Pack {
        val writer = PackWriter(type, key, compressor, random)
        blobs.forEach(writer::add)
        return writer.finish()
    }

    @Test
    fun headerListsEveryBlob() {
        val pack = writePack()

        assertEquals(pack.blobs, reader.readHeader(pack.id, pack.data))
        assertEquals(blobs.map(Id::hash), pack.blobs.map { it.id })
        assertEquals(Id.hash(pack.data), pack.id)
    }

    @Test
    fun blobsReadBackIntact() {
        val pack = writePack(BlobType.TREE)

        pack.blobs.zip(blobs).forEach { (blob, plaintext) ->
            assertEquals(BlobType.TREE, blob.type)
            assertArrayEquals(plaintext, reader.readBlob(pack.id, pack.data, blob))
        }
    }

    @Test
    fun emptyBlobIsStoredUncompressed() {
        val pack = writePack()

        assertNull(pack.blobs.last().uncompressedLength)
        assertEquals(blobs[1].size, pack.blobs[1].uncompressedLength)
    }

    @Test
    fun headerLengthMatchesResticEntrySizes() {
        val pack = writePack()
        val length =
            ByteBuffer.wrap(pack.data, pack.data.size - 4, 4).order(ByteOrder.LITTLE_ENDIAN).int

        assertEquals(CryptoKey.OVERHEAD + 2 * 41 + 37, length)
    }

    @Test(expected = RepositoryException.Corrupted::class)
    fun tamperedBlobIsDetected() {
        val pack = writePack()
        pack.data[10] = (pack.data[10].toInt() xor 1).toByte()

        reader.readBlob(pack.id, pack.data, pack.blobs.first())
    }

    @Test(expected = RepositoryException.Corrupted::class)
    fun tamperedHeaderIsDetected() {
        val pack = writePack()
        val index = pack.data.size - 10
        pack.data[index] = (pack.data[index].toInt() xor 1).toByte()

        reader.readHeader(pack.id, pack.data)
    }

    @Test(expected = RepositoryException.Corrupted::class)
    fun truncatedPackIsDetected() {
        val pack = writePack()

        reader.readHeader(pack.id, pack.data.copyOfRange(0, pack.data.size - 100))
    }

    @Test(expected = RepositoryException.Corrupted::class)
    fun blobFromAnotherPackIsDetected() {
        val pack = writePack()
        val other = writePack()

        reader.readBlob(
            pack.id,
            pack.data,
            other.blobs.first().copy(offset = 0, length = pack.blobs[1].length),
        )
    }

    @Test(expected = IllegalStateException::class)
    fun emptyPackCannotBeFinished() {
        PackWriter(BlobType.DATA, key, compressor, random).finish()
    }
}
