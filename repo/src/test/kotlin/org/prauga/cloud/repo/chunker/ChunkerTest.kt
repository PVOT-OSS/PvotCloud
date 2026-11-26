// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo.chunker

import java.io.ByteArrayInputStream
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Test

class ChunkerTest {

    private class Expected(val length: Int, val cut: Long, val sha256: String)

    private val polynomial = Polynomial(0x3DA3358B4DC173)
    private val data by lazy { GoRandom(23).bytes(32 * 1024 * 1024) }

    @Test
    fun matchesResticChunksForAverageOneMebibyte() {
        assertChunks(Chunker(polynomial), data, expected("restic-chunks-avg20.csv"))
    }

    @Test
    fun matchesResticChunksForAverageHalfMebibyte() {
        assertChunks(
            Chunker(polynomial, averageBits = 19),
            data,
            expected("restic-chunks-avg19.csv"),
        )
    }

    @Test
    fun matchesResticChunksWithLargeBoundaries() {
        val chunker = Chunker(polynomial, minSize = 16 * 1024 * 1024, maxSize = 32 * 1024 * 1024)

        assertChunks(chunker, data, expected("restic-chunks-16m-32m.csv"))
    }

    @Test
    fun splitsNullBytesAtTheMinimumSize() {
        val nullChunk =
            Expected(
                Chunker.MIN_SIZE,
                0,
                "07854d2fef297a06ba81685e660c332de36d5d18d546927d30daad6d7fda1541",
            )

        assertChunks(Chunker(polynomial), ByteArray(4 * Chunker.MIN_SIZE), List(4) { nullChunk })
    }

    @Test
    fun emptyInputHasNoChunks() {
        assertEquals(0, Chunker(polynomial).chunks(ByteArrayInputStream(ByteArray(0))).count())
    }

    private fun assertChunks(chunker: Chunker, input: ByteArray, expected: List<Expected>) {
        val chunks = chunker.chunks(ByteArrayInputStream(input)).toList()

        assertEquals(expected.size, chunks.size)
        var start = 0L
        chunks.zip(expected).forEachIndexed { index, (chunk, want) ->
            assertEquals("start of chunk $index", start, chunk.start)
            assertEquals("length of chunk $index", want.length, chunk.data.size)
            assertEquals("cut of chunk $index", want.cut, chunk.cut)
            assertEquals("hash of chunk $index", want.sha256, sha256(chunk.data))
            start += chunk.data.size
        }
    }

    private fun expected(resource: String): List<Expected> =
        checkNotNull(javaClass.getResourceAsStream("/$resource")) { "missing $resource" }
            .bufferedReader()
            .readLines()
            .filter { it.isNotBlank() }
            .map { line ->
                val (length, cut, hash) = line.split(',')
                Expected(
                    length.toInt(),
                    java.lang.Long.parseUnsignedLong(cut.removePrefix("0x"), 16),
                    hash,
                )
            }

    private fun sha256(data: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(data).joinToString("") { "%02x".format(it) }
}
