// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo

import com.github.luben.zstd.Zstd
import com.github.luben.zstd.ZstdException
import com.github.luben.zstd.ZstdInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException

class ZstdCompressor(private val level: Int = DEFAULT_LEVEL) : Compressor {

    override fun compress(data: ByteArray): ByteArray = Zstd.compress(data, level)

    override fun decompress(data: ByteArray, maxSize: Int): ByteArray {
        val declared = Zstd.getFrameContentSize(data)
        if (declared > maxSize)
            throw CompressionException("frame declares $declared bytes, limit is $maxSize")
        return try {
            if (declared >= 0) Zstd.decompress(data, declared.toInt())
            else decompressStream(data, maxSize)
        } catch (e: ZstdException) {
            throw CompressionException("invalid zstd frame", e)
        } catch (e: IOException) {
            throw CompressionException("invalid zstd frame", e)
        }
    }

    private fun decompressStream(data: ByteArray, maxSize: Int): ByteArray {
        val out = ByteArrayOutputStream()
        ZstdInputStream(ByteArrayInputStream(data)).use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                if (out.size() + read > maxSize)
                    throw CompressionException("frame exceeds $maxSize bytes")
                out.write(buffer, 0, read)
            }
        }
        return out.toByteArray()
    }

    private companion object {
        const val DEFAULT_LEVEL = 3
        const val BUFFER_SIZE = 64 * 1024
    }
}
