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
        return current[position++].toInt() and 0xff
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (!fill()) return -1
        val count = minOf(length, current.size - position)
        current.copyInto(buffer, offset, position, position + count)
        position += count
        return count
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
}
