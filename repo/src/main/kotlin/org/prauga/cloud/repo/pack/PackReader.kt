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

    fun readHeader(id: Id, pack: ByteArray): List<PackedBlob> =
        decodeHeader(pack) ?: throw corrupted(id)

    fun readBlob(id: Id, pack: ByteArray, blob: PackedBlob): ByteArray =
        decodeBlob(pack, blob) ?: throw corrupted(id)

    private fun decodeHeader(pack: ByteArray): List<PackedBlob>? {
        if (pack.size < CryptoKey.OVERHEAD + PackHeader.LENGTH_FIELD_SIZE) return null
        val headerLength =
            ByteBuffer.wrap(
                    pack,
                    pack.size - PackHeader.LENGTH_FIELD_SIZE,
                    PackHeader.LENGTH_FIELD_SIZE,
                )
                .order(ByteOrder.LITTLE_ENDIAN)
                .int
        val headerStart = pack.size - PackHeader.LENGTH_FIELD_SIZE - headerLength
        val valid = headerLength in CryptoKey.OVERHEAD..PackHeader.MAX_SIZE && headerStart >= 0
        val header =
            if (valid) openOrNull(pack.copyOfRange(headerStart, headerStart + headerLength))
            else null
        return header?.let { PackHeader.decode(it, headerStart.toLong()) }
    }

    private fun decodeBlob(pack: ByteArray, blob: PackedBlob): ByteArray? {
        val end = blob.offset + blob.length
        val payload =
            if (blob.offset >= 0 && end <= pack.size) {
                openOrNull(pack.copyOfRange(blob.offset.toInt(), end.toInt()))
            } else {
                null
            }
        val plaintext = payload?.let { decompressed(it, blob.uncompressedLength) }
        return plaintext?.takeIf { Id.hash(it) == blob.id }
    }

    private fun decompressed(payload: ByteArray, size: Int?): ByteArray? =
        if (size == null) {
            payload
        } else {
            try {
                compressor.decompress(payload, size).takeIf { it.size == size }
            } catch (_: CompressionException) {
                null
            }
        }

    private fun openOrNull(sealed: ByteArray): ByteArray? =
        try {
            key.open(sealed)
        } catch (_: AuthenticationException) {
            null
        }

    private fun corrupted(id: Id) = RepositoryException.Corrupted(FileType.PACK, id.hex)
}
