// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.cloud.sdk.agent

import java.io.IOException
import java.io.InputStream

internal class TarReader(private val input: InputStream) {

    private var current: EntryStream? = null

    fun next(): TarEntry? {
        current?.let { finishEntry(it) }
        current = null
        val header = ByteArray(Tar.BLOCK)
        val atEnd = !readFully(header) || header.all { it == ZERO }
        return if (atEnd) null else Tar.parse(header).also { current = EntryStream(it.size) }
    }

    fun content(): InputStream = checkNotNull(current) { "no current entry" }

    private fun finishEntry(stream: EntryStream) {
        stream.skipRest()
        val padding = Tar.padding(stream.size)
        if (padding > 0 && !readFully(ByteArray(padding)))
            throw IOException("archive ends inside padding")
    }

    private fun readFully(target: ByteArray): Boolean {
        var offset = 0
        while (offset < target.size) {
            val read = input.read(target, offset, target.size - offset)
            if (read < 0) {
                if (offset == 0) return false
                throw IOException("archive ends inside a block")
            }
            offset += read
        }
        return true
    }

    private inner class EntryStream(val size: Long) : InputStream() {
        private var left = size

        override fun read(): Int {
            val one = ByteArray(1)
            return if (read(one, 0, 1) < 0) -1 else one[0].toInt() and BYTE_MASK
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (left == 0L) return -1
            val read = input.read(buffer, offset, minOf(length.toLong(), left).toInt())
            if (read < 0) throw IOException("archive ends inside an entry")
            left -= read
            return read
        }

        fun skipRest() {
            val sink = ByteArray(Tar.BLOCK * Tar.BUFFER_BLOCKS)
            while (read(sink, 0, sink.size) >= 0) Unit
        }
    }

    private companion object {
        const val ZERO: Byte = 0
        const val BYTE_MASK = 0xff
    }
}
