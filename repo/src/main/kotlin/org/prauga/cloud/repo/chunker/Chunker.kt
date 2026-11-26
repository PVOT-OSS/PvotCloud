// SPDX-FileCopyrightText: 2014 Alexander Neumann <alexander@bumpern.de>
// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: BSD-2-Clause

package org.prauga.cloud.repo.chunker

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

// Mirrors restic/chunker line for line; its byte, window and table constants stay inline for
// comparison.
@Suppress("MagicNumber", "ReturnCount")
class Chunker(
    private val polynomial: Polynomial,
    private val minSize: Int = MIN_SIZE,
    private val maxSize: Int = MAX_SIZE,
    averageBits: Int = AVERAGE_BITS,
) {

    private val tables = tablesFor(polynomial)
    private val polShift = polynomial.degree - 8
    private val splitMask = (1L shl averageBits) - 1

    init {
        require(polynomial.degree in 9..MAX_DEGREE) {
            "polynomial degree must be at most $MAX_DEGREE"
        }
        require(minSize in WINDOW_SIZE..maxSize) { "invalid chunk size bounds" }
    }

    fun chunks(input: InputStream): Sequence<Chunk> = sequence {
        val state = State().also(::reset)
        val buffer = ByteArray(BUFFER_SIZE)
        val data = ByteArrayOutputStream()
        var position = 0L
        var start = 0L
        var bufferPosition = 0
        var bufferEnd = 0
        while (true) {
            if (bufferPosition >= bufferEnd) {
                val read = input.readFully(buffer)
                if (read == 0) {
                    if (data.size() > 0) yield(Chunk(start, data.toByteArray(), state.digest))
                    return@sequence
                }
                bufferPosition = 0
                bufferEnd = read
            }
            val split = nextSplitPoint(state, buffer, bufferPosition, bufferEnd)
            if (split == null) {
                data.write(buffer, bufferPosition, bufferEnd - bufferPosition)
                position += bufferEnd - bufferPosition
                bufferPosition = bufferEnd
            } else {
                data.write(buffer, bufferPosition, split.length)
                bufferPosition += split.length
                position += split.length
                yield(Chunk(start, data.toByteArray(), split.cut))
                data.reset()
                start = position
            }
        }
    }

    private class State {
        val window = ByteArray(WINDOW_SIZE)
        var windowPosition = 0
        var digest = 0L
        var pre = 0
        var count = 0
    }

    private class Split(val length: Int, val cut: Long)

    private class Tables(val out: LongArray, val mod: LongArray)

    private fun reset(state: State) {
        state.window.fill(0)
        state.windowPosition = 0
        state.count = 0
        state.digest = slide(state, 0L, 1)
        state.pre = minSize - WINDOW_SIZE
    }

    private fun slide(state: State, digest: Long, byte: Byte): Long {
        val out = state.window[state.windowPosition]
        state.window[state.windowPosition] = byte
        state.windowPosition = (state.windowPosition + 1) % WINDOW_SIZE
        return update(digest xor tables.out[out.toInt() and 0xff], byte)
    }

    private fun update(digest: Long, byte: Byte): Long {
        val index = (digest ushr polShift).toInt()
        return ((digest shl 8) or (byte.toLong() and 0xff)) xor tables.mod[index]
    }

    private fun nextSplitPoint(state: State, buffer: ByteArray, from: Int, to: Int): Split? {
        var begin = from
        var skipped = 0
        if (state.pre > 0) {
            val available = to - from
            if (state.pre >= available) {
                state.pre -= available
                state.count += available
                return null
            }
            begin = from + state.pre
            skipped = state.pre
            state.count += state.pre
            state.pre = 0
        }
        var added = state.count
        var digest = state.digest
        val window = state.window
        var windowPosition = state.windowPosition
        for (i in begin until to) {
            val byte = buffer[i]
            val slot = windowPosition % WINDOW_SIZE
            val out = window[slot]
            window[slot] = byte
            digest = update(digest xor tables.out[out.toInt() and 0xff], byte)
            windowPosition++
            added++
            if ((digest and splitMask) == 0L || added >= maxSize) {
                if (added < minSize) continue
                reset(state)
                return Split(skipped + (i - begin) + 1, digest)
            }
        }
        state.digest = digest
        state.windowPosition = windowPosition % WINDOW_SIZE
        state.count += to - begin
        return null
    }

    companion object {
        const val MIN_SIZE = 512 * 1024
        const val MAX_SIZE = 8 * 1024 * 1024
        const val AVERAGE_BITS = 20
        private const val WINDOW_SIZE = 64
        private const val BUFFER_SIZE = 512 * 1024
        private const val MAX_DEGREE = 53

        private val tableCache = ConcurrentHashMap<Long, Tables>()

        private fun tablesFor(polynomial: Polynomial): Tables =
            tableCache.getOrPut(polynomial.value) {
                val out = LongArray(256)
                val mod = LongArray(256)
                for (b in 0 until 256) {
                    var hash = appendByte(Polynomial.ZERO, b, polynomial)
                    repeat(WINDOW_SIZE - 1) { hash = appendByte(hash, 0, polynomial) }
                    out[b] = hash.value
                }
                val degree = polynomial.degree
                for (b in 0 until 256) {
                    val shifted = b.toLong() shl degree
                    mod[b] = (Polynomial(shifted) % polynomial).value or shifted
                }
                Tables(out, mod)
            }

        private fun appendByte(hash: Polynomial, byte: Int, polynomial: Polynomial): Polynomial =
            Polynomial((hash.value shl 8) or byte.toLong()) % polynomial

        private fun InputStream.readFully(buffer: ByteArray): Int {
            var total = 0
            while (total < buffer.size) {
                val read = read(buffer, total, buffer.size - total)
                if (read < 0) break
                total += read
            }
            return total
        }
    }
}
