// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.cloud.sdk.agent

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

internal class TarWriter(private val out: OutputStream) {

    private val buffer = ByteArray(Tar.BLOCK * Tar.BUFFER_BLOCKS)

    fun file(path: String, size: Long, modifiedSeconds: Long, content: InputStream) {
        out.write(Tar.header(path, size, modifiedSeconds))
        var left = size
        while (left > 0) {
            val read = content.read(buffer, 0, minOf(buffer.size.toLong(), left).toInt())
            if (read < 0) throw IOException("$path ended $left bytes early")
            out.write(buffer, 0, read)
            left -= read
        }
        val padding = Tar.padding(size)
        if (padding > 0) out.write(ByteArray(padding))
    }

    fun finish() {
        out.write(ByteArray(Tar.BLOCK * 2))
        out.flush()
    }
}
