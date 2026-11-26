// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo.crypto

import java.security.SecureRandom
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.prauga.cloud.repo.hex

class CryptoKeyTest {

    private val random = SecureRandom()

    @Test
    fun opensResticTestVector() {
        val key =
            CryptoKey(
                encryptionKey =
                    hex("303e8687b1d7db18421bdc6bb8588ccadac4d59ee87b8ff70c44e635790cafef"),
                macK = hex("ef4d8824cb80b2bcc5fbff8a9b12a42c"),
                macR = hex("cc8d4b948ee0ebfe1d415de921d10353"),
            )
        val sealed =
            hex(
                "69fb41c62d12def4593bd71757138606338f621aeaeb39da0fe4f99233f8037a54ea63338a813bcf3f75d8c3cc75dddf8750"
            )

        assertEquals("Dies ist ein Test!", key.open(sealed).decodeToString())
    }

    @Test
    fun sealThenOpenRoundTrips() {
        val key = CryptoKey.random(random)
        for (size in listOf(0, 1, 15, 16, 17, 4096, 1 shl 20)) {
            val plaintext = ByteArray(size).also(random::nextBytes)
            val sealed = key.seal(plaintext, random)

            assertEquals(size + CryptoKey.OVERHEAD, sealed.size)
            assertArrayEquals(plaintext, key.open(sealed))
        }
    }

    @Test(expected = AuthenticationException::class)
    fun rejectsTamperedCiphertext() {
        val key = CryptoKey.random(random)
        val sealed = key.seal("payload".toByteArray(), random)
        sealed[CryptoKey.NONCE_SIZE] = (sealed[CryptoKey.NONCE_SIZE].toInt() xor 1).toByte()

        key.open(sealed)
    }

    @Test(expected = AuthenticationException::class)
    fun rejectsWrongKey() {
        val sealed = CryptoKey.random(random).seal("payload".toByteArray(), random)

        CryptoKey.random(random).open(sealed)
    }

    @Test(expected = AuthenticationException::class)
    fun rejectsZeroNonce() {
        val key = CryptoKey.random(random)
        val sealed = key.seal("payload".toByteArray(), random)
        sealed.fill(0, 0, CryptoKey.NONCE_SIZE)

        key.open(sealed)
    }
}
