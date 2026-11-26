// SPDX-FileCopyrightText: 2009 The Go Authors
// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: BSD-3-Clause

package org.prauga.cloud.repo.chunker

class GoRandom(seed: Long) {

    private val vec = LongArray(LENGTH)
    private var tap = 0
    private var feed = LENGTH - TAP

    init {
        var s = seed % INT32_MAX
        if (s < 0) s += INT32_MAX
        if (s == 0L) s = 89482311
        var x = s.toInt()
        for (i in -20 until LENGTH) {
            x = seedRand(x)
            if (i >= 0) {
                var u = x.toLong() shl 40
                x = seedRand(x)
                u = u xor (x.toLong() shl 20)
                x = seedRand(x)
                u = u xor x.toLong()
                vec[i] = u xor COOKED[i]
            }
        }
    }

    fun uint64(): Long {
        tap = if (tap == 0) LENGTH - 1 else tap - 1
        feed = if (feed == 0) LENGTH - 1 else feed - 1
        val x = vec[feed] + vec[tap]
        vec[feed] = x
        return x
    }

    fun int63(): Long = uint64() and Long.MAX_VALUE

    fun uint32(): Int = (int63() ushr 31).toInt()

    fun bytes(count: Int): ByteArray {
        val out = ByteArray(count)
        for (i in 0 until count step 4) {
            val r = uint32()
            out[i] = r.toByte()
            out[i + 1] = (r ushr 8).toByte()
            out[i + 2] = (r ushr 16).toByte()
            out[i + 3] = (r ushr 24).toByte()
        }
        return out
    }

    private companion object {
        const val LENGTH = 607
        const val TAP = 273
        const val INT32_MAX = (1L shl 31) - 1

        val COOKED: LongArray =
            checkNotNull(GoRandom::class.java.getResourceAsStream("/go-rng-cooked.txt")) {
                    "missing go-rng-cooked.txt"
                }
                .bufferedReader()
                .readLines()
                .filter { it.isNotBlank() }
                .map { it.trim().toLong() }
                .toLongArray()
                .also { check(it.size == LENGTH) { "expected $LENGTH cooked values" } }

        fun seedRand(value: Int): Int {
            val a = 48271
            val q = 44488
            val r = 3399
            val hi = value / q
            val lo = value % q
            var x = a * lo - r * hi
            if (x < 0) x += Int.MAX_VALUE
            return x
        }
    }
}
