// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo.crypto

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class CryptoKey(encryptionKey: ByteArray, macK: ByteArray, macR: ByteArray) {

    private val encryptionKey: ByteArray = encryptionKey.copyOf()
    internal val macK: ByteArray = macK.copyOf()
    internal val macR: ByteArray = macR.copyOf()

    init {
        require(encryptionKey.size == ENCRYPTION_KEY_SIZE) {
            "encryption key must be $ENCRYPTION_KEY_SIZE bytes"
        }
        require(macK.size == MAC_PART_SIZE && macR.size == MAC_PART_SIZE) {
            "MAC key parts must be $MAC_PART_SIZE bytes"
        }
    }

    fun seal(plaintext: ByteArray, random: SecureRandom): ByteArray {
        val nonce = newNonce(random)
        val sealed = ByteArray(plaintext.size + OVERHEAD)
        nonce.copyInto(sealed)
        ctr(Cipher.ENCRYPT_MODE, nonce).doFinal(plaintext, 0, plaintext.size, sealed, NONCE_SIZE)
        Poly1305Aes.mac(sealed, NONCE_SIZE, plaintext.size, nonce, macK, macR)
            .copyInto(sealed, NONCE_SIZE + plaintext.size)
        return sealed
    }

    fun open(sealed: ByteArray): ByteArray {
        if (!isAuthentic(sealed)) throw AuthenticationException()
        val nonce = sealed.copyOfRange(0, NONCE_SIZE)
        return ctr(Cipher.DECRYPT_MODE, nonce).doFinal(sealed, NONCE_SIZE, sealed.size - OVERHEAD)
    }

    private fun isAuthentic(sealed: ByteArray): Boolean {
        if (sealed.size < OVERHEAD) return false
        val nonce = sealed.copyOfRange(0, NONCE_SIZE)
        val length = sealed.size - OVERHEAD
        val actual = sealed.copyOfRange(NONCE_SIZE + length, sealed.size)
        return nonce.any { it != ZERO } &&
            MessageDigest.isEqual(
                Poly1305Aes.mac(sealed, NONCE_SIZE, length, nonce, macK, macR),
                actual,
            )
    }

    internal fun encryptionKeyCopy(): ByteArray = encryptionKey.copyOf()

    private fun ctr(mode: Int, nonce: ByteArray): Cipher =
        Cipher.getInstance("AES/CTR/NoPadding").apply {
            init(mode, SecretKeySpec(encryptionKey, "AES"), IvParameterSpec(nonce))
        }

    companion object {
        const val NONCE_SIZE = 16
        const val MAC_SIZE = 16
        const val OVERHEAD = NONCE_SIZE + MAC_SIZE
        internal const val ENCRYPTION_KEY_SIZE = 32
        internal const val MAC_PART_SIZE = 16
        private const val ZERO: Byte = 0

        fun random(random: SecureRandom): CryptoKey {
            fun bytes(size: Int) = ByteArray(size).also(random::nextBytes)
            return CryptoKey(bytes(ENCRYPTION_KEY_SIZE), bytes(MAC_PART_SIZE), bytes(MAC_PART_SIZE))
        }

        private fun newNonce(random: SecureRandom): ByteArray {
            val nonce = ByteArray(NONCE_SIZE)
            do random.nextBytes(nonce) while (nonce.all { it == ZERO })
            return nonce
        }
    }
}
