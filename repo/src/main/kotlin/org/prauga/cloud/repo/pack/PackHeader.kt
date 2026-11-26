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
    private const val TREE_FLAG = 1
    private const val COMPRESSED_FLAG = 2
    private const val MAX_TYPE_CODE = TREE_FLAG or COMPRESSED_FLAG

    fun encode(blobs: List<PackedBlob>): ByteArray {
        val out = ByteArrayOutputStream()
        for (blob in blobs) {
            val compressed = blob.uncompressedLength != null
            val code =
                (if (blob.type == BlobType.TREE) TREE_FLAG else 0) or
                    (if (compressed) COMPRESSED_FLAG else 0)
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
            val blob = decodeEntry(buffer, offset) ?: return null
            blobs += blob
            offset += blob.length
        }
        return blobs.takeIf { offset == dataEnd }
    }

    private fun decodeEntry(buffer: ByteBuffer, offset: Long): PackedBlob? {
        val code = buffer.get().toInt()
        val compressed = (code and COMPRESSED_FLAG) != 0
        val entrySize = if (compressed) COMPRESSED_ENTRY_SIZE else PLAIN_ENTRY_SIZE
        if (code !in 0..MAX_TYPE_CODE || buffer.remaining() < entrySize - 1) return null
        val length = buffer.int
        val uncompressedLength = if (compressed) buffer.int else null
        val id = ByteArray(ID_SIZE).also(buffer::get)
        val type = if ((code and TREE_FLAG) != 0) BlobType.TREE else BlobType.DATA
        val valid = length >= 0 && (uncompressedLength == null || uncompressedLength > 0)
        return if (valid) PackedBlob(type, Id.of(id), offset, length, uncompressedLength) else null
    }

    fun littleEndian(value: Int): ByteArray =
        ByteBuffer.allocate(LENGTH_FIELD_SIZE).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()
}
