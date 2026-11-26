// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.cloud.sdk.agent

import java.io.IOException

internal object Tar {
    const val BLOCK = 512
    const val BUFFER_BLOCKS = 128

    private const val NAME = 0
    private const val NAME_LENGTH = 100
    private const val MODE = 100
    private const val UID = 108
    private const val GID = 116
    private const val ID_LENGTH = 8
    private const val SIZE = 124
    private const val SIZE_LENGTH = 12
    private const val MTIME = 136
    private const val CHECKSUM = 148
    private const val CHECKSUM_LENGTH = 8
    private const val TYPE = 156
    private const val MAGIC = 257
    private const val PREFIX = 345
    private const val PREFIX_LENGTH = 155
    private const val FILE_MODE = 0b110_100_100L
    private const val OCTAL = 8
    private const val BYTE_MASK = 0xff
    private const val ASCII_SPACE: Byte = 0x20
    private const val REGULAR_FILE: Byte = 0x30
    private const val OLD_REGULAR_FILE: Byte = 0
    private val MAGIC_VALUE = "ustar\u000000".toByteArray(Charsets.US_ASCII)

    fun header(path: String, size: Long, modifiedSeconds: Long): ByteArray {
        val (prefix, name) = split(path)
        val header = ByteArray(BLOCK)
        put(header, NAME, NAME_LENGTH, name.toByteArray(Charsets.UTF_8))
        octal(header, MODE, ID_LENGTH, FILE_MODE)
        octal(header, UID, ID_LENGTH, 0)
        octal(header, GID, ID_LENGTH, 0)
        octal(header, SIZE, SIZE_LENGTH, size)
        octal(header, MTIME, SIZE_LENGTH, modifiedSeconds)
        header[TYPE] = REGULAR_FILE
        MAGIC_VALUE.copyInto(header, MAGIC)
        put(header, PREFIX, PREFIX_LENGTH, prefix.toByteArray(Charsets.UTF_8))
        header.fill(ASCII_SPACE, CHECKSUM, CHECKSUM + CHECKSUM_LENGTH)
        octal(
            header,
            CHECKSUM,
            CHECKSUM_LENGTH - 1,
            header.sumOf { it.toInt() and BYTE_MASK }.toLong(),
        )
        return header
    }

    fun parse(header: ByteArray): TarEntry {
        val stored = readOctal(header, CHECKSUM, CHECKSUM_LENGTH)
        val actual =
            header.indices.sumOf { i ->
                if (i in CHECKSUM until CHECKSUM + CHECKSUM_LENGTH) ASCII_SPACE.toInt()
                else header[i].toInt() and BYTE_MASK
            }
        if (stored != actual.toLong()) throw IOException("tar header checksum mismatch")
        val type = header[TYPE]
        if (type != REGULAR_FILE && type != OLD_REGULAR_FILE) {
            throw IOException("unsupported tar entry type ${type.toInt().toChar()}")
        }
        val name = text(header, NAME, NAME_LENGTH)
        val prefix = text(header, PREFIX, PREFIX_LENGTH)
        return TarEntry(
            path = if (prefix.isEmpty()) name else "$prefix/$name",
            size = readOctal(header, SIZE, SIZE_LENGTH),
            modifiedSeconds = readOctal(header, MTIME, SIZE_LENGTH),
        )
    }

    fun padding(size: Long): Int = ((BLOCK - size % BLOCK) % BLOCK).toInt()

    private fun split(path: String): Pair<String, String> {
        val bytes = path.toByteArray(Charsets.UTF_8)
        if (bytes.size <= NAME_LENGTH) return "" to path
        val cut =
            path.indices
                .filter { path[it] == '/' }
                .lastOrNull { index ->
                    path.substring(0, index).toByteArray(Charsets.UTF_8).size <= PREFIX_LENGTH &&
                        path.substring(index + 1).toByteArray(Charsets.UTF_8).size <= NAME_LENGTH
                } ?: throw IOException("path is too long for tar: $path")
        return path.substring(0, cut) to path.substring(cut + 1)
    }

    private fun put(header: ByteArray, offset: Int, length: Int, value: ByteArray) {
        require(value.size <= length) { "field too long" }
        value.copyInto(header, offset)
    }

    private fun octal(header: ByteArray, offset: Int, length: Int, value: Long) {
        val digits = value.toString(OCTAL).padStart(length - 1, '0')
        require(digits.length < length) { "value $value does not fit in $length bytes" }
        digits.toByteArray(Charsets.US_ASCII).copyInto(header, offset)
        header[offset + length - 1] = 0
    }

    private fun readOctal(header: ByteArray, offset: Int, length: Int): Long {
        val digits = text(header, offset, length).trim()
        return if (digits.isEmpty()) 0
        else digits.toLongOrNull(OCTAL) ?: throw IOException("bad tar number")
    }

    private fun text(header: ByteArray, offset: Int, length: Int): String {
        val end =
            (offset until offset + length).firstOrNull { header[it] == OLD_REGULAR_FILE }
                ?: (offset + length)
        return String(header, offset, end - offset, Charsets.UTF_8)
    }
}
