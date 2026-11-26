// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo.pack

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.prauga.cloud.repo.Id

internal object PackHeader {

    const val LENGTH_FIELD_SIZE = 4
    const val MAX_SIZE = 16 * 1024 * 1024
    private const val ID_SIZE = 32
    private const val PLAIN_ENTRY_SIZE = 1 + 4 + ID_SIZE
    private const val COMPRESSED_ENTRY_SIZE = PLAIN_ENTRY_SIZE + 4

    fun encode(blobs: List<PackedBlob>): ByteArray {
        val out = ByteArrayOutputStream()
        for (blob in blobs) {
            val compressed = blob.uncompressedLength != null
            val code = (if (blob.type == BlobType.TREE) 1 else 0) + (if (compressed) 2 else 0)
            out.write(code)
            out.write(littleEndian(blob.length))
            blob.uncompressedLength?.let { out.write(littleEndian(it)) }
            out.write(blob.id.bytes())
        }
        return out.toByteArray()
    }

    fun decode(header: ByteArray, dataEnd: Long): List<PackedBlob>? {
        val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        val blobs = mutableListOf<PackedBlob>()
        var offset = 0L
        while (buffer.hasRemaining()) {
            val code = buffer.get().toInt()
            if (code !in 0..3) return null
            val compressed = code >= 2
            if (
                buffer.remaining() <
                    (if (compressed) COMPRESSED_ENTRY_SIZE else PLAIN_ENTRY_SIZE) - 1
            )
                return null
            val length = buffer.int
            val uncompressedLength = if (compressed) buffer.int else null
            if (length < 0 || (uncompressedLength != null && uncompressedLength <= 0)) return null
            val id = ByteArray(ID_SIZE).also(buffer::get)
            val type = if (code % 2 == 1) BlobType.TREE else BlobType.DATA
            blobs += PackedBlob(type, Id.of(id), offset, length, uncompressedLength)
            offset += length
        }
        return blobs.takeIf { offset == dataEnd }
    }

    fun littleEndian(value: Int): ByteArray =
        ByteBuffer.allocate(LENGTH_FIELD_SIZE).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()
}
