// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo.restore

import java.io.IOException
import java.io.InputStream
import org.prauga.cloud.repo.Id

internal class ContentStream(
    private val blobs: Iterator<Id>,
    private val expectedSize: Long,
    private val load: (Id) -> ByteArray,
) : InputStream() {

    private var current = ByteArray(0)
    private var position = 0
    private var total = 0L

    override fun read(): Int {
        if (!fill()) return -1
        return current[position++].toInt() and BYTE_MASK
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        when {
            length == 0 -> 0
            !fill() -> -1
            else -> {
                val count = minOf(length, current.size - position)
                current.copyInto(buffer, offset, position, position + count)
                position += count
                count
            }
        }

    private fun fill(): Boolean {
        while (position >= current.size) {
            if (!blobs.hasNext()) {
                if (total != expectedSize)
                    throw IOException("restored $total bytes, expected $expectedSize")
                return false
            }
            current = load(blobs.next())
            position = 0
            total += current.size
        }
        return true
    }

    private companion object {
        const val BYTE_MASK = 0xff
    }
}
