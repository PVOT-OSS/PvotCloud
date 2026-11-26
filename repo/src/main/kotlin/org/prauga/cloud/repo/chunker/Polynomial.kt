// SPDX-FileCopyrightText: 2014 Alexander Neumann <alexander@bumpern.de>
// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: BSD-2-Clause

package org.prauga.cloud.repo.chunker

import java.security.SecureRandom

@JvmInline
value class Polynomial(val value: Long) {

    val degree: Int
        get() = 63 - java.lang.Long.numberOfLeadingZeros(value)

    operator fun plus(other: Polynomial): Polynomial = Polynomial(value xor other.value)

    operator fun times(other: Polynomial): Polynomial {
        when {
            value == 0L || other.value == 0L -> return ZERO
            value == 1L -> return other
            other.value == 1L -> return this
            other.value == 2L -> return timesTwo()
        }
        var result = 0L
        for (i in 0..other.degree) {
            if (other.bit(i)) result = result xor (value shl i)
        }
        check(Polynomial(result) / other == this) { "multiplication would overflow 64 bits" }
        return Polynomial(result)
    }

    operator fun div(divisor: Polynomial): Polynomial = divMod(divisor).first

    operator fun rem(divisor: Polynomial): Polynomial = divMod(divisor).second

    fun divMod(divisor: Polynomial): Pair<Polynomial, Polynomial> {
        require(divisor.value != 0L) { "division by zero" }
        if (value == 0L) return ZERO to ZERO
        val divisorDegree = divisor.degree
        var remainder = value
        var diff = degreeOf(remainder) - divisorDegree
        if (diff < 0) return ZERO to this
        var quotient = 0L
        while (diff >= 0) {
            quotient = quotient or (1L shl diff)
            remainder = remainder xor (divisor.value shl diff)
            diff = degreeOf(remainder) - divisorDegree
        }
        return Polynomial(quotient) to Polynomial(remainder)
    }

    fun gcd(other: Polynomial): Polynomial {
        var a = this
        var b = other
        while (b.value != 0L) {
            if (a.value == 0L) return b
            if (a.degree < b.degree) a = b.also { b = a }
            val r = a % b
            a = b
            b = r
        }
        return a
    }

    fun isIrreducible(): Boolean {
        for (i in 1..degree / 2) {
            if (gcd(qp(i, this)) != ONE) return false
        }
        return true
    }

    fun mulMod(factor: Polynomial, modulus: Polynomial): Polynomial {
        if (value == 0L || factor.value == 0L) return ZERO
        var result = ZERO
        for (i in 0..factor.degree) {
            if (factor.bit(i)) {
                var a = this
                repeat(i) { a = (a * TWO) % modulus }
                result = (result + a) % modulus
            }
        }
        return result
    }

    fun toHex(): String = java.lang.Long.toHexString(value)

    private fun bit(i: Int): Boolean = ((value ushr i) and 1L) != 0L

    private fun timesTwo(): Polynomial {
        check((value and Long.MIN_VALUE) == 0L) { "multiplication would overflow 64 bits" }
        return Polynomial(value shl 1)
    }

    companion object {
        val ZERO = Polynomial(0L)
        val ONE = Polynomial(1L)
        private val TWO = Polynomial(2L)
        private const val RANDOM_TRIES = 1_000_000
        private const val DEGREE = 53

        fun parse(hex: String): Polynomial = Polynomial(java.lang.Long.parseUnsignedLong(hex, 16))

        fun random(random: SecureRandom): Polynomial {
            repeat(RANDOM_TRIES) {
                val candidate =
                    (random.nextLong() and ((1L shl (DEGREE + 1)) - 1)) or (1L shl DEGREE) or 1L
                if (Polynomial(candidate).isIrreducible()) return Polynomial(candidate)
            }
            error("no irreducible polynomial found")
        }

        private fun degreeOf(value: Long): Int = 63 - java.lang.Long.numberOfLeadingZeros(value)

        private fun qp(p: Int, g: Polynomial): Polynomial {
            val num = 1L shl p
            var i = 1L
            var result = TWO
            while (i < num) {
                result = result.mulMod(result, g)
                i *= 2
            }
            return (result + TWO) % g
        }
    }
}
