// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class ZstdCompressorTest {

    private val compressor = ZstdCompressor()

    @Test
    fun roundTrips() {
        val data = "restic ".repeat(10_000).toByteArray()

        assertArrayEquals(data, compressor.decompress(compressor.compress(data), data.size))
    }

    @Test(expected = CompressionException::class)
    fun rejectsFramesLargerThanTheLimit() {
        val data = ByteArray(1024)

        compressor.decompress(compressor.compress(data), data.size - 1)
    }

    @Test(expected = CompressionException::class)
    fun rejectsGarbage() {
        compressor.decompress(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8), 1024)
    }
}
