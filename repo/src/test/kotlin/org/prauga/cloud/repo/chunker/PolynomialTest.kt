// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo.chunker

import java.security.SecureRandom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PolynomialTest {

    private val irreducibility =
        mapOf(
            0x38f1e565e288df to false,
            0x3DA3358B4DC173 to true,
            0x30a8295b9d5c91 to false,
            0x255f4350b962cb to false,
            0x267f776110a235 to false,
            0x2f4dae10d41227 to false,
            0x2482734cacca49 to true,
            0x312daf4b284899 to false,
            0x29dfb6553d01d1 to false,
            0x3548245eb26257 to false,
            0x3199e7ef4211b3 to false,
            0x362f39017dae8b to false,
            0x200d57aa6fdacb to false,
            0x35e0a4efa1d275 to false,
            0x2ced55b026577f to false,
            0x260b012010893d to false,
            0x2df29cbcd59e9d to false,
            0x3f2ac7488bd429 to false,
            0x3e5cb1711669fb to false,
            0x226d8de57a9959 to false,
            0x3c8de80aaf5835 to false,
            0x2026a59efb219b to false,
            0x39dfa4d13fb231 to false,
            0x3143d0464b3299 to false,
        )

    @Test
    fun classifiesResticIrreducibilityVectors() {
        for ((value, irreducible) in irreducibility) {
            assertEquals("0x${value.toString(16)}", irreducible, Polynomial(value).isIrreducible())
        }
    }

    @Test
    fun randomPolynomialIsIrreducibleOfDegree53() {
        val polynomial = Polynomial.random(SecureRandom())

        assertEquals(53, polynomial.degree)
        assertTrue(polynomial.isIrreducible())
    }

    @Test
    fun divisionInvertsMultiplication() {
        val a = Polynomial(0x1d5b)
        val b = Polynomial(0x3c1)

        assertEquals(a, (a * b) / b)
        assertEquals(Polynomial.ZERO, (a * b) % b)
    }

    @Test
    fun hexRoundTrips() {
        val polynomial = Polynomial(0x25b468838dcb75)

        assertEquals("25b468838dcb75", polynomial.toHex())
        assertEquals(polynomial, Polynomial.parse(polynomial.toHex()))
    }
}
