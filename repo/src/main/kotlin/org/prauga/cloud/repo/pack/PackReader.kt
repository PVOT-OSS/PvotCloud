// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo.pack

import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.prauga.cloud.repo.CompressionException
import org.prauga.cloud.repo.Compressor
import org.prauga.cloud.repo.FileType
import org.prauga.cloud.repo.Id
import org.prauga.cloud.repo.RepositoryException
import org.prauga.cloud.repo.crypto.AuthenticationException
import org.prauga.cloud.repo.crypto.CryptoKey

class PackReader(private val key: CryptoKey, private val compressor: Compressor) {

    fun readHeader(id: Id, pack: ByteArray): List<PackedBlob> {
        val minimum = CryptoKey.OVERHEAD + PackHeader.LENGTH_FIELD_SIZE
        if (pack.size < minimum) throw corrupted(id)
        val headerLength =
            ByteBuffer.wrap(
                    pack,
                    pack.size - PackHeader.LENGTH_FIELD_SIZE,
                    PackHeader.LENGTH_FIELD_SIZE,
                )
                .order(ByteOrder.LITTLE_ENDIAN)
                .int
        val headerStart = pack.size - PackHeader.LENGTH_FIELD_SIZE - headerLength
        if (
            headerLength < CryptoKey.OVERHEAD ||
                headerLength > PackHeader.MAX_SIZE ||
                headerStart < 0
        ) {
            throw corrupted(id)
        }
        val header = open(id, pack.copyOfRange(headerStart, headerStart + headerLength))
        return PackHeader.decode(header, headerStart.toLong()) ?: throw corrupted(id)
    }

    fun readBlob(id: Id, pack: ByteArray, blob: PackedBlob): ByteArray {
        val end = blob.offset + blob.length
        if (blob.offset < 0 || end > pack.size) throw corrupted(id)
        val payload = open(id, pack.copyOfRange(blob.offset.toInt(), end.toInt()))
        val plaintext =
            blob.uncompressedLength?.let { size ->
                try {
                    compressor.decompress(payload, size).takeIf { it.size == size }
                } catch (_: CompressionException) {
                    null
                } ?: throw corrupted(id)
            } ?: payload
        if (Id.hash(plaintext) != blob.id) throw corrupted(id)
        return plaintext
    }

    private fun open(id: Id, sealed: ByteArray): ByteArray =
        try {
            key.open(sealed)
        } catch (_: AuthenticationException) {
            throw corrupted(id)
        }

    private fun corrupted(id: Id) = RepositoryException.Corrupted(FileType.PACK, id.hex)
}
